package com.koxudaxi.pydantic

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticValidatorFieldReference(element: PyStringLiteralExpression) :
    PsiReferenceBase<PyStringLiteralExpression?>(element) {

    override fun resolve(): PsiElement? {
        val pyClass = PsiTreeUtil.getParentOfType(element, PyClass::class.java) ?: return null
        val typeEvalContext = TypeEvalContext.userInitiated(element.project, element.containingFile)
        return pyClass.findClassAttribute(element.stringValue, true, typeEvalContext)
    }
}
