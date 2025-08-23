package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.jetbrains.python.psi.PyTargetExpression


open class PydanticCompletionV18Test : PydanticTestCase(version = "v18") {


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
            "__concrete__",
            "__parameters__",
            "___slots__",
            "Generic",
            "Dict",
            "Optional",
        )
        val actual = myFixture!!.completeBasic().filter {
            it!!.psiElement is PyTargetExpression
        }.filterNot {
            excludes.contains(it!!.lookupString)
        }.mapNotNull {
            Pair(it!!.lookupString, LookupElementPresentation.renderElement(it).typeText ?: "null")
        }
        assertEquals(fieldNames, actual)
    }

    // TODO: Generic field completion broken in PyCharm 2025.2
    fun _disabled_testGenericField() {
        doFieldTest(
            listOf(
                Pair("a", "Type[int] A"),
                Pair("b", "List[str] A"),
                Pair("c", "Dict[float, bytes] A"),
                Pair("hij", "Optional[bool]=None B"),
            )
        )
    }


    // TODO: Generic keyword argument completion broken in PyCharm 2025.2
    fun _disabled_testGenericKeywordArgument() {
        doFieldTest(
            listOf(
                Pair("a=", "Type[int] A"),
                Pair("b=", "List[str] A"),
                Pair("c=", "Dict[float, bytes] A"),
                Pair("hij=", "Optional[bool]=None B"),
                Pair("AT", "null"),
                Pair("BT", "null"),
                Pair("CT", "null"),
                Pair("DT", "null"),
                Pair("ET", "null"))
        )
    }
    // TODO: Override init keyword argument completion broken in PyCharm 2025.2
    fun _disabled_testOverrideInitKeywordArgument() {
        doFieldTest(
            listOf()
        )
    }
    fun testOverrideInitField() {
        doFieldTest(
            listOf(Pair("abc", "str='123' A"))
        )
    }
    fun testAliasNameKeywordArgument() {
        doFieldTest(
            listOf(
                Pair("abc=", "str='123' A"),
                Pair("klm=", "str A")
            )
        )
    }
    fun testInsertedArgument() {
        doFieldTest(
            listOf(
                Pair("abc_efg", "str='123' A"),
                Pair("abc_xyz", "str='456' A")
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
