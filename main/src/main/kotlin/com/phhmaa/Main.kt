package com.phhmaa

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

interface AbstractRepository {
    fun initA()
}

@GenerateFake
internal interface OrderRepository : AbstractRepository {
    val defaultOrder: Order
    val aUnit: Unit
    val aPrimitive: Char

    fun createOrder(order: Order)

    @FakeReturnValue("Order(\"\", \"\", 123.0, 0, OrderDetails(0L, \"\"))")
    fun getOrder(id: String): Order
    fun deleteOrder(id: String)
    fun updateOrder(id: String, order: Order): Int?
}

//@GenerateFake
//internal interface OrderRepositoryDescendant : OrderRepository

@GenerateFake
interface A

fun main() {
}