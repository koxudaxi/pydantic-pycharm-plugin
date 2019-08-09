package com.koxudaxi.pydantic

import com.jetbrains.python.psi.impl.stubs.CustomTargetExpressionStub

interface PydanticFieldStub : CustomTargetExpressionStub {
    fun hasDefault(): Boolean

    fun hasDefaultFactory(): Boolean

    fun initValue(): Boolean
}
