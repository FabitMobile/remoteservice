package ru.fabit.remoteservice

object HeaderName {

    // Используется для указания клиенту OkHttp о необходимости хранить кэш запросов,
    // возвращает кэш вместо повторного запроса в течении указанного времени в секундах
    // или использует значение по умолчанию 60с
    const val CACHE_CONTROL_SECONDS = "cacheControlSeconds"
}