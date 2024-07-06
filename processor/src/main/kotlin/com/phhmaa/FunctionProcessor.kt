@file:Suppress("UnnecessaryVariable")

package com.phhmaa

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

class FunctionProcessor(
    private val options: Map<String, String>,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GenerateFake::class.qualifiedName!!)
        symbols.forEach { symbol ->
            if (symbol is KSClassDeclaration) {
                generateFakeClass(symbol)
            }
        }
        return emptyList()
    }

    private fun generateFakeClass(symbol: KSClassDeclaration) {
        val packageName = symbol.packageName.asString()
        val className = symbol.simpleName.asString()
        val generatedClassName = "${className}Fake"

        // Define the class structure
        val classBuilder = TypeSpec.classBuilder(generatedClassName)
            .addFunction(
                FunSpec.builder("fakeFunction")
                    .addStatement("")
                    .build()
            )

        // Create the .kt file
        val fileSpec = FileSpec.builder(packageName, generatedClassName)
            .addType(classBuilder.build())
            .build()

        fileSpec.writeTo(codeGenerator, dependencies = Dependencies(false, symbol.containingFile!!))
    }
}