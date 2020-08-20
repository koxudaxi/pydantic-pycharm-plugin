package com.koxudaxi.pydantic

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.impl.PyClassImpl
import com.jetbrains.python.psi.types.PyClassLikeType
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticDynamicModel(astNode: ASTNode, val baseModel: PyClass) : PyClassImpl(astNode) {
    override fun getSuperClassTypes(context: TypeEvalContext): MutableList<PyClassLikeType> {
        return baseModel.getType(context)?.let {
            mutableListOf(it)
        } ?: mutableListOf()
    }
}