package com.phhmaa.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.phhmaa.generator.utils.*
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier

class FakeFileGenerator(
    private val logger: KSPLogger,
) {

    fun generateFakeFile(symbol: KSClassDeclaration): FileSpec {
        val packageName = symbol.packageName.asString()
        val className = symbol.toClassName().simpleName
        val generatedClassName = "Fake${className}"

        val functions = symbol.getFunctions()
        val generatedFunctions = functions.map { function -> generateFunction(function) }

        val fields = symbol.getProperties()
        val generatedFields = fields.map { field -> generateField(field) }

        val builtClass = TypeSpec.classBuilder(generatedClassName)
            .addModifiers(symbol.getModifiers()) // Match modifiers of the original class
            .addOverride(symbol) // Match the superclass or superinterface of the original class
            .primaryConstructor( // Generate primary constructor
                FunSpec.constructorBuilder()
                    .addParameters(functions.getConstructorParameters())
                    .addParameters(fields.getFieldConstructorParameters())
                    .build()
            )
            .addProperties(functions.toConstructorProperties()) // Populate constructor parameters
            .addProperties(generatedFields)
            .addFunctions(generatedFunctions)
            .build()

        return FileSpec
            .builder(packageName = packageName, fileName = generatedClassName)
            .addType(builtClass)
            .build()
    }

    private fun TypeSpec.Builder.addOverride(from: KSClassDeclaration): TypeSpec.Builder {
        if (from.classKind == ClassKind.INTERFACE) {
            addSuperinterface(from.toClassName())
        } else {
            superclass(from.toClassName())
        }
        return this
    }

    private fun KSClassDeclaration.getModifiers() = modifiers.mapNotNull { it.toKModifier() }
}