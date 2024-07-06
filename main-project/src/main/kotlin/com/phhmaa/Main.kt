package com.phhmaa

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

fun main() {
}