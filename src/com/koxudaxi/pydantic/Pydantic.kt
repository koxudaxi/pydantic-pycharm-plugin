package com.koxudaxi.pydantic

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.extensions.ModuleBasedContextAnchor
import com.jetbrains.extensions.QNameResolveContext
import com.jetbrains.extensions.resolveToElement
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.*
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.*
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.associatedModule
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules
import java.util.regex.Pattern

const val BASE_MODEL_Q_NAME = "pydantic.main.BaseModel"
const val DATA_CLASS_Q_NAME = "pydantic.dataclasses.dataclass"
const val DATA_CLASS_SHORT_Q_NAME = "pydantic.dataclass"
const val VALIDATOR_Q_NAME = "pydantic.class_validators.validator"
const val VALIDATOR_SHORT_Q_NAME = "pydantic.validator"
const val ROOT_VALIDATOR_Q_NAME = "pydantic.class_validators.root_validator"
const val ROOT_VALIDATOR_SHORT_Q_NAME = "pydantic.root_validator"
const val SCHEMA_Q_NAME = "pydantic.schema.Schema"
const val FIELD_Q_NAME = "pydantic.fields.Field"
const val DATACLASS_FIELD_Q_NAME = "dataclasses.field"
const val DEPRECATED_SCHEMA_Q_NAME = "pydantic.fields.Schema"
const val BASE_SETTINGS_Q_NAME = "pydantic.env_settings.BaseSettings"
const val VERSION_Q_NAME = "pydantic.version.VERSION"
const val BASE_CONFIG_Q_NAME = "pydantic.main.BaseConfig"
const val DATACLASS_MISSING = "dataclasses.MISSING"
const val CON_BYTES_Q_NAME = "pydantic.types.conbytes"
const val CON_DECIMAL_Q_NAME = "pydantic.types.condecimal"
const val CON_FLOAT_Q_NAME = "pydantic.types.confloat"
const val CON_INT_Q_NAME = "pydantic.types.conint"
const val CON_LIST_Q_NAME = "pydantic.types.conlist"
const val CON_STR_Q_NAME = "pydantic.types.constr"
const val LIST_Q_NAME = "builtins.list"
const val CREATE_MODEL = "pydantic.main.create_model"
const val ANY_Q_NAME = "typing.Any"
const val OPTIONAL_Q_NAME = "typing.Optional"
const val UNION_Q_NAME = "typing.Union"
const val ANNOTATED_Q_NAME = "typing.Annotated"
const val CLASSVAR_Q_NAME = "typing.ClassVar"


val VERSION_QUALIFIED_NAME = QualifiedName.fromDottedString(VERSION_Q_NAME)

val BASE_CONFIG_QUALIFIED_NAME = QualifiedName.fromDottedString(BASE_CONFIG_Q_NAME)

val BASE_MODEL_QUALIFIED_NAME = QualifiedName.fromDottedString(BASE_MODEL_Q_NAME)

val VALIDATOR_QUALIFIED_NAME = QualifiedName.fromDottedString(VALIDATOR_Q_NAME)

val VALIDATOR_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(VALIDATOR_SHORT_Q_NAME)

val ROOT_VALIDATOR_QUALIFIED_NAME = QualifiedName.fromDottedString(ROOT_VALIDATOR_Q_NAME)

val ROOT_VALIDATOR_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(ROOT_VALIDATOR_SHORT_Q_NAME)

val DATA_CLASS_QUALIFIED_NAME = QualifiedName.fromDottedString(DATA_CLASS_Q_NAME)

val DATA_CLASS_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(DATA_CLASS_SHORT_Q_NAME)

val DATA_CLASS_QUALIFIED_NAMES = listOf(
        DATA_CLASS_QUALIFIED_NAME,
        DATA_CLASS_SHORT_QUALIFIED_NAME
)

val VALIDATOR_QUALIFIED_NAMES = listOf(
        VALIDATOR_QUALIFIED_NAME,
        VALIDATOR_SHORT_QUALIFIED_NAME,
        ROOT_VALIDATOR_QUALIFIED_NAME,
        ROOT_VALIDATOR_SHORT_QUALIFIED_NAME
)

val VERSION_SPLIT_PATTERN: Pattern = Pattern.compile("[.a-zA-Z]")!!

val pydanticVersionCache: HashMap<String, KotlinVersion> = hashMapOf()

enum class ConfigType {
    BOOLEAN, LIST_PYTYPE
}

val DEFAULT_CONFIG = mapOf<String, Any?>(
        "allow_population_by_alias" to false,
        "allow_population_by_field_name" to false,
        "orm_mode" to false,
        "allow_mutation" to true,
        "keep_untouched" to listOf<PyType>()
)

val CONFIG_TYPES = mapOf(
        "allow_population_by_alias" to ConfigType.BOOLEAN,
        "allow_population_by_field_name" to ConfigType.BOOLEAN,
        "orm_mode" to ConfigType.BOOLEAN,
        "allow_mutation" to ConfigType.BOOLEAN,
        "keep_untouched" to ConfigType.LIST_PYTYPE
)

const val CUSTOM_ROOT_FIELD = "__root__"

fun getPyClassByPyCallExpression(pyCallExpression: PyCallExpression, includeDataclass: Boolean, context: TypeEvalContext): PyClass? {
    val callee = pyCallExpression.callee ?: return null
    val pyType = when (val type = context.getType(callee)) {
        is PyClass -> return type
        is PyClassType -> type
        else -> (callee.reference?.resolve() as? PyTypedElement)?.let { context.getType(it) } ?: return null
    }
    return getPyClassTypeByPyTypes(pyType).firstOrNull { isPydanticModel(it.pyClass, includeDataclass) }?.pyClass
}

fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument, context: TypeEvalContext): PyClass? {
    val pyCallExpression = PsiTreeUtil.getParentOfType(pyKeywordArgument, PyCallExpression::class.java) ?: return null
    return getPyClassByPyCallExpression(pyCallExpression, true, context)
}

fun isPydanticModel(pyClass: PyClass, includeDataclass: Boolean, context: TypeEvalContext? = null): Boolean {
    return (isSubClassOfPydanticBaseModel(pyClass, context) || (includeDataclass && isPydanticDataclass(pyClass))) && !isPydanticBaseModel(pyClass)
}

fun isPydanticBaseModel(pyClass: PyClass): Boolean {
    return pyClass.qualifiedName == BASE_MODEL_Q_NAME
}

internal fun isSubClassOfPydanticBaseModel(pyClass: PyClass, context: TypeEvalContext?): Boolean {
    return pyClass.isSubclass(BASE_MODEL_Q_NAME, context)
}

internal fun isBaseSetting(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(BASE_SETTINGS_Q_NAME, context)
}

internal fun hasDecorator(pyDecoratable: PyDecoratable, refNames: List<QualifiedName>): Boolean {
    return pyDecoratable.decoratorList?.decorators?.mapNotNull { it.callee as? PyReferenceExpression }?.any {
        PyResolveUtil.resolveImportedElementQNameLocally(it).any { decoratorQualifiedName ->
            refNames.any { refName -> decoratorQualifiedName == refName }
        }
    } ?: false
}

internal fun isPydanticDataclass(pyClass: PyClass): Boolean {
    return hasDecorator(pyClass, DATA_CLASS_QUALIFIED_NAMES)
}

internal fun isPydanticSchema(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(SCHEMA_Q_NAME, context)
}

internal fun isPydanticField(pyFunction: PyFunction): Boolean {
    return pyFunction.qualifiedName == FIELD_Q_NAME || pyFunction.qualifiedName == DEPRECATED_SCHEMA_Q_NAME
}

internal fun isDataclassField(pyFunction: PyFunction): Boolean {
    return pyFunction.qualifiedName == DATACLASS_FIELD_Q_NAME
}


internal fun isPydanticCreateModel(pyFunction: PyFunction): Boolean {
    return pyFunction.qualifiedName == CREATE_MODEL
}

internal fun isDataclassMissing(pyTargetExpression: PyTargetExpression): Boolean {
    return pyTargetExpression.qualifiedName == DATACLASS_MISSING
}

internal fun isValidatorMethod(pyFunction: PyFunction): Boolean {
    return hasDecorator(pyFunction, VALIDATOR_QUALIFIED_NAMES)
}

internal fun isConfigClass(pyClass: PyClass): Boolean {
    return pyClass.name == "Config"
}

internal fun getClassVariables(pyClass: PyClass, context: TypeEvalContext): Sequence<PyTargetExpression> {
    return pyClass.classAttributes
            .asReversed()
            .asSequence()
            .filterNot { PyTypingTypeProvider.isClassVar(it, context) }
}

private fun getAliasedFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?): String? {
    val fieldName = field.name
    val assignedField = field.findAssignedValue()?.let {
        getFieldFromPyExpression(it, context, pydanticVersion)
    } ?:
        (field.annotation?.value as? PySubscriptionExpression)
            ?.takeIf { getQualifiedName(it, context) == ANNOTATED_Q_NAME }
            ?.let { getFieldFromAnnotated(it, context) }
    ?: return fieldName

    return when (val alias = assignedField.getKeywordArgument("alias")) {
                is StringLiteralExpression -> alias.stringValue
                is PyReferenceExpression -> ((alias.reference.resolve() as? PyTargetExpressionImpl)
                        ?.findAssignedValue() as? StringLiteralExpression)?.stringValue
                //TODO Support dynamic assigned Value. eg:  Schema(..., alias=get_alias_name(field_name))
                else -> fieldName
            } ?: fieldName
    }


fun getResolveElements(referenceExpression: PyReferenceExpression, context: TypeEvalContext): Array<ResolveResult> {
    return PyResolveContext.defaultContext().withTypeEvalContext(context).let {
        referenceExpression.getReference(it).multiResolve(false)
    }

}

fun getPyClassTypeByPyTypes(pyType: PyType): List<PyClassType> {
    return when (pyType) {
        is PyUnionType -> pyType.members.mapNotNull { it }.flatMap { getPyClassTypeByPyTypes(it) }
        is PyClassType -> listOf(pyType)
        else -> listOf()
    }
}


fun isPydanticSchemaByPsiElement(psiElement: PsiElement, context: TypeEvalContext): Boolean {
    return PsiTreeUtil.getContextOfType(psiElement, PyClass::class.java)
            ?.let { isPydanticSchema(it, context) } ?: false
}

inline fun <reified T : PsiElement> validatePsiElementByFunction(psiElement: PsiElement, validator: (T) -> Boolean): Boolean {
    return when {
        T::class.java.isInstance(psiElement) -> validator(psiElement as T)
        else -> PsiTreeUtil.getContextOfType(psiElement, T::class.java)
                ?.let { validator(it) } ?: false
    }
}

fun isPydanticFieldByPsiElement(psiElement: PsiElement): Boolean {
    return validatePsiElementByFunction(psiElement, ::isPydanticField)
}

fun isDataclassFieldByPsiElement(psiElement: PsiElement): Boolean {
    return validatePsiElementByFunction(psiElement, ::isDataclassField)
}

fun isDataclassMissingByPsiElement(psiElement: PsiElement): Boolean {
    return validatePsiElementByFunction(psiElement, ::isDataclassMissing)
}

fun getSdk(project: Project): Sdk? {
    return project.pythonSdk ?: project.modules.mapNotNull { PythonSdkUtil.findPythonSdk(it) }.firstOrNull()
}

fun getPsiElementByQualifiedName(qualifiedName: QualifiedName, project: Project, context: TypeEvalContext): PsiElement? {
    val pythonSdk = getSdk(project) ?: return null
    val module = pythonSdk.associatedModule ?: project.modules.firstOrNull() ?: return null
    val contextAnchor = ModuleBasedContextAnchor(module)
    return qualifiedName.resolveToElement(QNameResolveContext(contextAnchor, pythonSdk, context))
}

fun getPydanticVersion(project: Project, context: TypeEvalContext): KotlinVersion? {
    val version = getPsiElementByQualifiedName(VERSION_QUALIFIED_NAME, project, context) as? PyTargetExpression
            ?: return null
    val versionString = (version.findAssignedValue()?.lastChild?.firstChild?.nextSibling as? PyStringLiteralExpression)?.stringValue
            ?: return null
    return pydanticVersionCache.getOrPut(versionString, {
        val versionList = versionString.split(VERSION_SPLIT_PATTERN).map { it.toIntOrNull() ?: 0 }
        val pydanticVersion = when {
            versionList.size == 1 -> KotlinVersion(versionList[0], 0)
            versionList.size == 2 -> KotlinVersion(versionList[0], versionList[1])
            versionList.size >= 3 -> KotlinVersion(versionList[0], versionList[1], versionList[2])
            else -> null
        } ?: KotlinVersion(0, 0)
        pydanticVersionCache[versionString] = pydanticVersion
        pydanticVersion
    })
}

fun isValidField(field: PyTargetExpression, context: TypeEvalContext): Boolean {
    if (!isValidFieldName(field.name)) return false

    val annotationValue = field.annotation?.value ?: return true
    // TODO Support a variable.
    return getQualifiedName(annotationValue, context) != CLASSVAR_Q_NAME
}

fun isValidFieldName(name: String?): Boolean {
    return name?.let { !it.startsWith('_') || it == CUSTOM_ROOT_FIELD } ?: false
}

fun getConfigValue(name: String, value: Any?, context: TypeEvalContext): Any? {
    if (value is PyReferenceExpression) {
        val resolveResults = getResolveElements(value, context)
        val targetExpression = PyUtil.filterTopPriorityResults(resolveResults).firstOrNull() ?: return null
        val assignedValue = (targetExpression as? PyTargetExpression)?.findAssignedValue() ?: return null
        return getConfigValue(name, assignedValue, context)
    }
    return when (CONFIG_TYPES[name]) {
        ConfigType.BOOLEAN ->
            when (value) {
                is PyBoolLiteralExpression -> value.value
                is Boolean -> value
                else -> null
            }
        ConfigType.LIST_PYTYPE -> {
            if (value is PyElement) {
                when (val tupleValue = PsiTreeUtil.findChildOfType(value, PyTupleExpression::class.java)) {
                    is PyTupleExpression -> tupleValue.toList().mapNotNull { getPyTypeFromPyExpression(it, context) }
                    else -> null
                }
            } else null
        }
        else -> null
    }
}

fun getConfig(pyClass: PyClass, context: TypeEvalContext, setDefault: Boolean): HashMap<String, Any?> {
    val config = hashMapOf<String, Any?>()
    pyClass.getAncestorClasses(context)
            .reversed()
            .filter { isPydanticModel(it, false) }
            .map { getConfig(it, context, false) }
            .forEach {
                it.entries.forEach { entry ->
                    if (entry.value != null) {
                        config[entry.key] = getConfigValue(entry.key, entry.value, context)
                    }
                }
            }
    pyClass.nestedClasses.firstOrNull { isConfigClass(it) }?.let {
        it.classAttributes.forEach { attribute ->
            attribute.findAssignedValue()?.let { value ->
                attribute.name?.let { name ->
                    config[name] = getConfigValue(name, value, context)
                }
            }
        }
    }

    if (setDefault) {
        DEFAULT_CONFIG.forEach { (key, value) ->
            if (!config.containsKey(key)) {
                config[key] = getConfigValue(key, value, context)
            }
        }
    }
    return config
}

fun getFieldName(field: PyTargetExpression,
                 context: TypeEvalContext,
                 config: HashMap<String, Any?>,
                 pydanticVersion: KotlinVersion?): String? {

    return when (pydanticVersion?.major) {
        0 -> when {
            config["allow_population_by_alias"] == true -> field.name
            else -> getAliasedFieldName(field, context, pydanticVersion)
        }
        else -> when {
            config["allow_population_by_field_name"] == true -> field.name
            else -> getAliasedFieldName(field, context, pydanticVersion)
        }
    }
}


fun getPydanticBaseConfig(project: Project, context: TypeEvalContext): PyClass? {
    return getPyClassFromQualifiedName(BASE_CONFIG_QUALIFIED_NAME, project, context)
}

fun getPydanticBaseModel(project: Project, context: TypeEvalContext): PyClass? {
    return getPyClassFromQualifiedName(BASE_MODEL_QUALIFIED_NAME, project, context)
}

fun getPyClassFromQualifiedName(qualifiedName: QualifiedName, project: Project, context: TypeEvalContext): PyClass? {
    val module = project.modules.firstOrNull() ?: return null
    val pythonSdk = module.pythonSdk
    val contextAnchor = ModuleBasedContextAnchor(module)
    return qualifiedName.resolveToElement(QNameResolveContext(contextAnchor, pythonSdk, context)) as? PyClass
}

fun getPyClassByAttribute(pyPsiElement: PsiElement?): PyClass? {
    return pyPsiElement?.parent?.parent as? PyClass
}

fun createPyClassTypeImpl(qualifiedName: String, project: Project, context: TypeEvalContext): PyClassTypeImpl? {
    var psiElement = getPsiElementByQualifiedName(QualifiedName.fromDottedString(qualifiedName), project, context)
    if (psiElement == null) {
        psiElement = getPsiElementByQualifiedName(QualifiedName.fromDottedString("builtins.$qualifiedName"), project, context)
                ?: return null
    }
    return PyClassTypeImpl.createTypeByQName(psiElement, qualifiedName, false)
}

fun getPydanticPyClass(pyCallExpression: PyCallExpression, context: TypeEvalContext): PyClass? {
    val pyClass = getPyClassByPyCallExpression(pyCallExpression, false, context) ?: return null
    if (!isPydanticModel(pyClass, false, context)) return null
    return pyClass
}

fun getParentOfPydanticCallableExpression(file: PsiFile, offset: Int, context: TypeEvalContext): PyCallExpression? {
    var pyCallExpression: PyCallExpression? = PsiTreeUtil.getParentOfType(file.findElementAt(offset), PyCallExpression::class.java, true)
    while (pyCallExpression != null && getPydanticPyClass(pyCallExpression, context) == null) {
        pyCallExpression = PsiTreeUtil.getParentOfType(pyCallExpression, PyCallExpression::class.java, true)
    }
    return pyCallExpression
}

fun getPydanticCallExpressionAtCaret(file: PsiFile, editor: Editor, context: TypeEvalContext): PyCallExpression? {
    return getParentOfPydanticCallableExpression(file, editor.caretModel.offset, context)
            ?: getParentOfPydanticCallableExpression(file, editor.caretModel.offset - 1, context)
}


fun addKeywordArgument(pyCallExpression: PyCallExpression, pyKeywordArgument: PyKeywordArgument) {
    when (val lastArgument = pyCallExpression.arguments.lastOrNull()) {
        null -> pyCallExpression.argumentList?.addArgument(pyKeywordArgument)
        else -> pyCallExpression.argumentList?.addArgumentAfter(pyKeywordArgument, lastArgument)
    }
}

fun getPydanticUnFilledArguments(pyClass: PyClass?, pyCallExpression: PyCallExpression, pydanticTypeProvider: PydanticTypeProvider, context: TypeEvalContext): List<PyCallableParameter> {
    val pydanticClass = pyClass ?: getPydanticPyClass(pyCallExpression, context) ?: return emptyList()
    val pydanticType = pydanticTypeProvider.getPydanticTypeForClass(pydanticClass, context, true) ?: return emptyList()
    val currentArguments = pyCallExpression.arguments.filter { it is PyKeywordArgument || (it as? PyStarArgumentImpl)?.isKeyword == true }
            .mapNotNull { it.name }.toSet()
    return pydanticType.getParameters(context)?.filterNot { currentArguments.contains(it.name) } ?: emptyList()
}

val PyCallableParameter.required: Boolean
    get() = !hasDefaultValue() || (defaultValue !is PyNoneLiteralExpression && defaultValueText == "...")


fun getPyTypeFromPyExpression(pyExpression: PyExpression, context: TypeEvalContext): PyType? {
    return when (pyExpression) {
        is PyType -> pyExpression
        is PyReferenceExpression -> {
            val resolveResults = getResolveElements(pyExpression, context)
            PyUtil.filterTopPriorityResults(resolveResults)
                    .filterIsInstance<PyClass>()
                    .map { pyClass -> pyClass.getType(context)?.getReturnType(context) }
                    .firstOrNull()
        }
        else -> null
    }
}

internal fun hasTargetPyType(pyExpression: PyExpression, targetPyTypes: List<PyType>, context: TypeEvalContext): Boolean {
    val callee = (pyExpression as? PyCallExpression)?.callee ?: return false
    val pyType = getPyTypeFromPyExpression(callee, context) ?: return false
    val defaultValueTypeClassQName = pyType.declarationElement?.qualifiedName ?: return false
    return targetPyTypes.any { it.declarationElement?.qualifiedName == defaultValueTypeClassQName }
}

internal fun isUntouchedClass(pyExpression: PyExpression?, config: HashMap<String, Any?>, context: TypeEvalContext):Boolean {
    if (pyExpression == null) return false
    val keepUntouchedClasses = (config["keep_untouched"] as? List<*>)?.filterIsInstance<PyType>()?.toList() ?: return false
    if (keepUntouchedClasses.isNullOrEmpty()) return false
    return (hasTargetPyType(pyExpression, keepUntouchedClasses, context))
}

internal fun getFieldFromPyExpression(psiElement: PsiElement, context: TypeEvalContext, pydanticVersion: KotlinVersion?): PyCallExpression? {
    val callee = (psiElement as? PyCallExpression)
        ?.let { it.callee as? PyReferenceExpression }
        ?: return null
    val results = getResolveElements(callee, context)
    val versionZero = pydanticVersion?.major == 0
    if (!PyUtil.filterTopPriorityResults(results).any{
            when {
                versionZero -> isPydanticSchemaByPsiElement(it, context)
                else -> isPydanticFieldByPsiElement(it)
            }
    }) return null
    return psiElement
}

internal fun getFieldFromAnnotated(annotated: PyExpression, context: TypeEvalContext): PyCallExpression? =
    annotated.children
        .filterIsInstance <PyTupleExpression>()
        .firstOrNull()
        ?.children
        ?.getOrNull(1)
        ?.let {getFieldFromPyExpression(it, context, null)
        }

internal fun getTypeExpressionFromAnnotated(annotated: PyExpression, context: TypeEvalContext): PyExpression? =
    annotated.children
        .filterIsInstance <PyTupleExpression>()
        .firstOrNull()
        ?.children
        ?.getOrNull(0)
        ?.let { it as? PyExpression }

internal fun getDefaultFromField(field: PyCallExpression): PyExpression? = field.getKeywordArgument("default")
    ?: field.getArgument(0, PyExpression::class.java).takeIf { it?.name == null }

internal fun getDefaultFactoryFromField(field: PyCallExpression): PyExpression? = field.getKeywordArgument("default_factory")

internal fun getQualifiedName(pyExpression: PyExpression, context: TypeEvalContext) : String? {
    return when(pyExpression) {
    is PySubscriptionExpression -> pyExpression.qualifier?.let { getQualifiedName(it, context) }
    is PyReferenceExpression -> {
        val resolveResults = getResolveElements(pyExpression, context)
        return PyUtil.filterTopPriorityResults(resolveResults)
            .filterIsInstance<PyQualifiedNameOwner>()
            .mapNotNull { it.qualifiedName }
            .firstOrNull()
    }
    else -> {
        return null
        }
    }
}