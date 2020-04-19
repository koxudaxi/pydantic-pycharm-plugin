package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.codeInsight.typing.matchingProtocolDefinitions
import com.jetbrains.python.documentation.PythonDocumentationProvider
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.PyCallExpression.PyArgumentsMapping
import com.jetbrains.python.psi.impl.PyCallExpressionHelper
import com.jetbrains.python.psi.types.*
import com.jetbrains.python.psi.types.PyLiteralType.Companion.promoteToLiteral
import com.jetbrains.python.psi.types.PyTypedDictType.Companion.match

class PydanticTypeCheckerInspection : PyTypeCheckerInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        if (LOG.isDebugEnabled) {
            session.putUserData(TIME_KEY, System.nanoTime())
        }
        return Visitor(holder, session)
    }

    class Visitor(holder: ProblemsHolder?, session: LocalInspectionToolSession) : PyTypeCheckerInspection.Visitor(holder, session) {

        val pydanticConfigService = PydanticConfigService.getInstance(holder!!.project)

        override fun visitPyCallExpression(node: PyCallExpression) {
            val pyClass = getPyClassByPyCallExpression(node, true, myTypeEvalContext)
            getPyClassByPyCallExpression(node, true, myTypeEvalContext)
            if (pyClass is PyClass && isPydanticModel(pyClass, true)) {
                checkCallSiteForPydantic(node)
                return
            }
            super.visitPyCallExpression(node)
        }


        private fun checkCallSiteForPydantic(callSite: PyCallSiteExpression) {
            PyCallExpressionHelper.mapArguments(callSite, resolveContext)
                    .filter { mapping: PyArgumentsMapping? -> mapping!!.unmappedArguments.isEmpty() && mapping.unmappedParameters.isEmpty() }
                    .forEach { mapping: PyArgumentsMapping -> analyzeCallee(callSite, mapping) }
        }

        private fun getParsableTypeFromTypeMap(typeForParameter: PyType, cache: MutableMap<PyType, PyType?>): PyType? {
            return getTypeFromTypeMap(
                    { project: Project -> PydanticConfigService.getInstance(project).parsableTypeMap },
                    typeForParameter,
                    cache
            )
        }

        private fun getAcceptableTypeFromTypeMap(typeForParameter: PyType, cache: MutableMap<PyType, PyType?>): PyType? {
            return getTypeFromTypeMap(
                    { project: Project -> PydanticConfigService.getInstance(project).acceptableTypeMap },
                    typeForParameter,
                    cache
            )
        }

        private fun getTypeFromTypeMap(getTypeMap: (project: Project) -> (MutableMap<String, List<String>>), typeForParameter: PyType, cache: MutableMap<PyType, PyType?>): PyType? {
            return when {
                cache.containsKey(typeForParameter) -> {
                    cache[typeForParameter]
                }
                else -> {
                    val project = holder!!.project
                    val typeMap = getTypeMap(project)
                    val unionType = PyUnionType.union(getPyClassTypeByPyTypes(typeForParameter).toSet().flatMap { type ->
                        typeMap[type.classQName]?.mapNotNull {
                            createPyClassTypeImpl(it, project, myTypeEvalContext)
                        } as? List<PyType> ?: listOf()
                    })
                    cache[typeForParameter] = unionType
                    unionType
                }
            }
        }

        private fun analyzeCallee(callSite: PyCallSiteExpression, mapping: PyArgumentsMapping) {
            val callableType = mapping.callableType ?: return
            val receiver = callSite.getReceiver(callableType.callable)
            val substitutions = PyTypeChecker.unifyReceiver(receiver, myTypeEvalContext)
            val mappedParameters = mapping.mappedParameters
            val cachedParsableTypeMap = mutableMapOf<PyType, PyType?>()
            val cachedAcceptableTypeMap = mutableMapOf<PyType, PyType?>()
            for ((argument, parameter) in PyCallExpressionHelper.getRegularMappedParameters(mappedParameters)) {
                val expected = parameter.getArgumentType(myTypeEvalContext)
                val actual = promoteToLiteral(argument, expected, myTypeEvalContext)
                val strictMatched = matchParameterAndArgument(expected, actual, argument, substitutions)
                val strictResult = AnalyzeArgumentResult(argument, expected, substituteGenerics(expected, substitutions), actual, strictMatched)
                if (!strictResult.isMatched) {
                    val expectedType = PythonDocumentationProvider.getTypeName(strictResult.expectedType, myTypeEvalContext)
                    val actualType = PythonDocumentationProvider.getTypeName(strictResult.actualType, myTypeEvalContext)
                    val parsableType = expected?.let { getParsableTypeFromTypeMap(it, cachedParsableTypeMap) }
                    if (parsableType != null) {
                        val parsableMatched = matchParameterAndArgument(parsableType, actual, argument, substitutions)
                        val parsableResult = AnalyzeArgumentResult(argument, parsableType, substituteGenerics(parsableType, substitutions), actual, parsableMatched)
                        if (parsableResult.isMatched) {
                            registerProblem(
                                    argument,
                                    String.format("Field is of type '%s', '%s' may not be parsable to '%s'",
                                            expectedType,
                                            actualType,
                                            expectedType),
                                    pydanticConfigService.parsableTypeHighlightType
                            )
                            continue
                        } else {
                            registerProblem(argument, String.format("Expected type '%s', got '%s' instead",
                                    expectedType,
                                    actualType)
                            )
                            continue
                        }
                    }
                    val acceptableType = expected?.let { getAcceptableTypeFromTypeMap(it, cachedAcceptableTypeMap) }
                    if (acceptableType != null) {
                        val acceptableMatched = matchParameterAndArgument(acceptableType, actual, argument, substitutions)
                        val acceptableResult = AnalyzeArgumentResult(argument, acceptableType, substituteGenerics(acceptableType, substitutions), actual, acceptableMatched)
                        if (acceptableResult.isMatched) {
                            registerProblem(
                                    argument,
                                    String.format("Field is of type '%s', '%s' is set as an acceptable type in pyproject.toml",
                                            expectedType,
                                            actualType,
                                            expectedType),
                                    pydanticConfigService.acceptableTypeHighlightType
                            )
                            continue
                        } else {
                            registerProblem(argument, String.format("Expected type '%s', got '%s' instead",
                                    expectedType,
                                    actualType)
                            )
                            continue
                        }
                    }
                    registerProblem(argument, String.format("Expected type '%s', got '%s' instead",
                            expectedType,
                            actualType)
                    )
                }
            }
        }

        private fun matchParameterAndArgument(parameterType: PyType?,
                                              argumentType: PyType?,
                                              argument: PyExpression?,
                                              substitutions: Map<PyGenericType, PyType>): Boolean {
            return if (parameterType is PyTypedDictType && argument is PyDictLiteralExpression) match((parameterType as PyTypedDictType?)!!, (argument as PyDictLiteralExpression?)!!, myTypeEvalContext) else PyTypeChecker.match(parameterType, argumentType, myTypeEvalContext, substitutions) &&
                    !matchingProtocolDefinitions(parameterType, argumentType, myTypeEvalContext)
        }

        private fun substituteGenerics(expectedArgumentType: PyType?, substitutions: Map<PyGenericType, PyType>): PyType? {
            return if (PyTypeChecker.hasGenerics(expectedArgumentType, myTypeEvalContext)) PyTypeChecker.substitute(expectedArgumentType, substitutions, myTypeEvalContext) else null
        }

    }

    internal class AnalyzeArgumentResult(val argument: PyExpression,
                                         val expectedType: PyType?,
                                         val expectedTypeAfterSubstitution: PyType?,
                                         val actualType: PyType?,
                                         val isMatched: Boolean)

    companion object {
        private val LOG = Logger.getInstance(PydanticTypeCheckerInspection::class.java.name)
        private val TIME_KEY = Key.create<Long>("PydanticTypeCheckerInspection.StartTime")
    }
}