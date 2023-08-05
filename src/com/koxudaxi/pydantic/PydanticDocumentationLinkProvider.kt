package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.documentation.PythonDocumentationLinkProvider
import com.jetbrains.python.psi.PyQualifiedNameOwner
import org.jsoup.nodes.Document
import java.util.function.Function

class PydanticDocumentationLinkProvider : PythonDocumentationLinkProvider {
    private val v1Urls = mapOf(
        BASE_MODEL_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/models/#basic-model-usage",
        VALIDATOR_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/validators",
        VALIDATOR_SHORT_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/validators",
        ROOT_VALIDATOR_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/validators/#root-validators",
        ROOT_VALIDATOR_SHORT_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/validators/#root-validators",
        DATA_CLASS_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/dataclasses/",
        BASE_SETTINGS_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/dataclasses/",
        BASE_CONFIG_Q_NAME to "https://docs.pydantic.dev/dev-1/usage/model_config/"
    )
    private val v2Urls = mapOf(
        BASE_MODEL_Q_NAME to "https://docs.pydantic.dev/dev-v2/usage/models/#basic-model-usage",
        FIELD_VALIDATOR_Q_NAME to "https://docs.pydantic.dev/dev-v2/usage/validators/#field-validators",
        FIELD_VALIDATOR_SHORT_Q_NAME to "https://docs.pydantic.dev/dev-v2/usage/validators/#field-validators",
        MODEL_VALIDATOR_Q_NAME to "https://docs.pydantic.dev/dev-v2/usage/validators/#model-validators",
        MODEL_VALIDATOR_SHORT_Q_NAME to "https://docs.pydantic.dev/dev-v2/usage/validators/#model-validators",
        DATA_CLASS_Q_NAME to "https://docs.pydantic.dev/dev-v2/usage/dataclasses/",
        BASE_SETTINGS_Q_NAME to "https://docs.pydantic.dev/dev-v2/api/pydantic_settings/#pydantic_settings.BaseSettings",
        BASE_CONFIG_Q_NAME to "https://docs.pydantic.dev/dev-v2/api/config/#pydantic.config.BaseConfig",
        CONFIG_DICT_Q_NAME to "https://docs.pydantic.dev/dev-v2/api/config/#pydantic.config.ConfigDict",
        CONFIG_DICT_SHORT_Q_NAME to "https://docs.pydantic.dev/dev-v2/api/config/#pydantic.config.ConfigDict",
    )

    override fun getExternalDocumentationUrl(element: PsiElement?, originalElement: PsiElement?): String? {
        val qualifiedName = (element as? PyQualifiedNameOwner)?.qualifiedName ?: return null
        if (!qualifiedName.startsWith("pydantic.")) return null

        val version = PydanticCacheService.getInstance(element.project)
        return when {
            version.isV2 -> v2Urls[qualifiedName]
            else -> v1Urls[qualifiedName]
        }
    }

    override fun quickDocExtractor(namedElement: PsiNamedElement): Function<Document, String>? {
                return null
    }
}
