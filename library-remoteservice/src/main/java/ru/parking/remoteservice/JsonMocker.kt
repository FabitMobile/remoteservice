package ru.parking.remoteservice

import android.content.Context
import java.util.*

class JsonMocker constructor(private val context: Context) {

    companion object {
        const val GATED_PARKING_COST = "gatedparkings/getcost"
        const val LEGAL_ENTITY_DRIVERS1 = "legal-entity-drivers/100"
        const val LEGAL_ENTITY_DRIVERS2 = "legal-entity-drivers/200"
        const val LEGAL_ENTITY_DRIVERS3 = "legal-entity-drivers/300"
        const val ACCOUNTS = "accounts/me"
        const val RESERVATIONS = "me/reservations"
        const val RESERVATION_COST = "accounts/me/reservations/getcost"
        const val PAYMENTS = "accounts/me/payments"
    }

    fun getMockJson(
        relativePath: String,
        params: HashMap<String, Any>
    ): String? {
        return if (relativePath.contains(LEGAL_ENTITY_DRIVERS1)
            && relativePath.contains("vehicles")
        ) {
            getJson("legal-entity-drivers-vehicles.json")
        } else if (relativePath.contains(GATED_PARKING_COST)) {
            getJson("gated-parking-cost.json")
        } else if (relativePath.endsWith(ACCOUNTS)) {
            getJson("account.json")
        } else if (relativePath.endsWith(RESERVATIONS)) {
            getJson("reservations.json")
        } else if (relativePath.contains(LEGAL_ENTITY_DRIVERS1)) {
            getJson("legal-entity-drivers1.json")
        } else if (relativePath.contains(LEGAL_ENTITY_DRIVERS2)) {
            getJson("legal-entity-drivers2.json")
        } else if (relativePath.contains(LEGAL_ENTITY_DRIVERS3)) {
            getJson("legal-entity-drivers3.json")
        } else if (relativePath.contains(RESERVATION_COST)) {
            getJson("reservation-cost.json")
        } else if (relativePath.contains(PAYMENTS)) {
            getJson("legal-entity-payment-history.json")
        } else {
            null
        }
    }

    private fun getJson(jsonName: String): String? {
        val assetsPath = "json"
        return context.assets
            ?.list(assetsPath)
            ?.filter { it == jsonName }
            ?.map { path -> getJsonStringFromFile("$assetsPath/$path") }
            ?.first { it.isNotEmpty() }
    }

    private fun getJsonStringFromFile(fileName: String): String =
        context.assets.open(fileName).bufferedReader().use { it.readText() }
}