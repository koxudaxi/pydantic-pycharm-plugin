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

class PydanticInsertArgumentsQuickFix(private val onlyRequired: Boolean) : LocalQuickFix, IntentionAction,
    HighPriorityAction {
    private val pydanticTypeProvider = PydanticTypeProvider()
    private val pydanticDataclassTypeProvider = PydanticDataclassTypeProvider()
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
            val context = TypeEvalContext.userInitiated(project, file)
            getPydanticCallExpressionAtCaret(file, editor, context, true)?.let { runFix(project, file, it, context) }
        }
    }

    override fun startInWriteAction(): Boolean = true

    private fun runFix(
        project: Project,
        file: PsiFile,
        originalElement: PsiElement,
        context: TypeEvalContext,
    ): PyCallExpression? {
        if (originalElement !is PyCallExpression) return null
        if (file !is PyFile) return null
        val newEl = originalElement.copy() as PyCallExpression
        val pyClass = getPydanticPyClass(originalElement, context, true) ?: return null
        val pydanticType = if (pyClass.isPydanticDataclass) {
            pydanticDataclassTypeProvider.getDataclassCallableType(pyClass, context, originalElement)
        } else {
            pydanticTypeProvider.getPydanticTypeForClass(pyClass, context, true, originalElement) ?: return null
        } ?: return null
        val unFilledArguments =
            getPydanticUnFilledArguments(pydanticType, originalElement, context).let {
                when {
                    onlyRequired -> it.filter { arguments -> arguments.required }
                    else -> it
                }
            }.map {
                it.name to it
            }.filterIsInstance<Pair<String, PyCallableParameter>>().nullize()?.toMap() ?: return null
        val elementGenerator = PyElementGenerator.getInstance(project)
        val ellipsis = elementGenerator.createEllipsis()
        val pydanticVersion = PydanticCacheService.getVersion(project, context)
        val fields = (listOf(pyClass) + getAncestorPydanticModels(pyClass, true, context)).flatMap {
            it.classAttributes.filter { attribute -> unFilledArguments.contains(attribute.name) }
                .map { attribute -> attribute.name to attribute }
                .filterIsInstance<Pair<String, PyTargetExpression>>()
        }.toMap()

        unFilledArguments.forEach {
            val newArg = elementGenerator.createKeywordArgument(
                file.languageLevel,
                it.key,
                fields[it.key]?.let { field ->
                    pydanticTypeProvider.getDefaultValueForParameter(
                        field,
                        ellipsis,
                        context,
                        pydanticVersion,
                        pyClass.isPydanticDataclass
                    )?.text?.takeIf { defaultValue -> defaultValue != "..." }
                } ?: ""
            )
            addKeywordArgument(newEl, newArg)
        }
        originalElement.replace(newEl)
        return newEl
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        descriptor.psiElement.containingFile.let {
            runFix(project, it, descriptor.psiElement, TypeEvalContext.userInitiated(project, it))
        }
    }

}