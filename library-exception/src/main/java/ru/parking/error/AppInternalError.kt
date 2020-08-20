package ru.parking.error


data class AppInternalError
@JvmOverloads constructor(
    val detailMessage: String
) : Exception(detailMessage)
