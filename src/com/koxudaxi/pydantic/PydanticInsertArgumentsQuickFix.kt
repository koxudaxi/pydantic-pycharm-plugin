package com.koxudaxi.pydantic

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.nullize
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.PyCallableParameter
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticInsertArgumentsQuickFix(val onlyRequired: Boolean) : LocalQuickFix, IntentionAction, HighPriorityAction {
    private val pydanticTypeProvider = PydanticTypeProvider()
    override fun getText(): String = name

    override fun getFamilyName(): String =
            when {
                onlyRequired -> "Insert required arguments"
                else -> "Insert all arguments"
            }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        ApplicationManager.getApplication().runWriteAction {
            getPyCallExpressionAtCaret(file, editor)?.let { runFix(project, file, it) }
        }
    }

    override fun startInWriteAction(): Boolean = true

    fun runFix(project: Project, file: PsiFile, originalElement: PsiElement): PyCallExpression? {
        if (originalElement !is PyCallExpression) return null
        if (file !is PyFile) return null
        val newEl = originalElement.copy() as PyCallExpression
        val context = TypeEvalContext.userInitiated(originalElement.project, originalElement.containingFile)
        val unFilledArguments = getPydanticUnFilledArguments(null, originalElement, pydanticTypeProvider, context).let {
            when {
                onlyRequired -> it.filter { arguments -> arguments.required }
                else -> it
            }
        }.nullize() ?: return null
        val elementGenerator = PyElementGenerator.getInstance(project)
        unFilledArguments.forEach {
            val newArg = elementGenerator.createKeywordArgument(file.languageLevel, it.name, getDefaultArguments(it))
            addKeywordArgument(newEl, newArg)
        }
        originalElement.replace(newEl)
        return newEl
    }

    private fun getDefaultArguments(pyCallableParameter: PyCallableParameter): String {
        return pyCallableParameter.defaultValueText?.let { if (it == "...") "" else it } ?: ""
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        runFix(project, descriptor.psiElement.containingFile, descriptor.psiElement)
    }

}