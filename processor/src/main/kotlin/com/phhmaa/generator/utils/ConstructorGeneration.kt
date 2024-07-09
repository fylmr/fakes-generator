package com.phhmaa.generator.utils

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toTypeName

/**
 * Generate constructor parameters for the generated class.
 * @see toConstructorProperties
 */
internal fun Iterable<KSFunctionDeclaration>.getConstructorParameters() = map { function ->
    ParameterSpec
        .builder(
            name = getConstructorParameterName(function),
            type = getLambdaOf(function),
        )
        .defaultValue(function.toDefaultParameter())
        .build()
}

internal fun Iterable<KSPropertyDeclaration>.getFieldConstructorParameters() = map { field ->
    ParameterSpec
        .builder(
            name = getConstructorParameterName(field),
            type = getLambdaOf(field),
        )
        .defaultValue(field.toDefaultParameter())
        .build()
}

/**
 * Generate properties = constructor parameters ([getConstructorParameters]) for the generated class.
 */
internal fun Iterable<KSFunctionDeclaration>.toConstructorProperties(): List<PropertySpec> = map { function ->
    PropertySpec
        .builder(
            name = getConstructorParameterName(function),
            type = getLambdaOf(function),
        )
        .initializer(getConstructorParameterName(function))
        .addModifiers(KModifier.PRIVATE)
        .build()
}

private fun getConstructorParameterName(function: KSDeclaration) =
    "${function.simpleName.getShortName()}Fake"

private fun getLambdaOf(function: KSFunctionDeclaration) =
    LambdaTypeName.get(
        parameters = function.parameters.map {
            ParameterSpec(
                it.name!!.asString(),
                it.type.toTypeName()
            )
        },
        returnType = function.returnType!!.toTypeName()
    )

private fun getLambdaOf(field: KSPropertyDeclaration) =
    LambdaTypeName.get(
        parameters = emptyList(),
        returnType = field.type.toTypeName(),
    )
