package com.example.expensetracker.data.remote

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

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
        val remark: String
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
                                remark = item.optString("remark", "")
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
}
