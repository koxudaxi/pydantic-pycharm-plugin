package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.*

class PydanticDynamicModelMemberProvider : PyClassMembersProviderBase() {
    override fun resolveMember(type: PyClassType, name: String, location: PsiElement?, resolveContext: PyResolveContext): PsiElement? {
        val pyClass =  type.pyClass
        if (pyClass is PydanticDynamicModel && !type.isDefinition)
            pyClass.resolveMember(name)?.let { return it }
        return super.resolveMember(type, name, location, resolveContext)
    }

    override fun getMembers(clazz: PyClassType?, location: PsiElement?, context: TypeEvalContext): MutableCollection<PyCustomMember> {
        if (clazz == null || clazz.isDefinition) return mutableListOf()
        val pyClass =  clazz.pyClass
        return if (pyClass is PydanticDynamicModel) {
            pyClass.members.toMutableList()
        } else {
            mutableListOf()
        }
    }
}
