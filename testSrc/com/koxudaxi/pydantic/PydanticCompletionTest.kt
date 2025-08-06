package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.jetbrains.python.psi.PyTargetExpression


open class PydanticCompletionTest : PydanticTestCase() {


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
            "__type_params__",
            "Ellipsis",
            "EnvironmentError",
            "IOError",
            "NotImplemented",
            "List",
            "Type",
            "Annotated",
            "MISSING",
            "WindowsError")
        val completions = myFixture!!.completeBasic() ?: emptyArray()
        val actual = completions.filter {
            it!!.psiElement is PyTargetExpression
        }.filterNot {
            excludes.contains(it!!.lookupString)
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

    fun testKeywordArgumentCustomRoot() {
        doFieldTest(
            listOf(
                Pair("__root__=", "str A")
            )
        )
    }

//    fun testKeywordArgumentDot() {
//        doFieldTest(
//            listOf(Pair("___slots__", "BaseModel"))
//        )
//    }

//    fun testKeywordArgumentDotName() {
//        doFieldTest(
//            emptyList()
//        )
//    }

    fun testKeywordArgumentIgnore() {
        doFieldTest(
            listOf(
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
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
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
                Pair("efg", "A"),
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
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testAssignedClass() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("a", "null"),
                Pair("cde=", "str='abc' A"),
                Pair("efg=", "str='abc' A")
            )
        )
    }

    fun testAssignedInstanceWithImport() {
        doFieldTest(
            listOf(
                Pair("abc", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
                Pair("___slots__", "BaseModel")
            ),
            listOf("instance")
        )
    }

    fun testParameterAnnotation() {
        doFieldTest(
            listOf(
                Pair("abc", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

//    fun testParameterAnnotationType() {
//        doFieldTest(
//            listOf(
//                Pair("___slots__", "BaseModel")
//            )
//        )
//    }

    fun testParameterAnnotationTypeKeywordArgument() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("cde=", "str='abc' A"),
                Pair("efg=", "str='abc' A")
            )
        )
    }

    fun testParameterAnnotationUnion() {
        doFieldTest(
            listOf(
                Pair("abc", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
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
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
                Pair("___slots__", "BaseModel")
            ),
            listOf("instance")
        )
    }

    fun testImportedAssignedInstance() {
        doFieldTest(
            listOf(
                Pair("abc", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
                Pair("___slots__", "BaseModel")
            ),
            listOf("assignedInstance")
        )
    }

    fun testParameterDefaultValue() {
        doFieldTest(
            listOf(
                Pair("abc", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
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

//    fun testClass() {
//        doFieldTest(
//            listOf(
//                Pair("hij", "B"),
//                Pair("___slots__", "BaseModel")
//            )
//        )
//    }

    fun testConfig() {
        doFieldTest(
            listOf(
                Pair("alias_generator = None", "BaseConfig"),
                Pair("allow_mutation = True", "BaseConfig"),
                Pair("allow_population_by_field_name = False", "BaseConfig"),
                Pair("anystr_strip_whitespace = False", "BaseConfig"),
                Pair("arbitrary_types_allowed = False", "BaseConfig"),
                Pair("error_msg_templates = {}", "BaseConfig"),
                Pair("extra = Extra.ignore", "BaseConfig"),
                Pair("fields = {}", "BaseConfig"),
                Pair("getter_dict = GetterDict", "BaseConfig"),
                Pair("json_dumps = json.dumps", "BaseConfig"),
                Pair("json_encoders = {}", "BaseConfig"),
                Pair("json_loads = json.loads", "BaseConfig"),
                Pair("keep_untouched = ()", "BaseConfig"),
                Pair("max_anystr_length = None", "BaseConfig"),
                Pair("min_anystr_length = None", "BaseConfig"),
                Pair("orm_mode = False", "BaseConfig"),
                Pair("schema_extra = {}", "BaseConfig"),
                Pair("title = None", "BaseConfig"),
                Pair("use_enum_values = False", "BaseConfig"),
                Pair("validate_all = False", "BaseConfig"),
                Pair("validate_assignment = False", "BaseConfig")
            )
        )
    }

    fun testConfigDefined() {
        doFieldTest(
            listOf(
                Pair("alias_generator = None", "BaseConfig"),
                Pair("allow_mutation = True", "BaseConfig"),
                Pair("anystr_strip_whitespace = False", "BaseConfig"),
                Pair("arbitrary_types_allowed = False", "BaseConfig"),
                Pair("error_msg_templates = {}", "BaseConfig"),
                Pair("extra = Extra.ignore", "BaseConfig"),
                Pair("fields = {}", "BaseConfig"),
                Pair("getter_dict = GetterDict", "BaseConfig"),
                Pair("json_dumps = json.dumps", "BaseConfig"),
                Pair("json_encoders = {}", "BaseConfig"),
                Pair("json_loads = json.loads", "BaseConfig"),
                Pair("keep_untouched = ()", "BaseConfig"),
                Pair("min_anystr_length = None", "BaseConfig"),
                Pair("orm_mode = False", "BaseConfig"),
                Pair("schema_extra = {}", "BaseConfig"),
                Pair("title = None", "BaseConfig"),
                Pair("use_enum_values = False", "BaseConfig"),
                Pair("validate_all = False", "BaseConfig"),
                Pair("validate_assignment = False", "BaseConfig")
            )
        )
    }

//    fun testNestedClass() {
//        doFieldTest(
//                listOf(
//                        Pair("class Config", "null")
//                )
//        )
//    }

    fun testDefinedNestedClass() {
        doFieldTest(
            listOf(
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


//    fun testClassFields() {
//        doFieldTest(
//            listOf(
//                Pair("___slots__", "BaseModel")
//            )
//        )
//    }

    fun testField() {
        doFieldTest(
            listOf(
                Pair("abc", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("efg", "str A"),
                Pair("hij", "Any A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testFieldIgnore() {
        doFieldTest(
            listOf(
                Pair("descriptor1", "A"),
                Pair("efg", "A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testFieldCustomRoot() {
        doFieldTest(
            listOf(
                Pair("__root__", "str A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testFieldOptional() {
        doFieldTest(
            listOf(
                Pair("abc", "Union[str, NoneType]=... A"),
                Pair("cde", "str='abc' A"),
                Pair("efg", "str='abc' A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testFieldUnion() {
        doFieldTest(
            listOf(
                Pair("abc", "Union[str, int] A"),
                Pair("cde", "Union[str, int] A"),
                Pair("efg", "Union[Union[str, int], Any] A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testFieldSchema() {
        doFieldTest(
            listOf(
                Pair("a_id", "str A"),
                Pair("abc", "str A"),
                Pair("b_id", "str A"),
                Pair("c_id", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("d_id", "str A"),
                Pair("e_id", "str A"),
                Pair("efg", "str='abc' A"),
                Pair("f_id", "str A"),
                Pair("g_id", "str=get_alias() A"),
                Pair("hij", "Any A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testFieldSchemaField() {
        doFieldTest(
            listOf(
                Pair("a_id", "str A"),
                Pair("abc", "str A"),
                Pair("b_id", "str A"),
                Pair("c_id", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("d_id", "str A"),
                Pair("e_id", "str A"),
                Pair("efg", "str='abc' A"),
                Pair("f_id", "str A"),
                Pair("g_id", "str=get_alias() A"),
                Pair("hij", "Any A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testFieldField() {
        doFieldTest(
            listOf(
                Pair("a_id", "str A"),
                Pair("abc", "str A"),
                Pair("b_id", "str A"),
                Pair("c_id", "str A"),
                Pair("cde", "str='abc' A"),
                Pair("d_id", "str A"),
                Pair("e_id", "str A"),
                Pair("efg", "str='abc' A"),
                Pair("f_id", "str A"),
                Pair("g_id", "str=get_alias() A"),
                Pair("hij", "Any A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

//    fun testClassMethodCls() {
//        doFieldTest(
//            listOf(
//                Pair("___slots__", "BaseModel")
//            )
//        )
//    }

//    fun testClassValidatorCls() {
//        doFieldTest(
//            listOf(
//                Pair("___slots__", "BaseModel")
//            )
//        )
//    }

    fun testClassInitMethod() {
        doFieldTest(
            listOf(
                Pair("abc", "str A"),
                Pair("efg", "str C"),
                Pair("opq", "B"),
                Pair("xyz", "B"),
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

    fun testKeywordArgumentSchemaField() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("alias_a_id=", "str A"),
                Pair("alias_b_id=", "str A"),
                Pair("c_id=", "str A"),
                Pair("cde=", "str='abc' A"),
                Pair("d_id=", "str A"),
                Pair("e_id=", "str A"),
                Pair("efg=", "str='abc' A"),
                Pair("f_id=", "str A"),
                Pair("g_id=", "str=get_alias() A"),
                Pair("hij=", "Any A"),
                Pair("b_id", "null")
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
                Pair("cde=", "str='abc' A"),
                Pair("d_id=", "str A"),
                Pair("e_id=", "str A"),
                Pair("efg=", "str='abc' A"),
                Pair("f_id=", "str A"),
                Pair("g_id=", "str=get_alias() A"),
                Pair("hij=", "Any A"),
                Pair("b_id", "null")
            )
        )
    }

    // TODO: Subscription class completion broken in PyCharm 2025.2
    fun _disabled_testSubscriptionClass() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("cde=", "str='abc' A"),
                Pair("efg=", "str='abc' A")
            )
        )
    }

    fun testkeywordArgumentAllowPopulationByFieldName() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("cde=", "str A")
            )
        )
    }

    fun testkeywordArgumentAllowPopulationByFieldNameChild() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("cde=", "str A")
            )
        )
    }

    fun testkeywordArgumentAllowPopulationByFieldNameParent() {
        doFieldTest(
            listOf(
                Pair("abc=", "str B"),
                Pair("cde=", "str B")
            )
        )
    }

    fun testkeywordArgumentAllowPopulationByFieldNameMultipleInheritance() {
        doFieldTest(
            listOf(
                Pair("abc=", "str C"),
                Pair("cde=", "str C")
            )
        )
    }

    fun testkeywordArgumentAllowPopulationByFieldNameFalse() {
        doFieldTest(
            listOf(
                Pair("ABC=", "str A"),
                Pair("CDE=", "str A")
            )
        )
    }

    fun testKeywordArgumentInitArgsKwargs() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("cde=", "str A"),
                        Pair("efg=", "str A")
                )
        )
    }
    // TODO: Init args/kwargs completion broken in PyCharm 2025.2
    fun _disabled_testKeywordArgumentInitArgsKwargsDisable() {
        val config = PydanticConfigService.getInstance(myFixture!!.project)
        config.ignoreInitMethodKeywordArguments = false
        try {
            doFieldTest(
                emptyList(
                )
        )}
        finally {
            config.ignoreInitMethodKeywordArguments = true
        }
    }
    fun testKeywordArgumentInitKwargs() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("cde=", "str A"),
                        Pair("efg=", "str A")
                )
        )
    }

    // TODO: Init position argument completion broken in PyCharm 2025.2
    fun _disabled_testKeywordArgumentInitPosition() {
        doFieldTest(
            emptyList()
        )
    }

    fun testdataclassKeywordArgument() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("cda=", "str A"),
                Pair("cde=", "str A"),
                Pair("edc=", "str=dummy() A"),
                Pair("efg=", "str='xyz' A"),
                Pair("gef=", "str=unresolved A"),
                Pair("hij=", "str=lambda :'asd' A"),
                Pair("jih=", "str A"),
                Pair("klm=", "str='qwe' A"),
                Pair("mlk=", "str A"),
                Pair("qrs=", "str='fgh' A"),
                Pair("tuw=", "str A"),
                Pair("xyz=", "str A")
            )
        )
    }

    fun testconfig() {
        doFieldTest(
            listOf(
                Pair("alias_generator = None", "BaseConfig"),
                Pair("allow_mutation = True", "BaseConfig"),
                Pair("allow_population_by_field_name = False", "BaseConfig"),
                Pair("anystr_strip_whitespace = False", "BaseConfig"),
                Pair("arbitrary_types_allowed = False", "BaseConfig"),
                Pair("error_msg_templates = {}", "BaseConfig"),
                Pair("extra = Extra.ignore", "BaseConfig"),
                Pair("fields = {}", "BaseConfig"),
                Pair("getter_dict = GetterDict", "BaseConfig"),
                Pair("json_dumps = json.dumps", "BaseConfig"),
                Pair("json_encoders = {}", "BaseConfig"),
                Pair("json_loads = json.loads", "BaseConfig"),
                Pair("keep_untouched = ()", "BaseConfig"),
                Pair("max_anystr_length = None", "BaseConfig"),
                Pair("min_anystr_length = None", "BaseConfig"),
                Pair("orm_mode = False", "BaseConfig"),
                Pair("schema_extra = {}", "BaseConfig"),
                Pair("title = None", "BaseConfig"),
                Pair("use_enum_values = False", "BaseConfig"),
                Pair("validate_all = False", "BaseConfig"),
                Pair("validate_assignment = False", "BaseConfig")
            )
        )
    }

    fun testconfigDefined() {
        doFieldTest(
            listOf(
                Pair("alias_generator = None", "BaseConfig"),
                Pair("allow_mutation = True", "BaseConfig"),
                Pair("anystr_strip_whitespace = False", "BaseConfig"),
                Pair("arbitrary_types_allowed = False", "BaseConfig"),
                Pair("error_msg_templates = {}", "BaseConfig"),
                Pair("extra = Extra.ignore", "BaseConfig"),
                Pair("fields = {}", "BaseConfig"),
                Pair("getter_dict = GetterDict", "BaseConfig"),
                Pair("json_dumps = json.dumps", "BaseConfig"),
                Pair("json_encoders = {}", "BaseConfig"),
                Pair("json_loads = json.loads", "BaseConfig"),
                Pair("keep_untouched = ()", "BaseConfig"),
                Pair("min_anystr_length = None", "BaseConfig"),
                Pair("orm_mode = False", "BaseConfig"),
                Pair("schema_extra = {}", "BaseConfig"),
                Pair("title = None", "BaseConfig"),
                Pair("use_enum_values = False", "BaseConfig"),
                Pair("validate_all = False", "BaseConfig"),
                Pair("validate_assignment = False", "BaseConfig")
            )
        )
    }

//    fun testConlist() {
//        doFieldTest(
//            listOf(
//                Pair("abc=", "list A"),
//                Pair("cde=", "List[str] A"),
//                Pair("efg=", "List[str] A"),
//                Pair("hij=", "list A")
//            )
//        )
//    }

    fun testFieldAnnotated() {
        doFieldTest(
            listOf(
                Pair("a_id", "str A"),
                Pair("abc", "str A"),
                Pair("cde", "str='default_value' A"),
                Pair("efg", "str=lambda: 123 A"),
                Pair("klm", "str=lambda: 456 A"),
                Pair("nop", "str=lambda: 789 A"),
                Pair("___slots__", "BaseModel")
            )
        )
    }

    fun testKeywordArgumentFieldAnnotated() {
        doFieldTest(
            listOf(
                Pair("abc=", "str A"),
                Pair("alias_a_id=", "str A"),
                Pair("cde=", "str='default_value' A"),
                Pair("efg=", "str=lambda: 123 A")
            )
        )
    }
}
