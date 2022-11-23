package com.koxudaxi.pydantic

import com.intellij.icons.AllIcons
import com.intellij.lang.ASTNode
import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.PyClassImpl
import com.jetbrains.python.psi.types.PyCallableParameter
import com.jetbrains.python.psi.types.PyClassLikeType
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticDynamicModel(astNode: ASTNode, private val baseModel: PyClass, val attributes: Map<String, Attribute>) :
    PyClassImpl(astNode) {
    val members: List<PyCustomMember> = attributes.values.map { it.pyCustomMember }
    private val memberResolver: Map<String, PyElement> =
        attributes.entries.filterNot { it.value.isInAncestor }.associate { it.key to it.value.pyElement }

    fun resolveMember(name: String): PyElement? = memberResolver[name]

    override fun getSuperClassTypes(context: TypeEvalContext): MutableList<PyClassLikeType> {
        return baseModel.getType(context)?.let {
            mutableListOf(it)
        } ?: mutableListOf()
    }

    data class Attribute(
        val pyCallableParameter: PyCallableParameter,
        val pyCustomMember: PyCustomMember,
        val pyElement: PyElement,
        val isInAncestor: Boolean,
    )

    companion object {
        fun createAttribute(
            name: String,
            parameter: PyCallableParameter,
            originalPyExpression: PyExpression,
            context: TypeEvalContext,
            isInAncestor: Boolean,
        ): Attribute {
            val type = parameter.getType(context)
            return Attribute(parameter,
                PyCustomMember(name, null) { type }
                    .toPsiElement(originalPyExpression)
                    .withIcon(AllIcons.Nodes.Field),
                originalPyExpression,
                isInAncestor
            )
        }
    }
}