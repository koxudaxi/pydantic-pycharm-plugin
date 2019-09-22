package com.koxudaxi.pydantic

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.jetbrains.python.PyNames
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.codeInsight.PyCodeInsightSettings
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.impl.PyPsiUtils
import java.util.regex.Pattern

class PydanticTypedValidatorMethodHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(character: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        if (DumbService.isDumb(project) || fileType !is PythonFileType) return Result.CONTINUE
        if (character == '(') {
            if (!PyCodeInsightSettings.getInstance().INSERT_SELF_FOR_METHODS) {
                return Result.CONTINUE
            }
            val document = editor.document
            val documentManager = PsiDocumentManager.getInstance(project)
            val offset = editor.caretModel.offset
            val lineNumber = document.getLineNumber(offset)

            val linePrefix = document.getText(TextRange.create(document.getLineStartOffset(lineNumber), offset))
            if (!DEF_THEN_IDENTIFIER.matcher(linePrefix).matches()) {
                return Result.CONTINUE
            }
            documentManager.commitDocument(document)

            val token = file.findElementAt(offset - 1) ?: return Result.CONTINUE

            val tokenNode = token.node
            if (tokenNode != null && tokenNode.elementType === PyTokenTypes.IDENTIFIER) {
                val maybeDef = PyPsiUtils.getPrevNonCommentSibling(token.prevSibling, false) ?: return Result.CONTINUE
                val defNode = maybeDef.node
                if (defNode != null && defNode.elementType === PyTokenTypes.DEF_KEYWORD) {
                    val pyFunction = token.parent as? PyFunction ?: return Result.CONTINUE
                    if (!isValidatorMethod(pyFunction)) return Result.CONTINUE
                    val settings = CodeStyle.getLanguageSettings(file, PythonLanguage.getInstance())
                    val textToType = StringBuilder()
                    textToType.append("(")
                    if (settings.SPACE_WITHIN_METHOD_PARENTHESES) {
                        textToType.append(" ")
                    }
                    textToType.append(PyNames.CANONICAL_CLS)
                    if (settings.SPACE_WITHIN_METHOD_PARENTHESES) {
                        textToType.append(" ")
                    }

                    textToType.append(", )")
                    val caretOffset = editor.caretModel.offset
                    val chars = editor.document.charsSequence
                    if (caretOffset == chars.length || chars[caretOffset] != ':') {
                        textToType.append(':')
                    }
                    EditorModificationUtil.insertStringAtCaret(editor, textToType.toString(), true, 3 + PyNames.CANONICAL_CLS.length)
                    return Result.STOP
                }
            }
        }
        return Result.CONTINUE
    }

    companion object {
        private val DEF_THEN_IDENTIFIER = Pattern.compile(".*\\bdef\\s+" + PyNames.IDENTIFIER_RE)
    }
}
