package com.koxudaxi.pydantic


import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.PyClassTypeImpl

class PydanticDynamicModelClassType(source: PyClass, isDefinition: Boolean, val members: List<PyCustomMember>, private val memberResolver: Map<String, PyElement>) : PyClassTypeImpl(source, isDefinition) {
    fun resolveMember(name: String): PyElement? = memberResolver[name]
}