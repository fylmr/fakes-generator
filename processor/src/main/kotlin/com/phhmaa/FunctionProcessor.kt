@file:Suppress("UnnecessaryVariable")

package com.phhmaa

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration


/**
 * This processor handles interfaces annotated with @Function.
 * It generates the function for each annotated interface. For each property of the interface it adds an argument for
 * the generated function with the same type and name.
 *
 * For example, the following code:
 *
 * ```kotlin
 * @Function(name = "myFunction")
 * interface MyFunction {
 *     val arg1: String
 *     val arg2: List<List<*>>
 * }
 * ```
 *
 * Will generate the corresponding function:
 *
 * ```kotlin
 * fun myFunction(
 *     arg1: String,
 *     arg2: List<List<*>>
 * ) {
 *     println("Hello from myFunction")
 * }
 * ```
 */
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
        val file = codeGenerator.createNewFile(
            Dependencies(false, symbol.containingFile!!),
            packageName,
            generatedClassName
        )
        file.bufferedWriter().use { writer ->
            writer.write(
                """
                package $packageName
                
                class $generatedClassName {
                    // This is a generated fake class
                }
                """.trimIndent()
            )
        }
    }
}