package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.documentation.PythonDocumentationLinkProvider
import com.jetbrains.python.psi.PyQualifiedNameOwner
import org.jsoup.nodes.Document
import java.util.function.Function

class PydanticDocumentationLinkProvider : PythonDocumentationLinkProvider {
    private val urls = mapOf(
        BASE_MODEL_Q_NAME to "https://docs.pydantic.dev/usage/models/#basic-model-usage",
        VALIDATOR_Q_NAME to "https://docs.pydantic.dev/usage/validators/",
        VALIDATOR_SHORT_Q_NAME to "https://docs.pydantic.dev/usage/validators/",
        ROOT_VALIDATOR_Q_NAME to "https://docs.pydantic.dev/usage/validators/#root-validators",
        ROOT_VALIDATOR_SHORT_Q_NAME to "https://docs.pydantic.dev/usage/validators/#root-validators",
        DATA_CLASS_Q_NAME to "https://docs.pydantic.dev/usage/dataclasses/",
        BASE_SETTINGS_Q_NAME to "https://docs.pydantic.dev/usage/settings/",
        BASE_CONFIG_Q_NAME to "https://docs.pydantic.dev/usage/model_config/"
    )

    override fun getExternalDocumentationUrl(element: PsiElement, originalElement: PsiElement): String? {
        val qualifiedName = (element as? PyQualifiedNameOwner)?.qualifiedName ?: return null
        if (!qualifiedName.startsWith("pydantic.")) return null
        return urls[qualifiedName]
        }

    override fun quickDocExtractor(namedElement: PsiNamedElement): Function<Document, String>? {
                return null
    }
}
