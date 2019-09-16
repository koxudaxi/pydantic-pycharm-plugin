package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.jetbrains.python.psi.PyTargetExpression


open class PydanticCompletionTest : PydanticTestCase() {


    private fun doFieldTest(fieldNames: List<Pair<String, String>>, additionalModules: List<String>? = null) {
        configureByFile(additionalModules)

        val actual = myFixture!!.completeBasic().filter {
            it!!.psiElement is PyTargetExpression
        }.mapNotNull {

            Pair(it!!.lookupString, LookupElementPresentation.renderElement(it).typeText ?: "null")
        }
        assertEquals(fieldNames, actual)
    }

    fun testKeywordArgument() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("cde=", "str A"),
                        Pair("efg=", "str A")
                )
        )
    }

    fun testKeywordArgumentParent() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("cde=", "str A"),
                        Pair("efg=", "str B")
                )
        )
    }

    fun testKeywordArgumentInserted() {
        doFieldTest(
                listOf(
                        Pair("cde=", "str A"),
                        Pair("efg=", "str A")
                )
        )
    }

    fun testKeywordArgumentAssignValue() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testKeywordArgumentPythonClass() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testKeywordArgumentPythonFunction() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testInstance() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testInstanceParent() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str A"),
                        Pair("efg", "str B"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testInstancePythonClass() {
        doFieldTest(
                listOf(
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A")
                )
        )
    }

    fun testInstancePythonFunction() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testInstanceBroken() {
        doFieldTest(
                listOf(
                    Pair("abc", "str A"),
                    Pair("cde", "str=s A"),
                    Pair("efg", "Any A"),
                    Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testInstanceParentIsPythonClass() {
        doFieldTest(
                listOf(
                        Pair("hij", "str B"),
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testInstanceUnResolve() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testString() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testAssignedString() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testAssignedInstance() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testAssignedInstanceWithImport() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                ),
                listOf("instance")
        )
    }

    fun testParameterAnnotation() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testParameterAnnotationType() {
        doFieldTest(
                listOf(
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testParameterAnnotationUnion() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testAssignedInstancePythonClass() {
        doFieldTest(
                listOf(
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A")
                )
        )
    }

    fun testParameterAnnotationPythonClass() {
        doFieldTest(
                listOf(
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A")
                )
        )
    }

    fun testParameterAnnotationUnionPythonClass() {
        doFieldTest(
                listOf(
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A")
                )
        )
    }

    fun testImportedInstance() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                ),
                listOf("instance")
        )
    }

    fun testImportedAssignedInstance() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                ),
                listOf("assignedInstance")
        )
    }

    fun testParameterDefaultValue() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testParameterNoType() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testUnResolveInstance() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testClass() {
        doFieldTest(
                listOf(
                        Pair("hij", "B"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testPythonClass() {
        doFieldTest(
                listOf(
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A")
                )
        )
    }

    fun testField() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str A"),
                        Pair("hij", "Any A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testFieldOptional() {
        doFieldTest(
                listOf(
                        Pair("abc", "Optional[str]=None A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testFieldUnion() {
        doFieldTest(
                listOf(
                        Pair("abc", "Union[str, int] A"),
                        Pair("cde", "Union[str, int] A"),
                        Pair("efg", "Union[str, int, None]=None A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testFieldSchema() {
        doFieldTest(
                listOf(
                        Pair("a_id","str A"),
                        Pair("abc", "str A"),
                        Pair("b_id","str A"),
                        Pair("c_id","str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("d_id", "str A"),
                        Pair("e_id", "str A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("f_id", "str A"),
                        Pair("g_id", "str=get_alias() A"),
                        Pair("hij", "Any A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testClassMethodCls() {
        doFieldTest(
                listOf(
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testMethodSelf() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }

    fun testFieldOverride() {
        doFieldTest(
                listOf(
                        Pair("a", "str B"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }


    fun testBaseSetting() {
        doFieldTest(
                listOf(
                        Pair("b", "str=... A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }
    fun testKeywordArgumentSchema() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("alias_a_id=", "str A"),
                        Pair("alias_b_id=", "str A"),
                        Pair("c_id=", "str A"),
                        Pair("cde=", "str=str('abc') A"),
                        Pair("d_id=", "str A"),
                        Pair("e_id=", "str A"),
                        Pair("efg=", "str=str('abc') A"),
                        Pair("f_id=", "str A"),
                        Pair("g_id=", "str=get_alias() A"),
                        Pair("hij=", "Any A"),
                        Pair("b_id", "null")
                )
        )
    }
}
