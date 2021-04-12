package com.koxudaxi.pydantic

import com.jetbrains.python.psi.types.PyCallableType
import com.jetbrains.python.psi.types.PyCallableTypeImpl
import com.jetbrains.python.psi.types.PyClassTypeImpl

class PydanticDynamicModelClassType(private val source: PydanticDynamicModel, isDefinition: Boolean) :
    PyClassTypeImpl(source, isDefinition) {

    val pyCallableType: PyCallableType
        get() = PyCallableTypeImpl(
            source.attributes.values.map { attribute -> attribute.pyCallableParameter },
            this.toInstance()
        )
}