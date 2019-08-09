package com.koxudaxi.pydantic

import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.impl.stubs.CustomTargetExpressionStub
import com.jetbrains.python.psi.impl.stubs.CustomTargetExpressionStubType
import com.jetbrains.python.psi.resolve.PyResolveUtil
import java.io.IOException

class PydanticFieldStubImpl private constructor(private val calleeName: QualifiedName,
                                                   private val hasDefault: Boolean,
                                                   private val hasDefaultFactory: Boolean,
                                                   private val initValue: Boolean) : PydanticFieldStub {
    companion object {
        fun create(expression: PyTargetExpression): PydanticFieldStub? {
            val value = expression.findAssignedValue() as? PyCallExpression ?: return null
            val callee = value.callee as? PyReferenceExpression ?: return null

            val calleeName = calculateCalleeName(callee) ?: return null
            return PydanticFieldStubImpl(calleeName, hasDefault = true, hasDefaultFactory = true, initValue = true)
        }

        @Throws(IOException::class)
        fun deserialize(stream: StubInputStream): PydanticFieldStub? {
            val calleeName = stream.readNameString() ?: return null
            val hasDefault = stream.readBoolean()
            val hasDefaultFactory = stream.readBoolean()
            val initValue = stream.readBoolean()

            return PydanticFieldStubImpl(QualifiedName.fromDottedString(calleeName), hasDefault, hasDefaultFactory, initValue)
        }

        private fun calculateCalleeName(callee: PyReferenceExpression): QualifiedName? {
            val qualifiedName = callee.asQualifiedName() ?: return null

            val pydanticField = QualifiedName.fromComponents("pydantic", "main", "fields")


            for (originalQName in PyResolveUtil.resolveImportedElementQNameLocally(callee)) {
                when (originalQName) {
                    pydanticField -> return qualifiedName
                }
            }

            return null
        }

    }

    override fun getTypeClass(): Class<out CustomTargetExpressionStubType<out CustomTargetExpressionStub>> {
        return PydanticFieldStubType::class.java
    }

    override fun serialize(stream: StubOutputStream) {
        stream.writeName(calleeName.toString())
        stream.writeBoolean(hasDefault)
        stream.writeBoolean(hasDefaultFactory)
        stream.writeBoolean(initValue)
    }

    override fun getCalleeName(): QualifiedName = calleeName
    override fun hasDefault(): Boolean = hasDefault
    override fun hasDefaultFactory(): Boolean = hasDefaultFactory
    override fun initValue(): Boolean = initValue
}