package com.phhmaa.generator.utils

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName

/**
 * Recursively generate instances of custom classes.
 * For example, for this class
 * ```
 * data class Order(
 *     val id: String,
 *     val name: String,
 *     val price: Double,
 *     val quantity: Int,
 * )
 * ```
 * the generated code will be
 * ```
 * Order("", "", 0.0, 0)
 * ```
 */
private fun generateCustomReturnValue(returnType: KSType): String {
    if (returnType.isMarkedNullable) {
        return "null"
    }

    val declaration = returnType.declaration as? KSClassDeclaration ?: return ""

    val properties = declaration.declarations.filterIsInstance<KSPropertyDeclaration>()
        .filter { it.getVisibility() != Visibility.PRIVATE }
        .map { property ->
            val propertyType = property.type.resolve()
            val defaultValue = getReturnValue(propertyType)
            "${property.simpleName.getShortName()} = $defaultValue"
        }
        .joinToString(", ")
    return "${declaration.simpleName.getShortName()}($properties)"
}

internal fun getReturnValue(returnType: KSType) = when (returnType.toTypeName()) {
    STRING -> "\"\""
    INT -> "0"
    DOUBLE -> "0.0"
    BOOLEAN -> "false"
    LONG -> "0L"
    CHAR -> "' '"
    BYTE -> "0.toByte()"
    SHORT -> "0.toShort()"
    FLOAT -> "0.0f"
    UNIT -> ""
    LIST -> "emptyList()"
    SET -> "emptySet()"
    MAP -> "emptyMap()"
    ARRAY -> "emptyArray()"
    else -> generateCustomReturnValue(returnType)
}