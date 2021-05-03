package com.koxudaxi.pydantic

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.jetbrains.python.codeInsight.PyInjectionUtil
import com.jetbrains.python.codeInsight.PyInjectorBase
import com.jetbrains.python.codeInsight.regexp.PythonRegexpLanguage
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.StringLiteralExpression

class PydanticRegexInjector : PyInjectorBase() {
    override fun registerInjection(
        registrar: MultiHostRegistrar,
        context: PsiElement,
    ): PyInjectionUtil.InjectionResult {
        val result = super.registerInjection(registrar, context)
        return if (result === PyInjectionUtil.InjectionResult.EMPTY &&
            context is PsiLanguageInjectionHost &&
            context.getContainingFile() is PyFile &&
            context is StringLiteralExpression && isPydanticRegex(context)
        ) {
            return registerPyElementInjection(registrar, context)
        } else result

    }

    override fun getInjectedLanguage(context: PsiElement): Language? {
        return null
    }

    companion object {
        private fun registerPyElementInjection(
            registrar: MultiHostRegistrar,
            host: PsiLanguageInjectionHost,
        ): PyInjectionUtil.InjectionResult {
            val text = host.text
            registrar.startInjecting(PythonRegexpLanguage.INSTANCE)
            registrar.addPlace("", "", host, TextRange(0, text.length))
            registrar.doneInjecting()
            return PyInjectionUtil.InjectionResult(true, true)
        }
    }
}
