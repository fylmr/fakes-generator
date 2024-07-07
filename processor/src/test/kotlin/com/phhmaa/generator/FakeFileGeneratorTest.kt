package com.phhmaa.generator

import com.phhmaa.GenerateFakeProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class FakeFileGeneratorTest {

    @Test
    fun a() {
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

        // Check constructor
        assertEquals(1, clz.constructors.size)

        val constructor = clz.constructors.first()
        assertEquals(4, constructor.parameters.size)

        val createOrderFake = constructor.parameters.find { it.name == "createOrderFake" }
        assertEquals("(order: Order) -> kotlin.Unit", createOrderFake!!.type.toString())

        val getOrderFake = constructor.parameters.find { it.name == "getOrderFake" }
        assertEquals("(id: kotlin.String) -> Order", getOrderFake!!.type.toString())

        val deleteOrderFake = constructor.parameters.find { it.name == "deleteOrderFake" }
        assertEquals("(id: kotlin.String) -> kotlin.Unit", deleteOrderFake!!.type.toString())

        val updateOrderFake = constructor.parameters.find { it.name == "updateOrderFake" }
        assertEquals("(id: kotlin.String, order: Order) -> kotlin.Int?", updateOrderFake!!.type.toString())

        // Check functions
        val createOrder = clz.members.find { it.name == "createOrder" }
        assertEquals("kotlin.Unit", createOrder?.returnType.toString())

        val getOrder = clz.members.find { it.name == "getOrder" }
        assertEquals("Order", getOrder?.returnType.toString())

        val deleteOrder = clz.members.find { it.name == "deleteOrder" }
        assertEquals("kotlin.Unit", deleteOrder?.returnType.toString())

        val updateOrder = clz.members.find { it.name == "updateOrder" }
        assertEquals("kotlin.Int?", updateOrder?.returnType.toString())
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