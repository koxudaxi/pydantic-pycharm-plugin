package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.codeInsight.typing.matchingProtocolDefinitions
import com.jetbrains.python.documentation.PythonDocumentationProvider
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyCallExpression.PyArgumentsMapping
import com.jetbrains.python.psi.PyCallSiteExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.types.*
import com.jetbrains.python.psi.types.PyLiteralType.Companion.promoteToLiteral


@Suppress("UnstableApiUsage")
class PydanticTypeCheckerInspection : PyTypeCheckerInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor {
        if (LOG.isDebugEnabled) {
            session.putUserData(TIME_KEY, System.nanoTime())
        }
        return Visitor(holder, PyInspectionVisitor.getContext(session))
    }

    class Visitor(holder: ProblemsHolder, context: TypeEvalContext) :
        PyTypeCheckerInspection.Visitor(holder, context) {

        private val pydanticConfigService = PydanticConfigService.getInstance(holder!!.project)

        override fun visitPyCallExpression(node: PyCallExpression) {
            super.visitPyCallExpression(node)
            
            val pyClass = getPydanticPyClass(node, myTypeEvalContext, true)
            if (pyClass != null) {
                val mappings = node.multiMapArguments(resolveContext)
                for (mapping in mappings) {
                    if (mapping.unmappedArguments.isEmpty() && mapping.unmappedParameters.isEmpty()) {
                        analyzeCalleeForPydantic(node, mapping)
                    }
                }
            }
        }

        private fun getParsableTypeFromTypeMap(typeForParameter: PyType, cache: MutableMap<PyType, PyType?>): PyType? {
            return getTypeFromTypeMap(
                { project: Project -> PydanticConfigService.getInstance(project).parsableTypeMap },
                typeForParameter,
                cache
            )
        }

        private fun getAcceptableTypeFromTypeMap(
            typeForParameter: PyType,
            cache: MutableMap<PyType, PyType?>,
        ): PyType? {
            return getTypeFromTypeMap(
                { project: Project -> PydanticConfigService.getInstance(project).acceptableTypeMap },
                typeForParameter,
                cache
            )
        }

        private fun getTypeFromTypeMap(
            getTypeMap: (project: Project) -> (Map<String, List<String>>),
            typeForParameter: PyType,
            cache: MutableMap<PyType, PyType?>,
        ): PyType? {
            if (cache.containsKey(typeForParameter)) {
                return cache[typeForParameter]
            }

            val newType = when (typeForParameter) {
                is PyCollectionType ->
                    typeForParameter.elementTypes.mapNotNull {
                        it?.let { getTypeFromTypeMap(getTypeMap, it, cache) }
                    }.takeIf {
                        it.isNotEmpty()
                    }?.let { PyCollectionTypeImpl(typeForParameter.pyClass, typeForParameter.isDefinition, it) }
                else -> {
                    val project = holder!!.project
                    PyUnionType.union(typeForParameter.pyClassTypes.toSet().flatMap { type ->
                        getTypeMap(project)[type.classQName]?.mapNotNull {
                            createPyClassTypeImpl(it, project, myTypeEvalContext)
                        } as? List<PyType> ?: listOf()
                    })
                }
            }
            cache[typeForParameter] = newType
            return newType
        }

        private fun analyzeCalleeForPydantic(callSite: PyCallSiteExpression, mapping: PyArgumentsMapping) {
            val callableType = mapping.callableType ?: return
            val receiver = callSite.getReceiver(callableType.callable)
            val substitutions = PyTypeChecker.unifyReceiver(receiver, myTypeEvalContext)
            val mappedParameters = mapping.mappedParameters
            val cachedParsableTypeMap = mutableMapOf<PyType, PyType?>()
            val cachedAcceptableTypeMap = mutableMapOf<PyType, PyType?>()
            for (entry in mappedParameters.entries) {
                // In IntelliJ 2025.2, mappedParameters maps PyExpression -> PyCallableParameter
                val argumentExpression = entry.key ?: continue
                val parameter = entry.value
                val expected = parameter.getType(myTypeEvalContext)
                val promotedToLiteral = promoteToLiteral(argumentExpression, expected, myTypeEvalContext, substitutions)
                val actual = promotedToLiteral ?: myTypeEvalContext.getType(argumentExpression)
                val strictMatched = matchParameterAndArgument(expected, actual, substitutions)
                val strictResult = AnalyzeArgumentResult(expected, actual, strictMatched)
                if (!strictResult.isMatched) {
                    val expectedType =
                        PythonDocumentationProvider.getTypeName(strictResult.expectedType, myTypeEvalContext)
                    val actualType = PythonDocumentationProvider.getTypeName(strictResult.actualType, myTypeEvalContext)
                    if (expected is PyType) {
                        val parsableType = getParsableTypeFromTypeMap(expected, cachedParsableTypeMap)
                        if (parsableType != null) {
                            val parsableMatched =
                                matchParameterAndArgument(parsableType, actual, substitutions)
                            if (AnalyzeArgumentResult(parsableType, actual, parsableMatched).isMatched) {
                                holder.registerProblem(
                                    argumentExpression,
                                    String.format("Field is of type '%s', '%s' may not be parsable to '%s'",
                                        expectedType,
                                        actualType,
                                        expectedType),
                                    pydanticConfigService.parsableTypeHighlightType
                                )
                                continue
                            }
                        }
                        val acceptableType = getAcceptableTypeFromTypeMap(expected, cachedAcceptableTypeMap)
                        if (acceptableType != null) {
                            val acceptableMatched =
                                matchParameterAndArgument(acceptableType, actual, substitutions)
                            if (AnalyzeArgumentResult(acceptableType, actual, acceptableMatched).isMatched) {
                                holder.registerProblem(
                                    argumentExpression,
                                    String.format("Field is of type '%s', '%s' is set as an acceptable type in pyproject.toml",
                                        expectedType,
                                        actualType),
                                    pydanticConfigService.acceptableTypeHighlightType
                                )
                                continue
                            }
                        }
                    }
                    holder.registerProblem(argumentExpression, String.format("Expected type '%s', got '%s' instead",
                        expectedType,
                        actualType)
                    )
                }
            }
        }

        private fun matchParameterAndArgument(
            parameterType: PyType?,
            argumentType: PyType?,
            substitutions: PyTypeChecker.GenericSubstitutions,
        ): Boolean {
            return PyTypeChecker.match(parameterType,
                argumentType,
                myTypeEvalContext,
                substitutions) &&
                    !matchingProtocolDefinitions(parameterType, argumentType, myTypeEvalContext)
        }
    }

    internal class AnalyzeArgumentResult(
        val expectedType: PyType?,
        val actualType: PyType?,
        val isMatched: Boolean,
    )

    companion object {
        private val LOG = Logger.getInstance(PydanticTypeCheckerInspection::class.java.name)
        private val TIME_KEY = Key.create<Long>("PydanticTypeCheckerInspection.StartTime")
    }
}