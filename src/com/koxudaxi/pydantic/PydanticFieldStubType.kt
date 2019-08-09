package com.koxudaxi.pydantic

import com.jetbrains.python.psi.impl.stubs.CustomTargetExpressionStubType

import com.intellij.psi.stubs.StubInputStream
import com.jetbrains.python.psi.PyTargetExpression
import java.io.IOException

class PydanticFieldStubType : CustomTargetExpressionStubType<PydanticFieldStub>() {

    override fun createStub(psi: PyTargetExpression): PydanticFieldStub? {
        return PydanticFieldStubImpl.create(psi)
    }

    @Throws(IOException::class)
    override fun deserializeStub(stream: StubInputStream): PydanticFieldStub? {
        return PydanticFieldStubImpl.deserialize(stream)
    }
}