package ru.parking.remoteservice

import com.android.volley.Response
import org.json.JSONObject

class ResponseWithoutCacheFactory {
    companion object {
        @JvmStatic
        fun get(jsonObject: JSONObject): Response<JSONObject> = Response.success(jsonObject, null)
    }
}