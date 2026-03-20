package com.example.expensetracker.data.remote

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiApiClient {
    private const val TAG = "AiApiClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun analyze(endpoint: String, apiKey: String, model: String, prompt: String): Result<String> {
        return try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody(JSON_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(body ?: "").optJSONObject("error")?.optString("message")
                            ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: ${body?.take(200)}"
                    }
                    return Result.failure(Exception(errorMsg))
                }

                if (body == null) return Result.failure(Exception("Empty response"))

                val json = JSONObject(body)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                Result.success(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI API error: ${e.message}")
            Result.failure(e)
        }
    }
}
