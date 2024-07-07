package com.phhmaa

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class FakeReturnValue(
    val value: String,
)
