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

class PydanticInitializer : StartupActivity {

    private fun getDefaultPyProjectTomlPathPath(project: Project): String {
        return project.basePath + "/pyproject.toml"
    }


    private fun initializeParsableTypeMap(project: Project, configService: PydanticConfigService) {
        val pyprojectTomlDefault = configService.pyprojectToml ?: getDefaultPyProjectTomlPathPath(project)
        VirtualFileManager.getInstance().addAsyncFileListener(
                { events ->
                    object : AsyncFileListener.ChangeApplier {
                        override fun afterVfsChange() {
                            if (project.isDisposed) return
                            val configFile = events
                                    .asSequence()
                                    .filter {
                                        it is VFileContentChangeEvent || it is VFileMoveEvent || it is VFileCopyEvent
                                    }
                                    .mapNotNull { it.file }
                                    .filter { ProjectFileIndex.getInstance(project).isInContent(it) }
                                    .filter { it.path == configService.pyprojectToml ?: pyprojectTomlDefault }
                                    .lastOrNull() ?: return
                            loadPyprojecToml(configFile, configService)
                        }
                    }
                },
                {}
        )
        val configFile = LocalFileSystem.getInstance()
                .findFileByPath(configService.pyprojectToml ?: pyprojectTomlDefault)
        if (configFile is VirtualFile) {
            loadPyprojecToml(configFile, configService)
        } else {
            configService.parsableTypeMap.clear()
        }
    }

    private fun loadPyprojecToml(config: VirtualFile, configService: PydanticConfigService) {
        val result: TomlParseResult = Toml.parse(config.inputStream)

        val table = result.getTableOrEmpty("tool.pydantic-pycharm-plugin") ?: return
        val temporaryParsableTypeMap = getTypeMap("parsable-types", table)
        if (configService.parsableTypeMap != temporaryParsableTypeMap) {
            configService.parsableTypeMap = temporaryParsableTypeMap
        }

        configService.parsableTypeHighlightType = getHighlightLevel(table, "parsable-type-highlight")
    }

    private fun getHighlightLevel(table: TomlTable, path: String): ProblemHighlightType {
        return when (table.get(path) as? String) {
            "weak_warning" -> {
                ProblemHighlightType.WEAK_WARNING
            }
            "disable" -> {
                ProblemHighlightType.INFORMATION
            }
            else -> {
                ProblemHighlightType.WARNING
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
        initializeParsableTypeMap(project, configService)
    }
}