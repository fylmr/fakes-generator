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

@GenerateFake
internal interface OrderRepository {
    fun createOrder(order: Order)
    fun getOrder(id: String): Order
    fun deleteOrder(id: String)
    fun updateOrder(id: String, order: Order): Int?
}

fun main() {
}