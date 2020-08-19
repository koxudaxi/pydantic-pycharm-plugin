package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.*

class PydanticDynamicModelMemberProvider : PyClassMembersProviderBase() {
    override fun resolveMember(type: PyClassType, name: String, location: PsiElement?, resolveContext: PyResolveContext): PsiElement? {
        if (type is PydanticDynamicModelClassType) {
            type.resolveMember(name)?.let { return it }
        }
        return super.resolveMember(type, name, location, resolveContext)
    }

    override fun getMembers(clazz: PyClassType?, location: PsiElement?, context: TypeEvalContext): MutableCollection<PyCustomMember> {
        if (clazz !is PydanticDynamicModelClassType) return mutableListOf()
        return clazz.members.toMutableList()
    }
}
