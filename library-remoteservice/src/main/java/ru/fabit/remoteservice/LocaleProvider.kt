package ru.fabit.remoteservice

interface LocaleProvider {
    fun getSystemLocaleName(): String
}