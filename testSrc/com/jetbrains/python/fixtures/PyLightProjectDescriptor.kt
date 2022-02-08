/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.fixtures

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.PythonMockSdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.roots.OrderRootType
import com.jetbrains.python.psi.LanguageLevel

/**
 * Project descriptor (extracted from [com.jetbrains.python.fixtures.PyTestCase]) and should be used with it.
 *
 * @author Ilya.Kazakevich
 */
class PyLightProjectDescriptor private constructor(private val myName: String?, private val myLevel: LanguageLevel) :
    LightProjectDescriptor() {
    constructor(level: LanguageLevel) : this(null, level)
    constructor(name: String) : this(name, LanguageLevel.getLatest())

    override fun getSdk(): Sdk {
        return if (myName == null) PythonMockSdk.create(myLevel, *additionalRoots) else PythonMockSdk.create(myName)
    }

    /**
     * @return additional roots to add to mock python
     * @apiNote ignored when name is provided.
     */
    private val additionalRoots: Array<VirtualFile>
        get() = VirtualFile.EMPTY_ARRAY

    fun createLibrary(model: ModifiableRootModel, name: String?, path: String) {
        val modifiableModel = model.moduleLibraryTable.createLibrary(name).modifiableModel
        val home = LocalFileSystem.getInstance().refreshAndFindFileByPath(PathManager.getHomePath() + path)
        modifiableModel.addRoot(home!!, OrderRootType.CLASSES)
        modifiableModel.commit()
    }
}