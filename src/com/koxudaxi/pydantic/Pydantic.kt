package com.koxudaxi.pydantic

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.extenstions.ModuleBasedContextAnchor
import com.jetbrains.extenstions.QNameResolveContext
import com.jetbrains.extenstions.resolveToElement
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyUnionType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules
import java.util.regex.Pattern

const val BASE_MODEL_Q_NAME = "pydantic.main.BaseModel"
const val DATA_CLASS_Q_NAME = "pydantic.dataclasses.dataclass"
const val VALIDATOR_Q_NAME = "pydantic.validator"
const val ROOT_VALIDATOR_Q_NAME = "pydantic.root_validator"
const val SCHEMA_Q_NAME = "pydantic.schema.Schema"
const val FIELD_Q_NAME = "pydantic.fields.Field"
const val DEPRECATED_SCHEMA_Q_NAME = "pydantic.fields.Schema"
const val BASE_SETTINGS_Q_NAME = "pydantic.env_settings.BaseSettings"
const val VERSION_Q_NAME = "pydantic.version.VERSION"

val VERSION_QUALIFIED_NAME = QualifiedName.fromDottedString(VERSION_Q_NAME)

val VERSION_SPLIT_PATTERN: Pattern = Pattern.compile("[.a-zA-Z]")!!

val pydanticVersionCache: HashMap<String, KotlinVersion> = hashMapOf()

val DEFAULT_CONFIG = mapOf(
        "allow_population_by_alias" to "False",
        "allow_population_by_field_name" to "False"
)

internal fun getPyClassByPyCallExpression(pyCallExpression: PyCallExpression, context: TypeEvalContext): PyClass? {
    val callee = pyCallExpression.callee ?: return null
    val pyType = when (val type = context.getType(callee)) {
        is PyClass -> return type
        is PyClassType -> type
        else -> (callee.reference?.resolve() as? PyTypedElement)?.let { context.getType(it) } ?: return null
    }
    return getPyClassTypeByPyTypes(pyType).firstOrNull { isPydanticModel(it.pyClass) }?.pyClass
}

internal fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument, context: TypeEvalContext): PyClass? {
    val pyCallExpression = PsiTreeUtil.getParentOfType(pyKeywordArgument, PyCallExpression::class.java) ?: return null
    return getPyClassByPyCallExpression(pyCallExpression, context)
}

internal fun isPydanticModel(pyClass: PyClass, context: TypeEvalContext? = null): Boolean {
    return (isSubClassOfPydanticBaseModel(pyClass, context) || isPydanticDataclass(pyClass)) && !isPydanticBaseModel(pyClass)
}

internal fun isPydanticBaseModel(pyClass: PyClass): Boolean {
    return pyClass.qualifiedName == BASE_MODEL_Q_NAME
}

internal fun isSubClassOfPydanticBaseModel(pyClass: PyClass, context: TypeEvalContext?): Boolean {
    return pyClass.isSubclass(BASE_MODEL_Q_NAME, context)
}

internal fun isBaseSetting(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(BASE_SETTINGS_Q_NAME, context)
}

internal fun hasDecorator(pyDecoratable: PyDecoratable, refName: String): Boolean {
    pyDecoratable.decoratorList?.decorators?.mapNotNull { it.callee as? PyReferenceExpression }?.forEach {
        PyResolveUtil.resolveImportedElementQNameLocally(it).forEach { decoratorQualifiedName ->
            if (decoratorQualifiedName == QualifiedName.fromDottedString(refName)) return true
        }
    }
    return false
}

internal fun isPydanticDataclass(pyClass: PyClass): Boolean {
    return hasDecorator(pyClass, DATA_CLASS_Q_NAME)
}

internal fun isPydanticSchema(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(SCHEMA_Q_NAME, context)
}

internal fun isPydanticField(pyFunction: PyFunction): Boolean {
    return pyFunction.qualifiedName == FIELD_Q_NAME || pyFunction.qualifiedName == DEPRECATED_SCHEMA_Q_NAME
}

internal fun isValidatorMethod(pyFunction: PyFunction): Boolean {
    return hasDecorator(pyFunction, VALIDATOR_Q_NAME) || hasDecorator(pyFunction, ROOT_VALIDATOR_Q_NAME)
}

internal fun getClassVariables(pyClass: PyClass, context: TypeEvalContext): Sequence<PyTargetExpression> {
    return pyClass.classAttributes
            .asReversed()
            .asSequence()
            .filterNot { PyTypingTypeProvider.isClassVar(it, context) }
}

private fun getAliasedFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?): String? {
    val fieldName = field.name
    val assignedValue = field.findAssignedValue() ?: return fieldName
    val callee = (assignedValue as? PyCallExpressionImpl)?.callee ?: return fieldName
    val referenceExpression = callee.reference?.element as? PyReferenceExpression ?: return fieldName


    val resolveResults = getResolveElements(referenceExpression, context)

    val versionZero = pydanticVersion?.major == 0
    return PyUtil.filterTopPriorityResults(resolveResults)
            .filter {
                if (versionZero) {
                    isPydanticSchemaByPsiElement(it, context)
                } else {
                    isPydanticFieldByPsiElement(it)
                }

            }
            .mapNotNull {
                when (val alias = assignedValue.getKeywordArgument("alias")) {
                    is StringLiteralExpression -> alias.stringValue
                    is PyReferenceExpression -> ((alias.reference.resolve() as? PyTargetExpressionImpl)
                            ?.findAssignedValue() as? StringLiteralExpression)?.stringValue
                    //TODO Support dynamic assigned Value. eg:  Schema(..., alias=get_alias_name(field_name))
                    else -> null
                }
            }
            .firstOrNull() ?: fieldName
}


internal fun getResolveElements(referenceExpression: PyReferenceExpression, context: TypeEvalContext): Array<ResolveResult> {
    val resolveContext = PyResolveContext.noImplicits().withTypeEvalContext(context)
    return referenceExpression.getReference(resolveContext).multiResolve(false)

}

internal fun getPyClassTypeByPyTypes(pyType: PyType): List<PyClassType> {
    return when (pyType) {
        is PyUnionType ->
            pyType.members
                    .mapNotNull { it }
                    .flatMap {
                        getPyClassTypeByPyTypes(it)
                    }
        is PyClassType -> listOf(pyType)
        else -> listOf()
    }
}


internal fun isPydanticSchemaByPsiElement(psiElement: PsiElement, context: TypeEvalContext): Boolean {
    PsiTreeUtil.getContextOfType(psiElement, PyClass::class.java)
            ?.let { return isPydanticSchema(it, context) }
    return false
}

internal fun isPydanticFieldByPsiElement(psiElement: PsiElement): Boolean {
    when (psiElement) {
        is PyFunction -> return isPydanticField(psiElement)
        else -> PsiTreeUtil.getContextOfType(psiElement, PyFunction::class.java)
                ?.let { return isPydanticField(it) }
    }
    return false
}

internal fun getPydanticVersion(project: Project, context: TypeEvalContext): KotlinVersion? {
    val module = project.modules.firstOrNull() ?: return null
    val pythonSdk = module.pythonSdk
    val contextAnchor = ModuleBasedContextAnchor(module)
    val version = VERSION_QUALIFIED_NAME.resolveToElement(QNameResolveContext(contextAnchor, pythonSdk, context)) as? PyTargetExpressionImpl
            ?: return null
    val versionString = (version.findAssignedValue()?.lastChild?.firstChild?.nextSibling as? PyStringLiteralExpression)?.stringValue
            ?: return null
    return pydanticVersionCache.getOrElse(versionString, {
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

internal fun isValidFieldName(name: String): Boolean {
    return name.first() != '_'
}


internal fun getConfig(pyClass: PyClass, context: TypeEvalContext, setDefault: Boolean): HashMap<String, String?> {
    val config = hashMapOf<String, String?>()
    pyClass.getAncestorClasses(context)
            .reversed()
            .filter { isPydanticModel(it) }
            .map { getConfig(it, context, false) }
            .forEach {
                it.entries.forEach { entry ->
                    if (entry.value != null) {
                        config[entry.key] = entry.value
                    }
                }
            }
    pyClass.nestedClasses.firstOrNull { it.name == "Config" }?.let {
        it.classAttributes.forEach { attribute ->
            attribute.findAssignedValue()?.text?.let { value ->
                attribute.name?.let { name -> config[name] = value }
            }
        }
    }

    if (setDefault) {
        DEFAULT_CONFIG.forEach { (key, value) ->
            if (!config.containsKey(key)) {
                config[key] = value
            }
        }
    }
    return config
}

internal fun getFieldName(field: PyTargetExpression,
                          context: TypeEvalContext,
                          config: HashMap<String, String?>,
                          pydanticVersion: KotlinVersion?): String? {

    return if (pydanticVersion?.major == 0) {
        if (config["allow_population_by_alias"] == "True") {
            field.name
        } else {
            getAliasedFieldName(field, context, pydanticVersion)
        }
    } else {
        if (config["allow_population_by_field_name"] == "True") {
            field.name
        } else {
            getAliasedFieldName(field, context, pydanticVersion)
        }
    }
}