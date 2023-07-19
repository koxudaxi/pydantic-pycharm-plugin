package com.koxudaxi.pydantic

import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.*
class PydanticReferenceContributor : PsiReferenceContributor() {

    private object Holder {
        val VALIDATOR_FIELD: PatternCondition<PsiElement> =
            object : PatternCondition<PsiElement>("validator field") {
                override fun accepts(element: PsiElement, context: ProcessingContext): Boolean {
                    if (element !is PyStringLiteralExpression) {
                        return false
                    }
                    return isValidatorField(element)
                }
            }
    }


    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registerValidatorFieldReference(
            registrar,
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    return arrayOf(PydanticValidatorFieldReference((element as PyStringLiteralExpression)))
                }
            })
    }

    companion object {
        private fun registerValidatorFieldReference(
            registrar: PsiReferenceRegistrar,
            provider: PsiReferenceProvider
        ) {
            registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(
                    PyStringLiteralExpression::class.java
                ).withParents(
                        PyArgumentList::class.java,
                        PyCallExpression::class.java,
                        PyDecorator::class.java,
                        PyDecoratorList::class.java,
                        PyFunction::class.java,
                        PyStatementList::class.java,
                        PyClass::class.java).with(Holder.VALIDATOR_FIELD), provider
            )
        }
    }
}
