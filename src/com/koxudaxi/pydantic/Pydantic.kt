package com.koxudaxi.pydantic

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.extensions.ModuleBasedContextAnchor
import com.jetbrains.extensions.QNameResolveContext
import com.jetbrains.extensions.resolveToElement
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.packaging.PyPackageManagers
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyEvaluator
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.*
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules
import java.util.regex.Pattern

const val BASE_MODEL_Q_NAME = "pydantic.main.BaseModel"
const val ROOT_MODEL_Q_NAME = "pydantic.root_model.RootModel"
const val GENERIC_MODEL_Q_NAME = "pydantic.generics.GenericModel"
const val DATA_CLASS_Q_NAME = "pydantic.dataclasses.dataclass"
const val DATA_CLASS_SHORT_Q_NAME = "pydantic.dataclass"
const val VALIDATOR_Q_NAME = "pydantic.class_validators.validator"
const val VALIDATOR_SHORT_Q_NAME = "pydantic.validator"
const val VALIDATOR_DECORATOR_Q_NAME = "pydantic.deprecated.class_validators.validator"
const val ROOT_VALIDATOR_Q_NAME = "pydantic.class_validators.root_validator"
const val ROOT_VALIDATOR_SHORT_Q_NAME = "pydantic.root_validator"
const val FIELD_VALIDATOR_Q_NAME = "pydantic.field_validator"
const val FIELD_VALIDATOR_SHORT_Q_NAME = "pydantic.functional_validators.field_validator"
const val MODEL_VALIDATOR_Q_NAME = "pydantic.model_validator"
const val MODEL_VALIDATOR_SHORT_Q_NAME = "pydantic.functional_validators.model_validator"

const val SCHEMA_Q_NAME = "pydantic.schema.Schema"
const val FIELD_Q_NAME = "pydantic.fields.Field"
const val DATACLASS_FIELD_Q_NAME = "dataclasses.field"
const val SQL_MODEL_FIELD_Q_NAME = "sqlmodel.main.Field"
const val DEPRECATED_SCHEMA_Q_NAME = "pydantic.fields.Schema"
const val BASE_SETTINGS_Q_NAME = "pydantic.env_settings.BaseSettings"
const val VERSION_Q_NAME = "pydantic.version.VERSION"
const val BASE_CONFIG_Q_NAME = "pydantic.main.BaseConfig"
const val CONFIG_DICT_Q_NAME = "pydantic.config.ConfigDict"
const val CONFIG_DICT_SHORT_Q_NAME = "pydantic.ConfigDict"
const val CONFIG_DICT_DEFAULTS_Q_NAME = "pydantic._internal._config.config_defaults"
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

const val SQL_MODEL_Q_NAME = "sqlmodel.main.SQLModel"

val CUSTOM_BASE_MODEL_Q_NAMES = listOf(
    SQL_MODEL_Q_NAME
)

val CUSTOM_MODEL_FIELD_Q_NAMES = listOf(
    SQL_MODEL_FIELD_Q_NAME
)

val DATA_CLASS_Q_NAMES = listOf(DATA_CLASS_Q_NAME, DATA_CLASS_SHORT_Q_NAME)

val VERSION_QUALIFIED_NAME = QualifiedName.fromDottedString(VERSION_Q_NAME)

val BASE_CONFIG_QUALIFIED_NAME = QualifiedName.fromDottedString(BASE_CONFIG_Q_NAME)

val CONFIG_DICT_QUALIFIED_NAME = QualifiedName.fromDottedString(CONFIG_DICT_Q_NAME)

val CONFIG_DICT_DEFAULTS_QUALIFIED_NAME = QualifiedName.fromDottedString(CONFIG_DICT_DEFAULTS_Q_NAME)

val CONFIG_DICT_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(CONFIG_DICT_SHORT_Q_NAME)

val BASE_MODEL_QUALIFIED_NAME = QualifiedName.fromDottedString(BASE_MODEL_Q_NAME)

val VALIDATOR_QUALIFIED_NAME = QualifiedName.fromDottedString(VALIDATOR_Q_NAME)

val VALIDATOR_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(VALIDATOR_SHORT_Q_NAME)

val ROOT_VALIDATOR_QUALIFIED_NAME = QualifiedName.fromDottedString(ROOT_VALIDATOR_Q_NAME)

val ROOT_VALIDATOR_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(ROOT_VALIDATOR_SHORT_Q_NAME)

val FIELD_VALIDATOR_QUALIFIED_NAME = QualifiedName.fromDottedString(FIELD_VALIDATOR_Q_NAME)

val FIELD_VALIDATOR_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(FIELD_VALIDATOR_SHORT_Q_NAME)

val VALIDATOR_DECORATOR_QUALIFIED_NAME = QualifiedName.fromDottedString(VALIDATOR_DECORATOR_Q_NAME)

val MODEL_VALIDATOR_QUALIFIED_NAME = QualifiedName.fromDottedString(MODEL_VALIDATOR_Q_NAME)

val MODEL_VALIDATOR_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(MODEL_VALIDATOR_SHORT_Q_NAME)

val DATA_CLASS_QUALIFIED_NAME = QualifiedName.fromDottedString(DATA_CLASS_Q_NAME)

val DATA_CLASS_SHORT_QUALIFIED_NAME = QualifiedName.fromDottedString(DATA_CLASS_SHORT_Q_NAME)

val SQL_MODEL_QUALIFIED_NAME = QualifiedName.fromDottedString(SQL_MODEL_Q_NAME)

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

val V2_VALIDATOR_QUALIFIED_NAMES = listOf(
    VALIDATOR_QUALIFIED_NAME,
    VALIDATOR_SHORT_QUALIFIED_NAME,
    ROOT_VALIDATOR_QUALIFIED_NAME,
    ROOT_VALIDATOR_SHORT_QUALIFIED_NAME,
    FIELD_VALIDATOR_QUALIFIED_NAME,
    FIELD_VALIDATOR_SHORT_QUALIFIED_NAME,
    MODEL_VALIDATOR_QUALIFIED_NAME,
    MODEL_VALIDATOR_SHORT_QUALIFIED_NAME
)

val MODEL_VALIDATOR_QUALIFIED_NAMES = listOf(
    MODEL_VALIDATOR_QUALIFIED_NAME,
    MODEL_VALIDATOR_SHORT_QUALIFIED_NAME
)
val FIELD_VALIDATOR_Q_NAMES = listOf(
    VALIDATOR_Q_NAME,
    VALIDATOR_SHORT_Q_NAME,
    VALIDATOR_DECORATOR_Q_NAME,
    FIELD_VALIDATOR_Q_NAME,
    FIELD_VALIDATOR_SHORT_Q_NAME
)

val FIELD_VALIDATOR_QUALIFIED_NAMES = listOf(
    VALIDATOR_QUALIFIED_NAME,
    VALIDATOR_SHORT_QUALIFIED_NAME,
    VALIDATOR_DECORATOR_QUALIFIED_NAME,
    FIELD_VALIDATOR_QUALIFIED_NAME,
    FIELD_VALIDATOR_SHORT_QUALIFIED_NAME
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
    "extra" to ConfigType.EXTRA,
    "populate_by_name" to ConfigType.BOOLEAN,
    "from_attributes" to ConfigType.BOOLEAN,
)

const val CUSTOM_ROOT_FIELD = "__root__"

const val MODEL_FIELD_PREFIX = "model_"

const val MODEL_CONFIG_FIELD = "model_config"

fun PyTypedElement.getType(context: TypeEvalContext): PyType? = context.getType(this)


fun getPydanticModelByPyKeywordArgument(
    pyKeywordArgument: PyKeywordArgument,
    includeDataclass: Boolean,
    context: TypeEvalContext,
): PyClass? {
    val pyCallExpression = PsiTreeUtil.getParentOfType(pyKeywordArgument, PyCallExpression::class.java) ?: return null
    return getPydanticPyClass(pyCallExpression, context, includeDataclass)
}

fun isPydanticModel(pyClass: PyClass, includeDataclass: Boolean, context: TypeEvalContext): Boolean {
    return ((isSubClassOfPydanticBaseModel(pyClass,
        context) && !pyClass.isPydanticCustomBaseModel) || isSubClassOfPydanticGenericModel(pyClass,
        context) || (includeDataclass && pyClass.isPydanticDataclass) || isSubClassOfCustomBaseModel(pyClass,
        context)) && !pyClass.isPydanticBaseModel
            && !pyClass.isPydanticGenericModel && !pyClass.isBaseSettings && !pyClass.isPydanticCustomBaseModel
}

val PyClass.isPydanticBaseModel: Boolean get() = qualifiedName == BASE_MODEL_Q_NAME

val PyClass.isPydanticCustomBaseModel: Boolean get() = qualifiedName in CUSTOM_BASE_MODEL_Q_NAMES

val PyClass.isPydanticGenericModel: Boolean get() = qualifiedName == GENERIC_MODEL_Q_NAME


internal fun isSubClassOfPydanticGenericModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(GENERIC_MODEL_Q_NAME, context)
}

internal fun isSubClassOfPydanticBaseModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(BASE_MODEL_Q_NAME, context)
}

internal fun isSubClassOfPydanticRootModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(ROOT_MODEL_Q_NAME, context)
}

internal fun isSubClassOfBaseSetting(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(BASE_SETTINGS_Q_NAME, context)
}

internal fun isSubClassOfCustomBaseModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return CUSTOM_BASE_MODEL_Q_NAMES.any { pyClass.isSubclass(it, context) }
}

internal val PyClass.isBaseSettings: Boolean get() = qualifiedName == BASE_SETTINGS_Q_NAME


internal fun hasDecorator(pyDecoratable: PyDecoratable, refNames: List<QualifiedName>): Boolean =
    pyDecoratable.decoratorList?.decorators?.any {it.include(refNames)} ?: false


internal val PyClass.isPydanticDataclass: Boolean get() = hasDecorator(this, DATA_CLASS_QUALIFIED_NAMES)


internal fun isPydanticSchema(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(SCHEMA_Q_NAME, context)
}

internal val PyFunction.isPydanticField: Boolean get() = qualifiedName == FIELD_Q_NAME || qualifiedName == DEPRECATED_SCHEMA_Q_NAME

internal val PyFunction.isDataclassField: Boolean get() = qualifiedName == DATACLASS_FIELD_Q_NAME

internal val PyFunction.isCustomModelField: Boolean get() = qualifiedName in CUSTOM_MODEL_FIELD_Q_NAMES

internal val PyFunction.isPydanticCreateModel: Boolean get() = qualifiedName == CREATE_MODEL


internal fun isDataclassMissing(pyTargetExpression: PyTargetExpression): Boolean {
    return pyTargetExpression.qualifiedName == DATACLASS_MISSING
}

internal fun PyFunction.hasValidatorMethod(pydanticVersion: KotlinVersion?): Boolean =
    hasDecorator(this, if(pydanticVersion.isV2) V2_VALIDATOR_QUALIFIED_NAMES else VALIDATOR_QUALIFIED_NAMES)

internal fun PyDecorator.include(refNames: List<QualifiedName>): Boolean = (callee as? PyReferenceExpression)?.let {
        PyResolveUtil.resolveImportedElementQNameLocally(it).any { decoratorQualifiedName ->
            refNames.any { refName -> decoratorQualifiedName == refName }
        }
} ?: false

internal val PyKeywordArgument.value: PyExpression?
    get() = when (val value = valueExpression) {
            is PyReferenceExpression -> (value.reference.resolve() as? PyTargetExpression)?.findAssignedValue()
            else -> value
        }

internal fun PyFunction.hasModelValidatorModeAfter(): Boolean = decoratorList?.decorators
    ?.filter { it.include(MODEL_VALIDATOR_QUALIFIED_NAMES) }
    ?.any { modelValidator ->
        modelValidator.argumentList?.getKeywordArgument("mode")
            ?.let { it.value as? PyStringLiteralExpression }?.stringValue == "after"
    } ?: false
internal val PyClass.isConfigClass: Boolean get() = name == "Config"


internal val PyFunction.isConStr: Boolean get() = qualifiedName == CON_STR_Q_NAME

internal val PyFunction.isPydanticDataclass: Boolean get() = qualifiedName in DATA_CLASS_Q_NAMES
internal fun isPydanticRegex(stringLiteralExpression: StringLiteralExpression): Boolean {
    val pyKeywordArgument = stringLiteralExpression.parent as? PyKeywordArgument ?: return false
    if (pyKeywordArgument.keyword != "regex") return false
    val pyCallExpression = pyKeywordArgument.parent.parent as? PyCallExpression ?: return false
    val context = TypeEvalContext.userInitiated(pyCallExpression.project, pyCallExpression.containingFile)
    return pyCallExpression.multiResolveCalleeFunction(PyResolveContext.defaultContext(context)).filterIsInstance<PyFunction>()
        .any { pyFunction -> pyFunction.isPydanticField || pyFunction.isConStr || pyFunction.isCustomModelField }
}

internal fun isValidatorField(stringLiteralExpression: StringLiteralExpression, typeEvalContext: TypeEvalContext): Boolean {
    val pyArgumentList = stringLiteralExpression.parent as? PyArgumentList ?: return false
    val pyCallExpression = pyArgumentList.parent as? PyCallExpression ?: return false
    val pyFunction = pyCallExpression.callee?.reference?.resolve() as? PyFunction ?: return false
    if(pyFunction.qualifiedName !in FIELD_VALIDATOR_Q_NAMES) return false
    val pyClass = PsiTreeUtil.getParentOfType(pyCallExpression, PyClass::class.java) ?: return false
    return isPydanticModel(pyClass, true, typeEvalContext)
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
    return RecursionManager.doPreventingRecursion(
        Pair.create(
            referenceExpression,
            context
        ), false
    ) {
        val resolveContext = PyResolveContext.defaultContext(context)
        PyUtil.filterTopPriorityResults(
            referenceExpression.getReference(resolveContext).multiResolve(false)
        )
    } ?: emptyList()
}

val PyType.pyClassTypes: List<PyClassType>
    get() = when (this) {
        is PyUnionType -> this.members.mapNotNull { it }.flatMap { it.pyClassTypes }
        is PyClassType -> listOf(this)
        else -> listOf()

    }

val PyType.isNullable: Boolean
    get() = when (this) {
        is PyUnionType -> this.members.any {
            when (it) {
                is PyNoneType -> true
                is PyUnionType -> it.isNullable
                else -> false
            }
        }
        is PyNoneLiteralExpression -> true
        else -> false
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

val PsiElement.isCustomModelField: Boolean
    get() = validatePsiElementByFunction(this) { pyFunction: PyFunction ->
        pyFunction.isCustomModelField
    }

val PsiElement.isDataclassMissing: Boolean get() = validatePsiElementByFunction(this, ::isDataclassMissing)

val Project.sdk: Sdk? get() = pythonSdk ?: modules.firstNotNullOfOrNull { PythonSdkUtil.findPythonSdk(it) }


fun getPsiElementByQualifiedName(
    qualifiedName: QualifiedName,
    project: Project,
    context: TypeEvalContext,
): PsiElement? {
    val pythonSdk = project.sdk ?: return null
    val module = project.modules.firstOrNull { it.pythonSdk == pythonSdk } ?: project.modules.firstOrNull()
    ?: return null
    val contextAnchor = ModuleBasedContextAnchor(module)
    return qualifiedName.resolveToElement(QNameResolveContext(contextAnchor, pythonSdk, context))
}

fun isValidField(field: PyTargetExpression, context: TypeEvalContext, isV2: Boolean): Boolean {
    if (field.name?.isValidFieldName(isV2) != true) return false

    val annotationValue = field.annotation?.value ?: return true
    // TODO Support a variable.
    return getQualifiedName(annotationValue, context) != CLASSVAR_Q_NAME
}

fun String.isValidFieldName(isV2: Boolean): Boolean = (!startsWith('_') || this == CUSTOM_ROOT_FIELD) && !(isV2 && this.startsWith(MODEL_FIELD_PREFIX))

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
                    is PyTupleExpression -> tupleValue.toList().mapNotNull { context.getType(it) }
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
    val version = pydanticVersion ?: PydanticCacheService.getVersion(pyClass.project)
    getAncestorPydanticModels(pyClass, false, context)
        .reversed()
        .map { getConfig(it, context, false, version) }
        .forEach {
            it.entries.forEach { entry ->
                if (entry.value != null) {
                    config[entry.key] = getConfigValue(entry.key, entry.value, context)
                }
            }
        }
    if (version?.isV2 == true) {
        val configDict = pyClass.findClassAttribute(MODEL_CONFIG_FIELD, false, context)?.findAssignedValue().let {
            when (it) {
                is PyReferenceExpression -> {
                    val targetExpression = getResolvedPsiElements(it, context).firstOrNull() ?: return@let null
                    (targetExpression as? PyTargetExpression)?.findAssignedValue() ?: return@let null
                }
                else -> it
            }
        }
        when (configDict) {
            is PyDictLiteralExpression -> configDict.elements.forEach { element ->
                    element.key.text.drop(1).dropLast(1).let { name ->
                        config[name] = getConfigValue(name, element.value, context)
                    }
                }
            is PyCallExpression -> configDict.arguments.forEach { argument ->
                    argument.name?.let {name ->
                        configDict.getKeywordArgument(name)?.let { value ->
                            config[name] = getConfigValue(name, value, context)
                        }
                    }
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
        if (version?.isV2 == true) {
            PydanticCacheService.getConfigDictDefaults(pyClass.project, context)
                ?.filterNot { config.containsKey(it.key) }
                ?.forEach { (name, value) ->
                    config[name] = value
                }
            }
        } else {
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
        2 -> when {
            config["populate_by_name"] == true -> field.name
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

fun getPydanticConfigDictDefaults(project: Project, context: TypeEvalContext): PyCallExpression? {
    val targetExpression = getPyTargetExpressionFromQualifiedName(CONFIG_DICT_DEFAULTS_QUALIFIED_NAME, project, context) ?: return null
    return targetExpression.findAssignedValue() as? PyCallExpression
}

fun getPydanticBaseModel(project: Project, context: TypeEvalContext): PyClass? {
    return getPyClassFromQualifiedName(BASE_MODEL_QUALIFIED_NAME, project, context)
}

fun getPsiElementFromQualifiedName(qualifiedName: QualifiedName, project: Project, context: TypeEvalContext): PsiElement? {
    val module = project.modules.firstOrNull() ?: return null
    val pythonSdk = module.pythonSdk
    val contextAnchor = ModuleBasedContextAnchor(module)
    return qualifiedName.resolveToElement(QNameResolveContext(contextAnchor, pythonSdk, context))
}

fun getPyClassFromQualifiedName(qualifiedName: QualifiedName, project: Project, context: TypeEvalContext): PyClass? {
    return getPsiElementFromQualifiedName(qualifiedName, project, context) as? PyClass
}

fun getPyTargetExpressionFromQualifiedName(qualifiedName: QualifiedName, project: Project, context: TypeEvalContext): PyTargetExpression? {
    return getPsiElementFromQualifiedName(qualifiedName, project, context) as? PyTargetExpression
}
fun getPyClassByAttribute(pyPsiElement: PsiElement?): PyClass? {
    return pyPsiElement?.parent?.parent as? PyClass
}

fun getPydanticModelByAttribute(pyPsiElement: PsiElement?, includeDataclass: Boolean, context: TypeEvalContext): PyClass? =
    getPyClassByAttribute(pyPsiElement)?.takeIf { isPydanticModel(it, includeDataclass, context) }

fun createPyClassTypeImpl(qualifiedName: String, project: Project, context: TypeEvalContext): PyClassTypeImpl? {
    var psiElement = getPsiElementByQualifiedName(QualifiedName.fromDottedString(qualifiedName), project, context)
    if (psiElement == null) {
        psiElement =
            getPsiElementByQualifiedName(QualifiedName.fromDottedString("builtins.$qualifiedName"), project, context)
                ?: return null
    }
    return PyClassTypeImpl.createTypeByQName(psiElement, qualifiedName, false)
}

fun getPydanticPyClass(pyCallExpression: PyCallExpression, context: TypeEvalContext, includeDataclass: Boolean = false): PyClass? =
        pyCallExpression.callee?.reference?.resolve()
        ?.let { it as? PyClass }
        ?.takeIf { isPydanticModel(it, includeDataclass, context)}

fun getPydanticPyClassType(pyTypedElement: PyTypedElement, context: TypeEvalContext, includeDataclass: Boolean = false): PyClassType? =
    context.getType(pyTypedElement)?.pyClassTypes?.firstOrNull {
        isPydanticModel(it.pyClass, includeDataclass, context)
    }

fun getAncestorPydanticModels(pyClass: PyClass, includeDataclass: Boolean, context: TypeEvalContext): List<PyClass> {
    return pyClass.getAncestorClasses(context).filter {  isPydanticModel(it, includeDataclass, context) }
}
fun getParentOfPydanticCallableExpression(file: PsiFile, offset: Int, context: TypeEvalContext, includeDataclass: Boolean): PyCallExpression? {
    var pyCallExpression: PyCallExpression? =
        PsiTreeUtil.getParentOfType(file.findElementAt(offset), PyCallExpression::class.java, true)
    while (pyCallExpression != null && getPydanticPyClass(pyCallExpression, context, includeDataclass) == null) {
        pyCallExpression = PsiTreeUtil.getParentOfType(pyCallExpression, PyCallExpression::class.java, true)
    }
    return pyCallExpression
}

fun getPydanticCallExpressionAtCaret(file: PsiFile, editor: Editor, context: TypeEvalContext, includeDataclass: Boolean): PyCallExpression? {
    return getParentOfPydanticCallableExpression(file, editor.caretModel.offset, context, includeDataclass)
        ?: getParentOfPydanticCallableExpression(file, editor.caretModel.offset - 1, context, includeDataclass)
}


fun addKeywordArgument(pyCallExpression: PyCallExpression, pyKeywordArgument: PyKeywordArgument) {
    when (val lastArgument = pyCallExpression.arguments.lastOrNull()) {
        null -> pyCallExpression.argumentList?.addArgument(pyKeywordArgument)
        else -> pyCallExpression.argumentList?.addArgumentAfter(pyKeywordArgument, lastArgument)
    }
}

val PyExpression.isKeywordArgument: Boolean get() =
    this is PyKeywordArgument || (this as? PyStarArgument)?.isKeyword == true

fun getPydanticUnFilledArguments(
    pydanticType: PyCallableType,
    pyCallExpression: PyCallExpression,
    context: TypeEvalContext,
    isDataClass: Boolean
): List<PyCallableParameter> {
    val parameters = pydanticType.getParameters(context)?.let { allParameters ->
        if (isDataClass) {
            pyCallExpression.arguments
                .filterNot { it.isKeywordArgument }
                .let { allParameters.drop(it.size) }
        } else {
            allParameters.filterNot { it.declarationElement is PySingleStarParameter }
        }
    } ?: listOf()

    val currentArguments = pyCallExpression.arguments.filter { it.isKeywordArgument }.mapNotNull { it.name }.toSet()
    return parameters.filterNot { currentArguments.contains(it.name) }
}

val PyCallableParameter.required: Boolean
    get() = !hasDefaultValue() || (defaultValue !is PyNoneLiteralExpression && defaultValueText == "...")


internal fun hasTargetPyType(
    pyExpression: PyExpression,
    targetPyTypes: List<PyType>,
    context: TypeEvalContext,
): Boolean {
    val callee = (pyExpression as? PyCallExpression)?.callee ?: return false
    val pyType = callee.getType(context)?.pyClassTypes?.firstOrNull()?.getReturnType(context)
    val defaultValueTypeClassQName = pyType?.declarationElement?.qualifiedName ?: return false
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
    if (keepUntouchedClasses.isEmpty()) return false
    return (hasTargetPyType(pyExpression, keepUntouchedClasses, context))
}

internal fun getFieldFromPyExpression(
    psiElement: PsiElement,
    context: TypeEvalContext,
    pydanticVersion: KotlinVersion?,
): PyCallExpression? {
    if (psiElement !is PyCallExpression) return null
    val versionZero = pydanticVersion?.major == 0
    if (
        !psiElement.multiResolveCalleeFunction(PyResolveContext.defaultContext(context)).any {
            when {
                versionZero -> isPydanticSchemaByPsiElement(it, context)
                else -> it.isPydanticField || it.isCustomModelField
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
    val pydanticConfigService = PydanticConfigService.getInstance(pyClass.project)
    if (pydanticConfigService.ignoreInitMethodArguments) return null
    val pyFunction = pyClass.findInitOrNew(true, context) ?: return null
    if (pyFunction.name != PyNames.INIT) return null
    val containingClass = pyFunction.containingClass ?: return null
    if (!isPydanticModel(containingClass, false, context)) return null

    if (!pydanticConfigService.ignoreInitMethodKeywordArguments) return pyFunction
    val callParameters = pyFunction.getParameters(context)
        .filterNot { parameter -> parameter.isSelf }
    val callParametersWithoutKeywordContainer = callParameters.filterNot {
            parameter -> parameter.isKeywordContainer
    }
    val hasKeywordContainer = callParametersWithoutKeywordContainer.size != callParameters.size
    if (hasKeywordContainer) {
        val hasNonPositionalContainer = callParametersWithoutKeywordContainer.any {
                parameter -> !parameter.isPositionalContainer
        }
        if (!hasNonPositionalContainer) {
            return null
        }
    }
    return pyFunction
}

 fun PyCallExpression.isDefinitionCallExpression(context: TypeEvalContext): Boolean =
     this.callee?.reference?.resolve()?.let { it as? PyClass }?.getType(context)?.isDefinition == true

fun PyCallExpression.getPyCallableType(context: TypeEvalContext): PyCallableType? =
    this.callee?.getType(context) as? PyCallableType
fun PyCallableType.getPydanticModel(includeDataclass: Boolean, context: TypeEvalContext): PyClass? =
    this.getReturnType(context)?.pyClassTypes?.firstOrNull()?.pyClass?.takeIf { isPydanticModel(it,includeDataclass, context) }


val KotlinVersion?.isV2: Boolean
    get() = this?.isAtLeast(2, 0) == true

val Sdk.pydanticVersion: String?
    get() = PyPackageManagers.getInstance()
        .forSdk(this).packages?.find { it.name == "pydantic" }?.version

internal fun isInInit(field: PyTargetExpression): Boolean {
    val assignedValue = field.findAssignedValue() as? PyCallExpression ?: return true
    val initValue = assignedValue.getKeywordArgument("init") ?: return true
    return PyEvaluator.evaluateAsBoolean(initValue, true)
}