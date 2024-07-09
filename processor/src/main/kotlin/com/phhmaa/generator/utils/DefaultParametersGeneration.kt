package com.phhmaa.generator.utils

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.phhmaa.FakeReturnValue
import com.squareup.kotlinpoet.CodeBlock


internal fun KSFunctionDeclaration.toDefaultParameter(): CodeBlock? {
    val returnType = returnType?.resolve()

    val userFakeReturnValue = getFakeReturnValue()
    return when {
        returnType == null -> {
            null
        }

        userFakeReturnValue != null -> {
            // Get `value` parameter from FakeReturnValue
            val value = userFakeReturnValue
            CodeBlock.of("{ ${lambdaParams()} $value }")
        }

        else -> {
            val defaultReturnValue = getReturnValue(returnType)
            CodeBlock.of("{ ${lambdaParams()}$defaultReturnValue }")
        }
    }
}

internal fun KSPropertyDeclaration.toDefaultParameter(): CodeBlock? {
    val returnType = type.resolve()

    val userFakeReturnValue = getFakeReturnValue()
    return when {
        userFakeReturnValue != null -> {
            // Get `value` parameter from FakeReturnValue
            val value = userFakeReturnValue
            CodeBlock.of("{ $value }")
        }

        else -> {
            val defaultReturnValue = getReturnValue(returnType)
            CodeBlock.of("{$defaultReturnValue}")
        }
    }
}

internal fun KSDeclaration.getFakeReturnValue() =
    annotations.find { it.shortName.getShortName() == FakeReturnValue::class.simpleName }
        ?.arguments?.find { it.name?.asString() == FakeReturnValue::value.name }
        ?.value?.toString()
        ?.removeSurrounding("\"")


/**
 * Generate lambda parameters for functions with more than one parameter.
 * For example, for this function
 * ```
 * fun updateOrder(id: String, order: Order): Int?
 * ```
 * the generated code will be `_, _ ->`.
 *
 * If the function has only one parameter, return an empty string.
 * For example, for this function
 * ```
 * fun deleteOrder(id: String)
 * ```
 * the generated code will be an empty string.
 */
private fun KSFunctionDeclaration.lambdaParams() = "${parameters.joinToString { "_" }} -> "
    .takeIf { parameters.size > 1 }
    .orEmpty()
