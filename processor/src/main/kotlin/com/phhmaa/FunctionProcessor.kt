@file:Suppress("UnnecessaryVariable")

package com.phhmaa

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo


/*
Input:

data class Order(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int,
)

@GenerateFake
internal interface OrderRepository {
    fun createOrder(order: Order)
    fun getOrder(id: String): Order
    fun deleteOrder(id: String)

    @IgnoreFake
    fun updateOrder(id: String, order: Order): Int
}

--

Desired output:

class FakeOrderRepository(
    val fakeCreateOrder: (Order) -> Unit = {},
    val fakeGetOrder: (String) -> Order = { Order("1", "fake order", 0.0, 0) },
    val fakeDeleteOrder: (String) -> Unit = {},
) : OrderRepository {
    override fun createOrder(order: Order) = fakeCreateOrder(order)
    override fun getOrder(id: String): Order = fakeGetOrder(id)
    override fun deleteOrder(id: String) = fakeDeleteOrder(id)
}
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
        val className = symbol.toClassName().simpleName
        val generatedClassName = "Fake${className}"

        val functions = symbol.declarations.filterIsInstance<KSFunctionDeclaration>()
            .filter { it.getVisibility() != Visibility.PRIVATE }
            // ignore com.phhmaa.IgnoreFake annotations:
            .filter { it.annotations.none { annotation -> annotation.shortName.asString() == "IgnoreFake" } }
            .asIterable()

        val generatedFunctions = functions.map { function -> generateFunction(function) }

        val classBuilder = TypeSpec.classBuilder(generatedClassName)
            .addOverride(symbol)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(
                        functions.map { function ->
                            ParameterSpec.builder(
                                name = "${function.simpleName.getShortName()}Fake",
                                type = function.returnType!!.toTypeName(),
                            ).defaultValue(function.toDefaultParameter()).build()
                        }
                    )
                    .build()
            )
            .addModifiers(symbol.modifiers.mapNotNull { it.toKModifier() })
            .addFunctions(generatedFunctions)
            .addProperties(functions.toPropSpec())

        val fileSpec = FileSpec.builder(packageName, generatedClassName)
            .addType(classBuilder.build())
            .build()

        fileSpec.writeTo(codeGenerator, dependencies = Dependencies(false, symbol.containingFile!!))
    }

    private fun Iterable<KSFunctionDeclaration>.toPropSpec(): List<PropertySpec> = map { function ->
        PropertySpec
            .builder(
                name = "${function.simpleName.getShortName()}Fake",
                type = function.returnType!!.toTypeName(),
            )
            .initializer("${function.simpleName.getShortName()}Fake")
            .addModifiers(KModifier.PRIVATE)
            .build()
    }

    private fun generateFunction(function: KSFunctionDeclaration): FunSpec {
        val funSpecBuilder = FunSpec.builder(function.simpleName.asString())
            .addParameters(
                function.parameters.map { parameter ->
                    ParameterSpec.builder(parameter.name!!.asString(), parameter.type.toTypeName()).build()
                }
            )
            .addModifiers(KModifier.OVERRIDE)
        return funSpecBuilder.build()
    }

    private fun TypeSpec.Builder.addOverride(from: KSClassDeclaration): TypeSpec.Builder {
        if (from.classKind == ClassKind.INTERFACE) {
            addSuperinterface(from.toClassName())
        } else {
            superclass(from.toClassName())
        }
        return this
    }

    private fun KSFunctionDeclaration.toDefaultParameter(): CodeBlock {
        val type = returnType?.resolve()
        return when {
            type == null -> throw IllegalStateException("Return type of function $simpleName is null")
            type.isFunctionType -> CodeBlock.of("{}")
            type.isMarkedNullable -> CodeBlock.of("null")
            else -> CodeBlock.of("{}")
        }
    }
}