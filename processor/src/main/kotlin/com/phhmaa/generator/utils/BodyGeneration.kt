package com.phhmaa.generator.utils

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toTypeName


internal fun generateFunction(function: KSFunctionDeclaration): FunSpec {
    val funSpecBuilder = FunSpec.builder(function.simpleName.asString())
        .addParameters(
            function.parameters.map { parameter ->
                ParameterSpec.builder(
                    name = parameter.name!!.asString(),
                    type = parameter.type.toTypeName(),
                ).build()
            }
        )
        .returns(function.returnType!!.toTypeName())
        .addStatement("return ${function.simpleName.getShortName()}Fake(${function.parameters.joinToString { it.name!!.asString() }})") // e.g. fakeCreateOrder(order)
        .addModifiers(KModifier.OVERRIDE)
    return funSpecBuilder.build()
}

internal fun generateField(field: KSPropertyDeclaration): PropertySpec {
    val propertySpecBuilder = PropertySpec.builder(field.simpleName.asString(), field.type.toTypeName())
        .initializer("${field.simpleName.getShortName()}Fake()")
        .addModifiers(KModifier.OVERRIDE)
    return propertySpecBuilder.build()
}