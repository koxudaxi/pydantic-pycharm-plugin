package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import org.apache.tuweni.toml.Toml
import org.apache.tuweni.toml.TomlArray
import org.apache.tuweni.toml.TomlParseResult
import org.apache.tuweni.toml.TomlTable
import org.ini4j.Ini
import org.ini4j.IniPreferences


class PydanticInitializer : StartupActivity {

    private fun getDefaultPyProjectTomlPath(project: Project): String {
        return project.basePath + "/pyproject.toml"
    }

    private fun getDefaultMypyIniPath(project: Project): String {
        return project.basePath + "/mypy.ini"
    }

    private fun initializeFileLoader(project: Project, configService: PydanticConfigService) {
        val defaultPyProjectToml = getDefaultPyProjectTomlPath(project)
        val defaultMypyIni = getDefaultMypyIniPath(project)
        VirtualFileManager.getInstance().addAsyncFileListener(
                { events ->
                    object : AsyncFileListener.ChangeApplier {
                        override fun afterVfsChange() {
                            if (project.isDisposed) return
                            events
                                    .asSequence()
                                    .filter {
                                        it is VFileContentChangeEvent || it is VFileMoveEvent || it is VFileCopyEvent
                                    }
                                    .mapNotNull { it.file }
                                    .filter { ProjectFileIndex.getInstance(project).isInContent(it) }
                                    .forEach {
                                        when (it.path) {
                                            configService.pyprojectToml
                                                    ?: defaultPyProjectToml -> loadPyprojecToml(it, configService)
                                            configService.mypyIni ?: defaultMypyIni -> loadMypyIni(it, configService)
                                        }
                                    }
                        }
                    }
                },
                {}
        )

        when (val pyprojectToml = LocalFileSystem.getInstance()
                .findFileByPath(configService.pyprojectToml ?: defaultPyProjectToml)
            ) {
            is VirtualFile -> loadPyprojecToml(pyprojectToml, configService)
            else -> clearPyProjectTomlConfig(configService)
        }

        when (val mypyIni = LocalFileSystem.getInstance()
                .findFileByPath(configService.mypyIni ?: defaultMypyIni)
            ) {
            is VirtualFile -> loadMypyIni(mypyIni, configService)
            else -> clearMypyIniConfig(configService)
        }
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

    private fun fromIniBoolean(text: String): Boolean? {
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

    private fun loadPyprojecToml(config: VirtualFile, configService: PydanticConfigService) {
        val result: TomlParseResult = Toml.parse(config.inputStream)

        val table = result.getTableOrEmpty("tool.pydantic-pycharm-plugin")
        if (table.isEmpty) {
            clearPyProjectTomlConfig(configService)
            return
        }

        val temporaryParsableTypeMap = getTypeMap("parsable-types", table)
        if (configService.parsableTypeMap != temporaryParsableTypeMap) {
            configService.parsableTypeMap = temporaryParsableTypeMap
        }

        val temporaryAcceptableTypeMap = getTypeMap("acceptable-types", table)
        if (configService.acceptableTypeMap != temporaryAcceptableTypeMap) {
            configService.acceptableTypeMap = temporaryAcceptableTypeMap
        }

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

    private fun getTypeMap(path: String, table: TomlTable): MutableMap<String, List<String>> {

        val temporaryTypeMap = mutableMapOf<String, List<String>>()

        val parsableTypeTable = table.getTableOrEmpty(path).toMap()
        parsableTypeTable.entries.forEach { (key, value) ->
            run {
                if (value is TomlArray) {
                    value.toList().filterIsInstance<String>().let {
                        if (it.isNotEmpty()) {
                            temporaryTypeMap[key] = it
                        }
                    }
                }
            }
        }
        return temporaryTypeMap
    }

    override fun runActivity(project: Project) {
        val configService = PydanticConfigService.getInstance(project)
        initializeFileLoader(project, configService)
    }
}