package com.koxudaxi.pydantic

import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyKeywordArgument


fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument) : PyClass? {
    val pyCallExpression = pyKeywordArgument.parent?.parent as? PyCallExpression ?: return null
    return pyCallExpression.callee?.reference?.resolve() as? PyClass ?: return null
}
