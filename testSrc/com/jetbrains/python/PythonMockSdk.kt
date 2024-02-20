// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.python.codeInsight.typing.PyTypeShed.findRootsForLanguageLevel
import com.jetbrains.python.codeInsight.userSkeletons.PyUserSkeletonsUtil
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PythonSdkUtil
import org.jdom.Element
import org.jetbrains.annotations.NonNls
import java.io.File
import java.util.function.Consumer

import com.jetbrains.python.sdk.PythonSdkType.MOCK_PY_MARKER_KEY

/**
 * @author yole
 */
object PythonMockSdk {
    fun create(name: String): Sdk {
        return create(name, LanguageLevel.getLatest())
    }

    fun create(level: LanguageLevel, vararg additionalRoots: VirtualFile): Sdk {
        return create("MockSdk", level, *additionalRoots)
    }

    private fun create(name: String, level: LanguageLevel, vararg additionalRoots: VirtualFile): Sdk {
        return create(name, PyMockSdkType(level), level, *additionalRoots)
    }

    fun create(
        pathSuffix: String,
        sdkType: SdkTypeId,
        level: LanguageLevel,
        vararg additionalRoots: VirtualFile
    ): Sdk {
        val sdkName = "Mock " + PyNames.PYTHON_SDK_ID_NAME + " " + level.toPythonVersion()
        return create(sdkName, pathSuffix, sdkType, level, *additionalRoots)
    }

    fun create(
        name: String,
        pathSuffix: String,
        sdkType: SdkTypeId,
        level: LanguageLevel,
        vararg additionalRoots: VirtualFile
    ): Sdk {
        val mockSdkPath = PythonTestUtil.testDataPath + "/" + pathSuffix
        val sdk = ProjectJdkTable.getInstance().createSdk(name, sdkType)
        val sdkModificator = sdk.sdkModificator
        sdkModificator.homePath = "$mockSdkPath/bin/python"
        sdkModificator.versionString = toVersionString(level)

        createRoots(mockSdkPath, level).forEach(Consumer { vFile: VirtualFile? ->
            sdkModificator.addRoot(vFile!!, OrderRootType.CLASSES)
        })

        additionalRoots.forEach { vFile ->
            sdkModificator.addRoot(vFile, OrderRootType.CLASSES)
        }

        val application = ApplicationManager.getApplication()
        val runnable = Runnable { sdkModificator.commitChanges() }
        if (application.isDispatchThread) {
            application.runWriteAction(runnable)
        } else {
            application.invokeAndWait { application.runWriteAction(runnable) }
        }
        sdk.putUserData(MOCK_PY_MARKER_KEY, true);
        return sdk
    }

    private fun createRoots(@NonNls mockSdkPath: String, level: LanguageLevel): List<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        val localFS = LocalFileSystem.getInstance()
        ContainerUtil.addIfNotNull(result, localFS.refreshAndFindFileByIoFile(File(mockSdkPath, "Lib")))
        ContainerUtil.addIfNotNull(
            result,
            localFS.refreshAndFindFileByIoFile(File(mockSdkPath, PythonSdkUtil.SKELETON_DIR_NAME))
        )
        ContainerUtil.addIfNotNull(result, PyUserSkeletonsUtil.getUserSkeletonsDirectory())
        result.addAll(findRootsForLanguageLevel(level))
        return result
    }

    private fun toVersionString(level: LanguageLevel): String {
        return "Python " + level.toPythonVersion()
    }

    private class PyMockSdkType(private val myLevel: LanguageLevel) : SdkTypeId {
        override fun getName(): String {
            return PyNames.PYTHON_SDK_ID_NAME
        }

        override fun getVersionString(sdk: Sdk): String {
            return toVersionString(myLevel)
        }

        override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}
        override fun loadAdditionalData(currentSdk: Sdk, additional: Element): SdkAdditionalData? {
            return null
        }
    }
}