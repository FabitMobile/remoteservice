package ru.parking.error


data class RemoteServiceError
@JvmOverloads constructor(
    val errorCode: Int? = null,
    val detailMessage: String,
    val code: String? = null,
    val errorName: String? = null
) : Exception(detailMessage)
