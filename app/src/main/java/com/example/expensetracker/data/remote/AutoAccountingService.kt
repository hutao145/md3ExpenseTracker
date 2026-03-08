package com.example.expensetracker.data.remote

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import com.example.expensetracker.data.local.AssetEntity

object AutoAccountingService {
    private const val TAG = "AutoAccountingService"
    private const val BASE_URL = "http://127.0.0.1:52045"

    data class SyncBill(
        val id: Long,
        val typeId: Int, // 对应 BillType enum 序号 (0:支出, 4:收入 等)
        val money: Double,
        val time: Long,
        val cateName: String,
        val shopName: String,
        val shopItem: String,
        val remark: String,
        val accountName: String
    )

    /**
     * 拉取未同步的账单列表
     */
    fun fetchUnsyncedBills(): List<SyncBill> {
        val resultList = mutableListOf<SyncBill>()
        try {
            val url = URL("$BASE_URL/bill/sync/list")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseContent = reader.readText()
                reader.close()

                val jsonObject = JSONObject(responseContent)
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
            } else {
                Log.e(TAG, "HTTP Code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
        }
        return resultList
    }

    /**
     * 标记指定账单已同步
     */
    fun markAsSynced(id: Long): Boolean {
        try {
            val url = URL("$BASE_URL/bill/status")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val requestJson = JSONObject().apply {
                put("id", id)
                put("sync", true)
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(requestJson.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseContent = reader.readText()
                reader.close()
                val jsonObject = JSONObject(responseContent)
                return jsonObject.getInt("code") == 200
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mark sync Exception: ${e.message}")
        }
        return false
    }

    /**
     * 将字符串枚举映射为我们的 0=支出，1=收入 等标志
     */
    private fun parseBillTypeStringToId(typeStr: String): Int {
        return if (typeStr.contains("Income")) {
            1 // 收入
        } else {
            0 // 支出
        }
    }

    /**
     * 将应用的资产列表变更为指定的 JSON 数组格式，并推送给自动记账服务。
     */
    fun syncAssets(assets: List<AssetEntity>): Boolean {
        try {
            val jsonArray = JSONArray()
            for (asset in assets) {
                val assetJson = JSONObject()
                assetJson.put("name", asset.name)
                val typeStr = when (asset.type) {
                    0 -> "NORMAL"
                    1 -> "CREDIT"
                    2 -> "BORROWER"
                    else -> "NORMAL"
                }
                assetJson.put("type", typeStr)
                assetJson.put("currency", "CNY")
                assetJson.put("sort", 0) // Default sort
                jsonArray.put(assetJson)
            }

            val jsonData = jsonArray.toString()
            val md5 = md5Digest(jsonData)

            val url = URL("$BASE_URL/assets/put?md5=$md5")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "text/json; charset=UTF-8")
            connection.doOutput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
            writer.write(jsonData)
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseContent = reader.readText()
                reader.close()
                val jsonObject = JSONObject(responseContent)
                return jsonObject.getInt("code") == 200
            } else {
                Log.e(TAG, "Push assets response HTTP Code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Push assets Exception: ${e.message}")
        }
        return false
    }

    private fun md5Digest(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val messageDigest = md.digest(input.toByteArray(Charsets.UTF_8))
            val hexString = StringBuilder()
            for (b in messageDigest) {
                val hex = Integer.toHexString(0xFF and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
