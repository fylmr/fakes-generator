package com.phhmaa.generator

import com.phhmaa.GenerateFakeProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class FakeFileGeneratorTest {

    @Test
    fun `OrderRepository is generated`() {
        val kotlinSource = SourceFile.kotlin(
            "OrderRepository.kt",
            """       
            import com.phhmaa.GenerateFake
            import com.phhmaa.FakeReturnValue

            data class OrderDetails(
                val authorId: Long,
                val comment: String,
            )
            
            data class Order(
                val id: String,
                val name: String,
                val price: Double,
                val quantity: Int,
                val details: OrderDetails,
            )
            
            @GenerateFake
            internal interface OrderRepository {
                fun createOrder(order: Order)
            
                @FakeReturnValue("Order(\"\", \"\", 123.0, 0, OrderDetails(0L, \"\"))")
                fun getOrder(id: String): Order
                fun deleteOrder(id: String)
                fun updateOrder(id: String, order: Order): Int?
            }
            """
        )

        val compileSourcesResult = compileSources(kotlinSource)

        val clz = compileSourcesResult.classLoader.loadClass("FakeOrderRepository").kotlin
        val instance = clz.createInstance() as Any

        // Check constructor
        assertEquals(1, clz.constructors.size)

        val constructor = clz.primaryConstructor!!
        assertEquals(4, constructor.parameters.size)

        val createOrderFake = constructor.parameters.first { it.name == "createOrderFake" }
        assertEquals("(order: Order) -> kotlin.Unit", createOrderFake.type.toString())
        assertTrue { createOrderFake.isOptional }

        val getOrderFake = constructor.parameters.first { it.name == "getOrderFake" }
        assertEquals("(id: kotlin.String) -> Order", getOrderFake.type.toString())
        assertTrue { getOrderFake.isOptional }

        val deleteOrderFake = constructor.parameters.first { it.name == "deleteOrderFake" }
        assertEquals("(id: kotlin.String) -> kotlin.Unit", deleteOrderFake.type.toString())
        assertTrue { deleteOrderFake.isOptional }

        val updateOrderFake = constructor.parameters.first { it.name == "updateOrderFake" }
        assertEquals("(id: kotlin.String, order: Order) -> kotlin.Int?", updateOrderFake.type.toString())
        assertTrue { updateOrderFake.isOptional }

        // Check functions
        val createOrder = clz.members.first { it.name == "createOrder" }
        assertEquals("kotlin.Unit", createOrder.returnType.toString())

        val getOrder = clz.members.first { it.name == "getOrder" }
        assertEquals("Order", getOrder.returnType.toString())

        val deleteOrder = clz.members.first { it.name == "deleteOrder" }
        assertEquals("kotlin.Unit", deleteOrder.returnType.toString())

        val updateOrder = clz.members.first { it.name == "updateOrder" }
        assertEquals("kotlin.Int?", updateOrder.returnType.toString())
    }

    private fun compileSources(kotlinSource: SourceFile): KotlinCompilation.Result {
        val generateKspSourcesCompilation = KotlinCompilation().apply {
            sources = listOf(kotlinSource)
            symbolProcessorProviders = listOf(GenerateFakeProcessorProvider())
            messageOutputStream = System.out
            inheritClassPath = true
        }
        val result = generateKspSourcesCompilation.compile()
        Assertions.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val kspGeneratedSourceFiles = result.kspGeneratedSourceFiles()

        val compileKspSourcesCompilation = KotlinCompilation().apply {
            sources = kspGeneratedSourceFiles + generateKspSourcesCompilation.sources
            inheritClassPath = true
            messageOutputStream = System.out
        }
        val compileSourcesResult = compileKspSourcesCompilation.compile()
        assertEquals(
            KotlinCompilation.ExitCode.OK,
            compileSourcesResult.exitCode,
            message = "Compile KSP sources failed.",
        )
        return compileSourcesResult
    }

    private fun KotlinCompilation.Result.kspGeneratedSourceFiles() =
        outputDirectory.parentFile.resolve("ksp/sources").walk().filter { it.isFile }.toList()
            .map { SourceFile.fromPath(it.absoluteFile) }

}