package com.koxudaxi.pydantic

import com.jetbrains.python.psi.types.PyAnyType
import com.jetbrains.python.psi.types.PyType

// PyCharm 263 requires explicit Unknown types when its Any/Unknown mode is enabled.
internal fun PyType?.withUnknownFallback(): PyType? =
    this ?: if (PyAnyType.isEnabled) PyAnyType.unknown else null
