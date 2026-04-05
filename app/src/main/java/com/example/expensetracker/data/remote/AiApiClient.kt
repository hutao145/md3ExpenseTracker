package com.example.expensetracker.data.remote

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

    fun buildBaseUrl(input: String): String {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty()) { "API 域名不能为空" }

        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        val normalized = withScheme
            .removeSuffix("/v1/chat/completions")
            .removeSuffix("/v1/models")
            .trimEnd('/')

        require(normalized.toHttpUrlOrNull() != null) { "API 域名格式无效" }
        return normalized
    }

    fun buildChatCompletionsUrl(input: String): String {
        return "${buildBaseUrl(input)}/v1/chat/completions"
    }

    fun buildModelsUrl(input: String): String {
        return "${buildBaseUrl(input)}/v1/models"
    }

    fun analyze(baseUrl: String, apiKey: String, model: String, prompt: String): Result<String> {
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
                .url(buildChatCompletionsUrl(baseUrl))
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

    fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> {
        return try {
            val request = Request.Builder()
                .url(buildModelsUrl(baseUrl))
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
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

                val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
                val models = buildList {
                    for (index in 0 until data.length()) {
                        val id = data.optJSONObject(index)?.optString("id").orEmpty().trim()
                        if (id.isNotEmpty()) add(id)
                    }
                }

                if (models.isEmpty()) return Result.failure(Exception("未获取到模型列表"))

                Result.success(sortModels(models))
            }
        } catch (e: Exception) {
            Log.e(TAG, "AI model fetch error: ${e.message}")
            Result.failure(e)
        }
    }

    fun testConnection(baseUrl: String, apiKey: String, model: String): Result<String> {
        return analyze(baseUrl = baseUrl, apiKey = apiKey, model = model, prompt = "你好")
    }

    fun parseNaturalLanguageExpense(
        baseUrl: String,
        apiKey: String,
        model: String,
        input: String,
        assetNames: List<String>
    ): Result<String> {
        val assetHints = if (assetNames.isEmpty()) "当前没有可关联资产。" else "当前可关联资产：${assetNames.joinToString("、")}"
        val prompt = """
            你是记账解析助手。请把用户输入的自然语言记账文本解析为严格 JSON，不要输出 Markdown，不要输出解释文字。

            要求：
            1. 输出必须是一个 JSON 对象。
            2. JSON 结构固定为：
            {
              "items": [
                {
                  "amount": 32.5,
                  "type": "expense" | "income",
                  "category": "餐饮",
                  "note": "午饭",
                  "date": "2026-04-05",
                  "assetName": "招商银行卡",
                  "confidenceReason": "金额和分类来自明确文本"
                }
              ]
            }
            3. 支持把一段文本拆成多笔 items。
            4. 若无法确定字段，请填 null，不要编造。
            5. category 使用简体中文短词；note 尽量简短自然。
            6. date 使用 yyyy-MM-dd。

            $assetHints

            用户输入：$input
        """.trimIndent()
        return analyze(baseUrl = baseUrl, apiKey = apiKey, model = model, prompt = prompt)
    }

    private fun sortModels(models: List<String>): List<String> {
        val uniqueModels = models.distinct()
        val prioritized = uniqueModels.filter(::isPreferredChatModel)
        val fallback = uniqueModels.sorted()
        return if (prioritized.isNotEmpty()) {
            prioritized.sortedWith(compareBy<String> { chatModelPriority(it) }.thenBy { it.lowercase() })
        } else {
            fallback
        }
    }

    private fun isPreferredChatModel(model: String): Boolean {
        val normalized = model.lowercase()
        val excludedKeywords = listOf(
            "embedding", "moderation", "whisper", "tts", "speech", "transcribe",
            "transcription", "image", "vision-preview", "rerank", "ranker"
        )
        if (excludedKeywords.any { normalized.contains(it) }) return false

        val preferredKeywords = listOf(
            "gpt", "o1", "o3", "o4", "deepseek", "qwen", "glm", "claude", "gemini", "llama", "mistral"
        )
        return preferredKeywords.any { normalized.contains(it) }
    }

    private fun chatModelPriority(model: String): Int {
        val normalized = model.lowercase()
        return when {
            normalized.contains("gpt-4") || normalized.contains("gpt-4o") || normalized.contains("o1") || normalized.contains("o3") || normalized.contains("o4") -> 0
            normalized.contains("deepseek") || normalized.contains("claude") || normalized.contains("gemini") -> 1
            normalized.contains("qwen") || normalized.contains("glm") || normalized.contains("llama") || normalized.contains("mistral") -> 2
            else -> 3
        }
    }
}
