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
        val pyCallableType = originalElement.getPyCallableType(context) ?: return null
        val pyClass = pyCallableType.getReturnType(context)?.pyClassTypes?.firstOrNull()?.pyClass ?: return null
        if (!isPydanticModel(pyClass, true, context)) return null
        val unFilledArguments =
            getPydanticUnFilledArguments(pyCallableType, originalElement, context, pyClass.isPydanticDataclass).let {
                when {
                    onlyRequired -> it.filter { arguments -> arguments.required }
                    else -> it
                }
            }.mapNotNull {
                it.name?.let { name -> name to it}
            }.nullize()?.toMap() ?: return null
        val elementGenerator = PyElementGenerator.getInstance(project)
        val ellipsis = elementGenerator.createEllipsis()
        val pydanticVersion = PydanticCacheService.getVersion(project, context)
        val fields = (listOf(pyClass) + getAncestorPydanticModels(pyClass, true, context)).flatMap {
            it.classAttributes.filter { attribute -> unFilledArguments.contains(attribute.name) }
                .mapNotNull { attribute -> attribute.name?.let { name -> name to attribute }}
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