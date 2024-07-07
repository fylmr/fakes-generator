@file:Suppress("UnnecessaryVariable")

package com.phhmaa

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.phhmaa.generator.FakeFileGenerator
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * This processor handles interfaces annotated with @GenerateFake.
 * It generates a fake class for each annotated interface. The fake class contains a fake implementation for each
 * function declared in the interface.
 *
 * Input:
 * ```kotlin
 * data class OrderDetails(
 *     val authorId: Long,
 *     val comment: String,
 * )
 *
 * data class Order(
 *     val id: String,
 *     val name: String,
 *     val price: Double,
 *     val quantity: Int,
 *     val details: OrderDetails,
 * )
 *
 * @GenerateFake
 * internal interface OrderRepository {
 *     fun createOrder(order: Order)
 *     fun getOrder(id: String): Order
 *     fun deleteOrder(id: String)
 *     fun updateOrder(id: String, order: Order): Int?
 * }
 * ```
 * Desired output:
 *
 * ```kotlin
 * internal class FakeOrderRepository(
 *   private val createOrderFake: (order: Order) -> Unit = { _ ->  },
 *   private val getOrderFake: (id: String) -> Order = { _ -> Order(id = "", name = "", price = 0.0,
 *       quantity = 0, details = OrderDetails(authorId = 0L, comment = "")) },
 *   private val deleteOrderFake: (id: String) -> Unit = { _ ->  },
 *   private val updateOrderFake: (id: String, order: Order) -> Int? = { _, _ -> null },
 * ) : OrderRepository {
 *   override fun createOrder(order: Order): Unit = createOrderFake(order)
 *
 *   override fun getOrder(id: String): Order = getOrderFake(id)
 *
 *   override fun deleteOrder(id: String): Unit = deleteOrderFake(id)
 *
 *   override fun updateOrder(id: String, order: Order): Int? = updateOrderFake(id, order)
 * }
 * ```
 */
class GenerateFakeProcessor(
    private val codeGenerator: CodeGenerator,
    private val fakeFileGenerator: FakeFileGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GenerateFake::class.qualifiedName!!)
        symbols.forEach { symbol ->
            if (symbol is KSClassDeclaration) {
                val fileSpec = fakeFileGenerator.generateFakeFile(symbol)
                fileSpec.writeTo(
                    codeGenerator = codeGenerator,
                    dependencies = Dependencies(false, symbol.containingFile!!),
                )
            }
        }
        return emptyList()
    }

}