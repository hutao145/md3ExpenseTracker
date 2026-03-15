package com.example.expensetracker.data.remote

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import com.example.expensetracker.data.local.AssetEntity

object AutoAccountingService {
    private const val TAG = "AutoAccountingService"
    private const val BASE_URL = "http://127.0.0.1:52045"

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    data class SyncBill(
        val id: Long,
        val typeId: Int,
        val money: Double,
        val time: Long,
        val cateName: String,
        val shopName: String,
        val shopItem: String,
        val remark: String,
        val accountName: String
    )

    fun fetchUnsyncedBills(): List<SyncBill> {
        val resultList = mutableListOf<SyncBill>()
        try {
            val request = Request.Builder()
                .url("$BASE_URL/bill/sync/list")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP Code: ${response.code}")
                    return resultList
                }
                val body = response.body?.string() ?: return resultList
                val jsonObject = JSONObject(body)
                if (jsonObject.getInt("code") == 200) {
                    val dataArray = jsonObject.getJSONArray("data")
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        resultList.add(
                            SyncBill(
                                id = item.getLong("id"),
                                typeId = item.optString("type").let { parseBillTypeStringToId(it) },
                                money = item.getDouble("money"),
                                time = item.getLong("time"),
                                cateName = item.optString("cateName", ""),
                                shopName = item.optString("shopName", ""),
                                shopItem = item.optString("shopItem", ""),
                                remark = item.optString("remark", ""),
                                accountName = item.optString("accountNameFrom", "")
                                    .ifEmpty { item.optString("accountName", "") }
                                    .ifEmpty { item.optString("account", "") }
                                    .ifEmpty { item.optString("assetName", "") }
                                    .ifEmpty { item.optString("asset", "") }
                            )
                        )
                    }
                } else {
                    Log.e(TAG, "Fetch failed: ${jsonObject.getString("msg")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
        }
        return resultList
    }

    fun markAsSynced(id: Long): Boolean {
        try {
            val requestJson = JSONObject().apply {
                put("id", id)
                put("sync", true)
            }
            val request = Request.Builder()
                .url("$BASE_URL/bill/status")
                .post(requestJson.toString().toRequestBody(JSON_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body?.string() ?: return false
                return JSONObject(body).getInt("code") == 200
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mark sync Exception: ${e.message}")
        }
        return false
    }

    private fun parseBillTypeStringToId(typeStr: String): Int {
        return if (typeStr.contains("Income")) 1 else 0
    }

    fun syncAssets(assets: List<AssetEntity>): Boolean {
        try {
            val jsonArray = JSONArray()
            for (asset in assets) {
                val assetJson = JSONObject()
                assetJson.put("name", asset.name)
                assetJson.put("type", when (asset.type) {
                    0 -> "NORMAL"
                    1 -> "CREDIT"
                    2 -> "BORROWER"
                    else -> "NORMAL"
                })
                assetJson.put("currency", "CNY")
                assetJson.put("sort", 0)
                jsonArray.put(assetJson)
            }

            val jsonData = jsonArray.toString()
            val md5 = md5Digest(jsonData)

            val request = Request.Builder()
                .url("$BASE_URL/assets/put?md5=$md5")
                .post(jsonData.toRequestBody("text/json; charset=UTF-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Push assets HTTP Code: ${response.code}")
                    return false
                }
                val body = response.body?.string() ?: return false
                return JSONObject(body).getInt("code") == 200
            }
        } catch (e: Exception) {
            Log.e(TAG, "Push assets Exception: ${e.message}")
        }
        return false
    }

    private fun md5Digest(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
