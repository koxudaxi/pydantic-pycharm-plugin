package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.jetbrains.python.psi.PyTargetExpression


open class PydanticCompletionV2Test : PydanticTestCase(version = "v2") {


    private fun doFieldTest(fieldNames: List<Pair<String, String>>, additionalModules: List<String>? = null) {
        configureByFile(additionalModules)
        val excludes = listOf(
            "__annotations__",
            "__base__",
            "__bases__",
            "__basicsize__",
            "__dict__",
            "__dictoffset__",
            "__flags__",
            "__itemsize__",
            "__mro__",
            "__name__",
            "__qualname__",
            "__slots__",
            "__text_signature__",
            "__weakrefoffset__",
            "Ellipsis",
            "EnvironmentError",
            "IOError",
            "NotImplemented",
            "List",
            "Type",
            "Annotated",
            "MISSING",
            "WindowsError",
            "model_config = ",
            "model_fields = ",
            "from",
            "import",
            "lambda",
            "model_config",
            "model_fields",
            "__class_vars__",
            "__fields_set__",
            "__json_encoder__",
            "__private_attributes__",
            "__pydantic_core_schema__",
            "__pydantic_decorators__",
            "__pydantic_generic_args__",
            "__pydantic_generic_defaults__",
            "__pydantic_generic_origin__",
            "__pydantic_generic_parameters__",
            "__pydantic_generic_typevars_map__",
            "__pydantic_model_complete__",
            "__pydantic_parent_namespace__",
            "__pydantic_serializer__",
            "__pydantic_validator__",
            "__signature__",
            "__doc__",
            "__module__",
            "__pydantic_generic_typevars_map__ = ",
            "__pydantic_model_complete__ = ",
            "__pydantic_core_schema__ = ",
            "__pydantic_generic_parameters__ = ",
            "__pydantic_parent_namespace__ = ",
            "None",
            "not",
            "ConfigDict",
            "async",
            "False",
            "True"
        )
        val actual = myFixture!!.completeBasic().filter {
            it!!.psiElement is PyTargetExpression || it.psiElement == null
        }.filterNot {
            excludes.contains(it!!.lookupString)
        }.mapNotNull {
            Pair(it!!.lookupString, LookupElementPresentation.renderElement(it).typeText ?: "null")
        }
        assertEquals(fieldNames, actual)
    }

    fun testConfigDict() {
        doFieldTest(
            listOf(
                "model_config = ConfigDict()" to "null"
            )
        )
    }

    fun testValidatorField() {
        configureByFile()
        assertEquals(
            myFixture!!.completeBasic()
                .map { it!!.lookupString to LookupElementPresentation.renderElement(it).typeText }.toList(),
            listOf(
                "abc" to "A",
                "cde" to "B",
                "hij" to "B",
                "efg" to "C",
                "klm" to "C",
                "*" to "C"
            )
        )
    }
    fun testKeywordArgumentPopulateByName() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("cde=", "str A")
            )
        )
    }

    fun testFieldAnnotated() {
        doFieldTest(
                listOf(
                        Pair("a_id", "str A"),
                        Pair("abc", "str A"),
                        Pair("cde", "str='default_value' A"),
                        Pair("default_abc", "str='example' A"),
                        Pair("default_efg", "str='123' A"),
                        Pair("default_klm", "str='456' A"),
                        Pair("default_klm_positional", "str='456' A"),
                        Pair("default_nop", "str='789' A"),
                        Pair("default_nop_positional", "str='789' A"),
                        Pair("efg", "str=lambda: 123 A"),
                        Pair("klm", "str=lambda: 456 A"),
                        Pair("nop", "str=lambda: 789 A"),
                )
        )
    }
}
