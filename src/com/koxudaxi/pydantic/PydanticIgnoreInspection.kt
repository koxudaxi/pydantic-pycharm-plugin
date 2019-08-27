package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticIgnoreInspection : PyInspectionExtension() {

    override fun ignoreMethodParameters(function: PyFunction, context: TypeEvalContext): Boolean {
        return function.containingClass?.let { isPydanticModel(it, context) && validatorMethod(function) } == true
    }
}