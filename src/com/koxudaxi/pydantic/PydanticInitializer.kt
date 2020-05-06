package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.NoAccessDuringPsiEvents
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.util.QualifiedName
import com.intellij.serviceContainer.AlreadyDisposedException
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.PyQualifiedNameOwner
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.sdk.PythonSdkUtil.*
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules
import org.apache.tuweni.toml.Toml
import org.apache.tuweni.toml.TomlArray
import org.apache.tuweni.toml.TomlParseResult
import org.apache.tuweni.toml.TomlTable
import org.ini4j.Ini
import org.ini4j.IniPreferences
import java.io.File

class PydanticInitializer : StartupActivity {

    private fun getDefaultPyProjectTomlPath(project: Project): String {
        return project.basePath + "/pyproject.toml"
    }

    private fun getDefaultMypyIniPath(project: Project): String {
        return project.basePath + "/mypy.ini"
    }

    private fun walkDirectory(virtualFile: VirtualFile, parentTarget: VirtualFile, action: (virtualFile: VirtualFile) -> (Unit)) {
        virtualFile.children.forEach {
            when {
                it.isDirectory -> {
                    val nextTarget = parentTarget.findChild(it.name) ?: parentTarget.createChildDirectory(null, it.name)
                    walkDirectory(it, nextTarget, action)
                }
                else -> action(it)
            }
        }
    }

    private fun copyPyFileToPyiFile(source: VirtualFile, target: VirtualFile, fileName: String) {
        if (source.name.endsWith(".py") && target.findChild(fileName) == null) {
            source.copy(null, target, fileName)
        }
    }

    private fun copyPydanticStub(source: VirtualFile, skeletons: VirtualFile?, walk: Boolean) {
        skeletons?.let {
            val pydanticStub = skeletons.findChild("pydantic") ?: skeletons.createChildDirectory(null, "pydantic")
            when {
                walk -> walkDirectory(source, pydanticStub) { target ->
                    copyPyFileToPyiFile(target, pydanticStub, target.name + "i")
                }
                else -> copyPyFileToPyiFile(source, pydanticStub, source.name.removePrefix(pydanticStub.path) + "i")
            }
        }

    }

    private fun getSdk(project: Project): Sdk? {
        return project.pythonSdk ?: project.modules.mapNotNull { findPythonSdk(it) }.firstOrNull()
    }

    private fun getSitePackage(sdk: Sdk?): List<VirtualFile> {
        if (sdk == null) return emptyList()
        val prefix = sdk.homePath!!.split("/").last()
        val userSitePath = listOf(getUserSite(), "lib", prefix, PyNames.SITE_PACKAGES).joinToString(File.separator)
        return listOfNotNull(getSitePackagesDirectory(sdk), LocalFileSystem.getInstance().refreshAndFindFileByPath(userSitePath))
    }

    fun initializeFileLoader(project: Project) {
        val configService = PydanticConfigService.getInstance(project)
        val defaultPyProjectToml = getDefaultPyProjectTomlPath(project)
        val defaultMypyIni = getDefaultMypyIniPath(project)
        var sdk = getSdk(project)
        var sitePackages = getSitePackage(sdk)
        var skeletons = sdk?.let { findSkeletonsDir(it) }
        invokeAfterPsiEvents {
            when (val pyprojectToml = LocalFileSystem.getInstance()
                    .findFileByPath(configService.pyprojectToml ?: defaultPyProjectToml)
                ) {
                is VirtualFile -> loadPyprojecToml(project, pyprojectToml, configService)
                else -> clearPyProjectTomlConfig(configService)
            }
            when (val mypyIni = LocalFileSystem.getInstance()
                    .findFileByPath(configService.mypyIni ?: defaultMypyIni)
                ) {
                is VirtualFile -> loadMypyIni(mypyIni, configService)
                else -> clearMypyIniConfig(configService)
            }
            runWriteAction {
                sitePackages.asSequence().mapNotNull { sitePackage ->
                    sitePackage.findChild("pydantic")
                }.firstOrNull()?.let {
                    copyPydanticStub(it, findSkeletonsDir(sdk!!), true)
                }
            }
        }

        VirtualFileManager.getInstance().addAsyncFileListener(
                { events ->
                    object : AsyncFileListener.ChangeApplier {
                        override fun afterVfsChange() {
                            if (project.isDisposed) return
                            try {
                                val projectFiles = events
                                        .asSequence()
                                        .filter {
                                            it is VFileContentChangeEvent || it is VFileMoveEvent || it is VFileCopyEvent || it is VFileCreateEvent || it is VFilePropertyChangeEvent
                                        }
                                        .mapNotNull { it.file }
                                        .filter {
                                            ProjectFileIndex.getInstance(project).isInContent(it) ||
                                                    ProjectFileIndex.getInstance(project).isInLibrary(it)
                                        }

                                if (projectFiles.count() == 0) return
                                val pyprojectToml = configService.pyprojectToml ?: defaultPyProjectToml
                                val mypyIni = configService.mypyIni ?: defaultMypyIni
                                val newSdk = getSdk(project)
                                if (sdk != newSdk) {
                                    skeletons = newSdk?.let { findSkeletonsDir(it) }
                                    sitePackages = getSitePackage(newSdk)
                                    sdk = newSdk
                                }

                                val pydanticPackage = sitePackages.asSequence().map { it.findChild("pydantic") }.firstOrNull()
                                invokeAfterPsiEvents {
                                    val libraries = projectFiles.mapNotNull {
                                        when (it.path) {
                                            pyprojectToml -> {
                                                loadPyprojecToml(project, it, configService)
                                                null
                                            }
                                            mypyIni -> {
                                                loadMypyIni(it, configService)
                                                null
                                            }
                                            else -> it
                                        }
                                    }.filter { targetFile ->
                                        pydanticPackage?.let { targetFile.path.startsWith(pydanticPackage.path) } == true
                                    }.toList()
                                    if (libraries.isEmpty()) return@invokeAfterPsiEvents
                                    runWriteAction {
                                        libraries.forEach {
                                            copyPydanticStub(it, skeletons, it.isDirectory)
                                        }
                                    }
                                }
                            } catch (e: AlreadyDisposedException) {
                            }
                        }
                    }
                },
                {}
        )
    }

    private fun clearMypyIniConfig(configService: PydanticConfigService) {
        configService.mypyInitTyped = null
        configService.mypyWarnUntypedFields = null
    }

    private fun clearPyProjectTomlConfig(configService: PydanticConfigService) {
        configService.parsableTypeMap.clear()
        configService.acceptableTypeMap.clear()
        configService.parsableTypeHighlightType = ProblemHighlightType.WARNING
        configService.acceptableTypeHighlightType = ProblemHighlightType.WEAK_WARNING
    }

    private fun fromIniBoolean(text: String?): Boolean? {
        return when (text) {
            "True" -> true
            "False" -> false
            else -> null
        }
    }

    private fun loadMypyIni(config: VirtualFile, configService: PydanticConfigService) {
        try {
            val ini = Ini(config.inputStream)
            val prefs = IniPreferences(ini)
            val pydanticMypy = prefs.node("pydantic-mypy")
            configService.mypyInitTyped = fromIniBoolean(pydanticMypy["init_typed", null])
            configService.mypyWarnUntypedFields = fromIniBoolean(pydanticMypy["warn_untyped_fields", null])
        } catch (t: Throwable) {
            clearMypyIniConfig(configService)
        }
    }

    private fun loadPyprojecToml(project: Project, config: VirtualFile, configService: PydanticConfigService) {
        val result: TomlParseResult = Toml.parse(config.inputStream)

        val table = result.getTableOrEmpty("tool.pydantic-pycharm-plugin")
        if (table.isEmpty) {
            clearPyProjectTomlConfig(configService)
            return
        }

        val context = TypeEvalContext.codeAnalysis(project, null)
        configService.parsableTypeMap = getTypeMap(project, "parsable-types", table, context)
        configService.acceptableTypeMap = getTypeMap(project, "acceptable-types", table, context)

        configService.parsableTypeHighlightType = getHighlightLevel(table, "parsable-type-highlight", ProblemHighlightType.WARNING)
        configService.acceptableTypeHighlightType = getHighlightLevel(table, "acceptable-type-highlight", ProblemHighlightType.WEAK_WARNING)
    }

    private fun getHighlightLevel(table: TomlTable, path: String, default: ProblemHighlightType): ProblemHighlightType {
        return when (table.get(path) as? String) {
            "warning" -> {
                ProblemHighlightType.WARNING
            }
            "weak_warning" -> {
                ProblemHighlightType.WEAK_WARNING
            }
            "disable" -> {
                ProblemHighlightType.INFORMATION
            }
            else -> {
                default
            }
        }
    }

    private fun getTypeMap(project: Project, path: String, table: TomlTable, context: TypeEvalContext): MutableMap<String, List<String>> {
        val temporaryTypeMap = mutableMapOf<String, List<String>>()

        val parsableTypeTable = table.getTableOrEmpty(path).toMap()
        parsableTypeTable.entries.forEach { (key, value) ->
            val name = when (val psiElement = getPsiElementByQualifiedName(QualifiedName.fromDottedString(key), project, context)) {
                is PyQualifiedNameOwner -> psiElement.qualifiedName!!
                else -> key
            }
            run {
                if (value is TomlArray) {
                    value.toList().filterIsInstance<String>().let {
                        if (it.isNotEmpty()) {
                            temporaryTypeMap[name] = it
                        }
                    }
                }
            }
        }
        return temporaryTypeMap
    }

    override fun runActivity(project: Project) {
        if (ApplicationManager.getApplication().isUnitTestMode) return
        if (project.isDisposed) return
        initializeFileLoader(project)
    }

    private fun invokeAfterPsiEvents(runnable: () -> Unit) {
        val wrapper = {
            when {
                NoAccessDuringPsiEvents.isInsideEventProcessing() -> invokeAfterPsiEvents(runnable)
                else -> runnable()
            }
        }
        ApplicationManager.getApplication().invokeLater(wrapper, { false })
    }
}
