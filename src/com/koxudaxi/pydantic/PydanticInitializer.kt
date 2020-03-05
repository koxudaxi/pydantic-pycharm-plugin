package com.koxudaxi.pydantic

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
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

class PydanticInitializer : StartupActivity {

    private fun getDefaultPyProjectTomlPathPath(project: Project): String {
//        Notifications.Bus.notify(Notification("pydantic", "Success", project.basePath + "", NotificationType.INFORMATION))
        return project.basePath + "/pyproject.toml"
    }


    private fun initializeVirtualUnionMap(project: Project, configService: PydanticConfigService) {
        val pyprojectTomlDefault = configService.pyprojectToml ?: getDefaultPyProjectTomlPathPath(project)
        VirtualFileManager.getInstance().addAsyncFileListener(
                { events ->
                    object : AsyncFileListener.ChangeApplier {
                        override fun afterVfsChange() {
                            if(project.isDisposed) return
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
            configService.virtualUnionMap.clear()
        }
    }

    private fun loadPyprojecToml(config: VirtualFile, configService: PydanticConfigService) {
        val temporaryVirtualUnionMap = mutableMapOf<String, List<String>>()

        val result: TomlParseResult = Toml.parse(config.inputStream)
        val acceptTypeTable = result.getTableOrEmpty("tool.pydantic-pycharm-plugin.accept-types").toMap()
        acceptTypeTable.entries.forEach { (key, value) ->
            run {
                if (value is TomlArray) {
                    value.toList().filterIsInstance<String>().let {
                        if (it.isNotEmpty()) {
                            temporaryVirtualUnionMap[key] = it
                        }
                    }
                }
            }
        }
        if (configService.virtualUnionMap != temporaryVirtualUnionMap) {
            configService.virtualUnionMap = temporaryVirtualUnionMap
//            Notifications.Bus.notify(Notification("pydantic", "Success", "Update accept-types", NotificationType.INFORMATION))
        }
    }

    override fun runActivity(project: Project) {
        val configService = PydanticConfigService.getInstance(project)
        initializeVirtualUnionMap(project, configService)
    }
}