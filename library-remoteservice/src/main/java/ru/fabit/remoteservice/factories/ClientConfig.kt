package ru.fabit.remoteservice.factories

import okhttp3.Authenticator

data class ClientConfig(
    val defaultHeaders: Map<String, String>,
    val isLogEnabled: Boolean,
    val connectTimeoutMillis: Long,
    val readTimeoutMillis: Long,
    val authenticator: Authenticator
)