package com.koxudaxi.pydantic

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.IndexingTestUtil
import com.jetbrains.python.sdk.PythonSdkUtil

class PydanticTypedValidatorMethodHandlerTest : PydanticTestCase() {

    fun testInsertsClsForV1Validator() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testDoesNotLoadPydanticVersionDuringWriteAction() {
        val fixture = myFixture!!
        configureByFile()
        val sdk = PythonSdkUtil.findPythonSdk(fixture.module)!!
        runWriteAction {
            ProjectRootManager.getInstance(fixture.project).projectSdk = sdk
        }
        IndexingTestUtil.waitUntilIndexesAreReady(fixture.project)
        DumbService.getInstance(fixture.project).waitForSmartMode()
        PydanticCacheService.clear(fixture.project)
        Registry.get("ide.run.blocking.cancellable.assert.in.tests")
            .setValue(true, fixture.testRootDisposable)

        fixture.type('(')
        fixture.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testIgnoresUnrelatedDecorator() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testIgnoresNonReferenceDecorator() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testIgnoresEmptyDecorator() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testIgnoresResolvedNonValidatorDecorator() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }
}

class PydanticTypedValidatorMethodHandlerV2Test : PydanticTestCase("v2") {

    fun testInsertsClsForV2Validator() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testInsertsClsForModelValidatorBefore() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testInsertsClsForModelValidatorWrap() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testInsertsClsForModelValidatorWithoutCall() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testInsertsClsForModelValidatorWithoutMode() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testInsertsClsForInvalidModelValidatorMode() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }

    fun testKeepsSelfForModelValidatorAfter() {
        configureByFile()
        myFixture!!.type('(')
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")
    }
}
