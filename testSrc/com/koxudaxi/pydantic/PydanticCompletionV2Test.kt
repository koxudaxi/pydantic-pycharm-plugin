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
            "__pydantic_generic_typevars_map__ = ",
            "__pydantic_model_complete__ = ",
            "__pydantic_core_schema__ = ",
            "__pydantic_generic_parameters__ = ",
            "__pydantic_parent_namespace__ = "
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
}
