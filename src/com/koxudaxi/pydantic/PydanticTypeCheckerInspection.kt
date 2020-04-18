package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.Logger
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


        private fun getParsableType(typeForParameter: PyType): PyType? {
            val pyTypes: MutableSet<PyType> = mutableSetOf(typeForParameter)
            val project = holder!!.project
            getPyClassTypeByPyTypes(typeForParameter).toSet().forEach { type ->
                val classQName: String? = type.classQName

                PydanticConfigService.getInstance(project).parsableTypeMap[classQName]?.mapNotNull {
                    createPyClassTypeImpl(it, project, myTypeEvalContext)
                            ?: createPyClassTypeImpl(it, project, myTypeEvalContext)
                }?.toCollection(pyTypes)
            }
            return PyUnionType.union(pyTypes)
        }

        private fun analyzeCallee(callSite: PyCallSiteExpression, mapping: PyArgumentsMapping) {
            val callableType = mapping.callableType ?: return
            val receiver = callSite.getReceiver(callableType.callable)
            val substitutions = PyTypeChecker.unifyReceiver(receiver, myTypeEvalContext)
            val mappedParameters = mapping.mappedParameters
            for ((argument, parameter) in PyCallExpressionHelper.getRegularMappedParameters(mappedParameters)) {
                val expected = parameter.getArgumentType(myTypeEvalContext)
                val parsableType = expected?.let { getParsableType(it) }
                val actual = promoteToLiteral(argument, expected, myTypeEvalContext)
                val strictMatched = matchParameterAndArgument(expected, actual, argument, substitutions)
                val strictResult = AnalyzeArgumentResult(argument, expected, substituteGenerics(expected, substitutions), actual, strictMatched)
                if (!strictResult.isMatched) {
                    val expectedType = PythonDocumentationProvider.getTypeName(strictResult.expectedType, myTypeEvalContext)
                    val actualType = PythonDocumentationProvider.getTypeName(strictResult.actualType, myTypeEvalContext)
                    val parsableMatched = matchParameterAndArgument(parsableType, actual, argument, substitutions)
                    val parsableResult = AnalyzeArgumentResult(argument, parsableType, substituteGenerics(parsableType, substitutions), actual, parsableMatched)
                    if (parsableResult.isMatched) {
                        registerProblem(argument, String.format("Field is of type '%s', '%s' may not be parsable to '%s'",
                                expectedType,
                                actualType,
                                expectedType)
                        )
                    } else {
                        registerProblem(argument, String.format("Expected type '%s', got '%s' instead",
                                expectedType,
                                actualType)
                        )
                    }
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