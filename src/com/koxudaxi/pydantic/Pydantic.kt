package com.koxudaxi.pydantic

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.extensions.ModuleBasedContextAnchor
import com.jetbrains.extensions.QNameResolveContext
import com.jetbrains.extensions.resolveToElement
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyStarArgumentImpl
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.*
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.isAssociatedWithModule
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules
import java.util.regex.Pattern

const val BASE_MODEL_Q_NAME = "pydantic.main.BaseModel"
const val GENERIC_MODEL_Q_NAME = "pydantic.generics.GenericModel"
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
const val GENERIC_Q_NAME = "typing.Generic"
const val TYPE_Q_NAME = "typing.Type"
const val TUPLE_Q_NAME = "typing.Tuple"

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
    BOOLEAN, LIST_PYTYPE, EXTRA
}

enum class EXTRA {
    ALLOW, IGNORE, FORBID
}
val DEFAULT_CONFIG = mapOf<String, Any?>(
    "allow_population_by_alias" to false,
    "allow_population_by_field_name" to false,
    "orm_mode" to false,
    "allow_mutation" to true,
    "frozen" to false,
    "keep_untouched" to listOf<PyType>(),
    "extra" to EXTRA.IGNORE
)

val CONFIG_TYPES = mapOf(
    "allow_population_by_alias" to ConfigType.BOOLEAN,
    "allow_population_by_field_name" to ConfigType.BOOLEAN,
    "orm_mode" to ConfigType.BOOLEAN,
    "allow_mutation" to ConfigType.BOOLEAN,
    "frozen" to ConfigType.BOOLEAN,
    "keep_untouched" to ConfigType.LIST_PYTYPE,
    "extra" to ConfigType.EXTRA
)

const val CUSTOM_ROOT_FIELD = "__root__"

fun PyTypedElement.getType(context: TypeEvalContext): PyType? = context.getType(this)

fun getPyClassByPyCallExpression(
    pyCallExpression: PyCallExpression,
    includeDataclass: Boolean,
    context: TypeEvalContext,
): PyClass? {
    val callee = pyCallExpression.callee ?: return null
    val pyType = when (val type = callee.getType(context)) {
        is PyClass -> return type
        is PyClassType -> type
        else -> (callee.reference?.resolve() as? PyTypedElement)?.getType(context) ?: return null
    }
    return pyType.pyClassTypes.firstOrNull {
        isPydanticModel(it.pyClass,
            includeDataclass,
            context)
    }?.pyClass
}

fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument, context: TypeEvalContext): PyClass? {
    val pyCallExpression = PsiTreeUtil.getParentOfType(pyKeywordArgument, PyCallExpression::class.java) ?: return null
    return getPyClassByPyCallExpression(pyCallExpression, true, context)
}

fun isPydanticModel(pyClass: PyClass, includeDataclass: Boolean, context: TypeEvalContext): Boolean {
    return (isSubClassOfPydanticBaseModel(pyClass,
        context) || isSubClassOfPydanticGenericModel(pyClass,
        context) || (includeDataclass && pyClass.isPydanticDataclass)) && !pyClass.isPydanticBaseModel
            && !pyClass.isPydanticGenericModel && !pyClass.isBaseSettings
}

val PyClass.isPydanticBaseModel: Boolean get() = qualifiedName == BASE_MODEL_Q_NAME


val PyClass.isPydanticGenericModel: Boolean get() = qualifiedName == GENERIC_MODEL_Q_NAME


internal fun isSubClassOfPydanticGenericModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(GENERIC_MODEL_Q_NAME, context)
}

internal fun isSubClassOfPydanticBaseModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(BASE_MODEL_Q_NAME, context)
}

internal fun isSubClassOfBaseSetting(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(BASE_SETTINGS_Q_NAME, context)
}

internal val PyClass.isBaseSettings: Boolean get() = qualifiedName == BASE_SETTINGS_Q_NAME


internal fun hasDecorator(pyDecoratable: PyDecoratable, refNames: List<QualifiedName>): Boolean {
    return pyDecoratable.decoratorList?.decorators?.mapNotNull { it.callee as? PyReferenceExpression }?.any {
        PyResolveUtil.resolveImportedElementQNameLocally(it).any { decoratorQualifiedName ->
            refNames.any { refName -> decoratorQualifiedName == refName }
        }
    } ?: false
}

internal val PyClass.isPydanticDataclass: Boolean get() = hasDecorator(this, DATA_CLASS_QUALIFIED_NAMES)


internal fun isPydanticSchema(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(SCHEMA_Q_NAME, context)
}

internal val PyFunction.isPydanticField: Boolean get() = qualifiedName == FIELD_Q_NAME || qualifiedName == DEPRECATED_SCHEMA_Q_NAME


internal val PyFunction.isDataclassField: Boolean get() = qualifiedName == DATACLASS_FIELD_Q_NAME


internal val PyFunction.isPydanticCreateModel: Boolean get() = qualifiedName == CREATE_MODEL


internal fun isDataclassMissing(pyTargetExpression: PyTargetExpression): Boolean {
    return pyTargetExpression.qualifiedName == DATACLASS_MISSING
}

internal val PyFunction.isValidatorMethod: Boolean get() = hasDecorator(this, VALIDATOR_QUALIFIED_NAMES)


internal val PyClass.isConfigClass: Boolean get() = name == "Config"


internal val PyFunction.isConStr: Boolean get() = qualifiedName == CON_STR_Q_NAME

internal fun isPydanticRegex(stringLiteralExpression: StringLiteralExpression): Boolean {
    val pyKeywordArgument = stringLiteralExpression.parent as? PyKeywordArgument ?: return false
    if (pyKeywordArgument.keyword != "regex") return false
    val pyCallExpression = pyKeywordArgument.parent.parent as? PyCallExpression ?: return false
    val referenceExpression = pyCallExpression.callee as? PyReferenceExpression ?: return false
    val context = TypeEvalContext.userInitiated(referenceExpression.project, referenceExpression.containingFile)
    return getResolvedPsiElements(referenceExpression, context)
        .filterIsInstance<PyFunction>()
        .filter { pyFunction -> pyFunction.isPydanticField || pyFunction.isConStr }
        .any()
}

internal fun getClassVariables(pyClass: PyClass, context: TypeEvalContext): Sequence<PyTargetExpression> {
    return pyClass.classAttributes
        .asReversed()
        .asSequence()
        .filterNot { PyTypingTypeProvider.isClassVar(it, context) }
}

private fun getAliasedFieldName(
    field: PyTargetExpression,
    context: TypeEvalContext,
    pydanticVersion: KotlinVersion?,
): String? {
    val fieldName = field.name
    val assignedField = field.findAssignedValue()?.let {
        getFieldFromPyExpression(it, context, pydanticVersion)
    } ?: (field.annotation?.value as? PySubscriptionExpression)
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


fun getResolvedPsiElements(referenceExpression: PyReferenceExpression, context: TypeEvalContext): List<PsiElement> {
    return PyUtil.multiResolveTopPriority(referenceExpression,
        PyResolveContext.defaultContext().withTypeEvalContext(context))
}

val PyType.pyClassTypes: List<PyClassType>
    get() = when (this) {
        is PyUnionType -> this.members.mapNotNull { it }.flatMap { it.pyClassTypes }
        is PyClassType -> listOf(this)
        else -> listOf()

    }


fun isPydanticSchemaByPsiElement(psiElement: PsiElement, context: TypeEvalContext): Boolean {
    return (psiElement as? PyClass ?: PsiTreeUtil.getContextOfType(psiElement, PyClass::class.java))
        ?.let { isPydanticSchema(it, context) } ?: false

}

inline fun <reified T : PsiElement> validatePsiElementByFunction(
    psiElement: PsiElement,
    validator: (T) -> Boolean,
): Boolean {
    return when {
        T::class.java.isInstance(psiElement) -> validator(psiElement as T)
        else -> PsiTreeUtil.getContextOfType(psiElement, T::class.java)
            ?.let { validator(it) } ?: false
    }
}

val PsiElement.isPydanticField: Boolean
    get() = validatePsiElementByFunction(this) { pyFunction: PyFunction ->
        pyFunction.isPydanticField
    }


val PsiElement.isDataclassField: Boolean
    get() = validatePsiElementByFunction(this) { pyFunction: PyFunction ->
        pyFunction.isDataclassField
    }

val PsiElement.isDataclassMissing: Boolean get() = validatePsiElementByFunction(this, ::isDataclassMissing)

val Project.sdk: Sdk? get() = pythonSdk ?: modules.mapNotNull { PythonSdkUtil.findPythonSdk(it) }.firstOrNull()


fun getPsiElementByQualifiedName(
    qualifiedName: QualifiedName,
    project: Project,
    context: TypeEvalContext,
): PsiElement? {
    val pythonSdk = project.sdk ?: return null
    val module = project.modules.firstOrNull { pythonSdk.isAssociatedWithModule(it) } ?: project.modules.firstOrNull()
    ?: return null
    val contextAnchor = ModuleBasedContextAnchor(module)
    return qualifiedName.resolveToElement(QNameResolveContext(contextAnchor, pythonSdk, context))
}

fun isValidField(field: PyTargetExpression, context: TypeEvalContext): Boolean {
    if (field.name?.isValidFieldName != true) return false

    val annotationValue = field.annotation?.value ?: return true
    // TODO Support a variable.
    return getQualifiedName(annotationValue, context) != CLASSVAR_Q_NAME
}

val String.isValidFieldName: Boolean get() = !startsWith('_') || this == CUSTOM_ROOT_FIELD


fun getConfigValue(name: String, value: Any?, context: TypeEvalContext): Any? {
    if (value is PyReferenceExpression) {
        val targetExpression = getResolvedPsiElements(value, context).firstOrNull() ?: return null
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
        ConfigType.EXTRA -> {
            when ((value as? PyStringLiteralExpression)?.stringValue) {
                "allow" -> EXTRA.ALLOW
                "ignore" -> EXTRA.IGNORE
                "forbid" -> EXTRA.FORBID
                else -> EXTRA.IGNORE
            }
        }
        else -> null
    }
}

fun validateConfig(pyClass: PyClass, context: TypeEvalContext): List<PsiElement>? {
    val configClass = pyClass.nestedClasses.firstOrNull { it.isConfigClass } ?: return null
    val allowedConfigKwargs = PydanticCacheService.getAllowedConfigKwargs(pyClass.project, context) ?: return null
    val configKwargs = pyClass.superClassExpressions.filterIsInstance<PyKeywordArgument>()
        .filter { allowedConfigKwargs.contains(it.name) }
        .takeIf { it.isNotEmpty() } ?: return null

    val results: MutableList<PsiElement> = configKwargs.toMutableList()
    configClass.nameNode?.psi?.let { results.add(it) }
    return results
}

fun getConfig(
    pyClass: PyClass,
    context: TypeEvalContext,
    setDefault: Boolean,
    pydanticVersion: KotlinVersion? = null,
): HashMap<String, Any?> {
    val config = hashMapOf<String, Any?>()
    val version = pydanticVersion ?: PydanticCacheService.getVersion(pyClass.project, context)
    pyClass.getAncestorClasses(context)
        .reversed()
        .filter { isPydanticModel(it, false, context) }
        .map { getConfig(it, context, false, version) }
        .forEach {
            it.entries.forEach { entry ->
                if (entry.value != null) {
                    config[entry.key] = getConfigValue(entry.key, entry.value, context)
                }
            }
        }
    pyClass.nestedClasses.firstOrNull { it.isConfigClass }?.let {
        it.classAttributes.forEach { attribute ->
            attribute.findAssignedValue()?.let { value ->
                attribute.name?.let { name ->
                    config[name] = getConfigValue(name, value, context)
                }
            }
        }
    }

    if (version?.isAtLeast(1, 8) == true) {
        pyClass.superClassExpressions.filterIsInstance<PyKeywordArgument>().forEach {
            it.name?.let { name ->
                config[name] = getConfigValue(name, it.valueExpression, context)
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

fun getFieldName(
    field: PyTargetExpression,
    context: TypeEvalContext,
    config: HashMap<String, Any?>,
    pydanticVersion: KotlinVersion?,
): String? {

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
        psiElement =
            getPsiElementByQualifiedName(QualifiedName.fromDottedString("builtins.$qualifiedName"), project, context)
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
    var pyCallExpression: PyCallExpression? =
        PsiTreeUtil.getParentOfType(file.findElementAt(offset), PyCallExpression::class.java, true)
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

fun getPydanticUnFilledArguments(
    pyClass: PyClass?,
    pyCallExpression: PyCallExpression,
    pydanticTypeProvider: PydanticTypeProvider,
    context: TypeEvalContext,
): List<PyCallableParameter> {
    val pydanticClass = pyClass ?: getPydanticPyClass(pyCallExpression, context) ?: return emptyList()
    val pydanticType = pydanticTypeProvider.getPydanticTypeForClass(pydanticClass, context, true, pyCallExpression)
        ?: return emptyList()
    val currentArguments =
        pyCallExpression.arguments.filter { it is PyKeywordArgument || (it as? PyStarArgumentImpl)?.isKeyword == true }
            .mapNotNull { it.name }.toSet()
    return pydanticType.getParameters(context)?.filterNot { currentArguments.contains(it.name) } ?: emptyList()
}

val PyCallableParameter.required: Boolean
    get() = !hasDefaultValue() || (defaultValue !is PyNoneLiteralExpression && defaultValueText == "...")


fun getPyTypeFromPyExpression(pyExpression: PyExpression, context: TypeEvalContext): PyType? {
    return when (pyExpression) {
        is PyType -> pyExpression
        is PyReferenceExpression -> {
            getResolvedPsiElements(pyExpression, context)
                .asSequence()
                .filterIsInstance<PyClass>()
                .map { pyClass -> pyClass.getType(context)?.getReturnType(context) }
                .firstOrNull()
        }
        else -> null
    }
}

internal fun hasTargetPyType(
    pyExpression: PyExpression,
    targetPyTypes: List<PyType>,
    context: TypeEvalContext,
): Boolean {
    val callee = (pyExpression as? PyCallExpression)?.callee ?: return false
    val pyType = getPyTypeFromPyExpression(callee, context) ?: return false
    val defaultValueTypeClassQName = pyType.declarationElement?.qualifiedName ?: return false
    return targetPyTypes.any { it.declarationElement?.qualifiedName == defaultValueTypeClassQName }
}

internal fun isUntouchedClass(
    pyExpression: PyExpression?,
    config: HashMap<String, Any?>,
    context: TypeEvalContext,
): Boolean {
    if (pyExpression == null) return false
    val keepUntouchedClasses =
        (config["keep_untouched"] as? List<*>)?.filterIsInstance<PyType>()?.toList() ?: return false
    if (keepUntouchedClasses.isNullOrEmpty()) return false
    return (hasTargetPyType(pyExpression, keepUntouchedClasses, context))
}

internal fun getFieldFromPyExpression(
    psiElement: PsiElement,
    context: TypeEvalContext,
    pydanticVersion: KotlinVersion?,
): PyCallExpression? {
    val callee = (psiElement as? PyCallExpression)
        ?.let { it.callee as? PyReferenceExpression }
        ?: return null
    val versionZero = pydanticVersion?.major == 0
    if (!getResolvedPsiElements(callee, context).any {
            when {
                versionZero -> isPydanticSchemaByPsiElement(it, context)
                else -> it.isPydanticField
            }
        }) return null
    return psiElement
}

internal fun getFieldFromAnnotated(annotated: PyExpression, context: TypeEvalContext): PyCallExpression? =
    annotated.children
        .filterIsInstance<PyTupleExpression>()
        .firstOrNull()
        ?.children
        ?.getOrNull(1)
        ?.let {
            getFieldFromPyExpression(it, context, null)
        }

internal fun getTypeExpressionFromAnnotated(annotated: PyExpression): PyExpression? =
    annotated.children
        .filterIsInstance<PyTupleExpression>()
        .firstOrNull()
        ?.children
        ?.getOrNull(0)
        ?.let { it as? PyExpression }

internal fun getDefaultFromField(field: PyCallExpression, context: TypeEvalContext): PyExpression? =
    field.getKeywordArgument("default")
        ?: field.getArgument(0, PyExpression::class.java)?.let {
            when {
                it is PyReferenceExpression -> getResolvedPsiElements(it, context).firstOrNull() as? PyExpression
                it.name == null -> it
                else -> null
            }
        }

internal fun getDefaultFactoryFromField(field: PyCallExpression): PyExpression? =
    field.getKeywordArgument("default_factory")

internal fun getQualifiedName(pyExpression: PyExpression, context: TypeEvalContext): String? {
    return when (pyExpression) {
        is PySubscriptionExpression -> pyExpression.qualifier?.let { getQualifiedName(it, context) }
        is PyReferenceExpression -> return getResolvedPsiElements(pyExpression, context)
            .asSequence()
            .filterIsInstance<PyQualifiedNameOwner>()
            .mapNotNull { it.qualifiedName }
            .firstOrNull()
        else -> return null
    }
}

fun getPydanticModelInit(pyClass: PyClass, context: TypeEvalContext): PyFunction? {
    if (PydanticConfigService.getInstance(pyClass.project).ignoreInitMethodArguments) return null
    val pyFunction = pyClass.findInitOrNew(true, context) ?: return null
    if (pyFunction.name != PyNames.INIT) return null
    val containingClass = pyFunction.containingClass ?: return null
    if (!isPydanticModel(containingClass, false, context)) return null
    return pyFunction
}