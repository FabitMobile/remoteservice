package ru.fabit.remoteservice

import com.android.volley.Response
import org.json.JSONArray
import org.json.JSONObject

class ResponseWithoutCacheFactory {
    companion object {
        @JvmStatic
        fun get(jsonObject: JSONObject): Response<JSONObject> = Response.success(jsonObject, null)

        @JvmStatic
        fun get(jsonArray: JSONArray): Response<JSONArray> = Response.success(jsonArray, null)
    }
}