package com.example.expensetracker.ui.screen

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.ExpenseTrackerApplication
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.local.AiAnalysisEntity
import com.example.expensetracker.data.local.ExpenseDatabase
import com.example.expensetracker.data.remote.AiApiClient
import com.example.expensetracker.security.BiometricHelper
import com.example.expensetracker.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AiAnalysisActivity : AppCompatActivity() {

    private val database by lazy { ExpenseDatabase.getInstance(applicationContext) }
    private val repository by lazy {
        ExpenseRepository(database.expenseDao(), database.assetDao(), database)
    }
    private val aiAnalysisDao by lazy { database.aiAnalysisDao() }
    private var webViewRef: WebView? = null
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var prefs: SharedPreferences

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("ExpenseAppPrefs", MODE_PRIVATE)
        viewModel = ViewModelProvider(
            this,
            ExpenseViewModel.factory(repository, prefs)
        )[ExpenseViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val analyses by aiAnalysisDao.observeAll().collectAsState(initial = emptyList())

            // null = list view, non-null = detail view showing this entity
            var detailItem by remember { mutableStateOf<AiAnalysisEntity?>(null) }
            var showRangeDialog by remember { mutableStateOf(false) }
            var showDatePicker by remember { mutableStateOf(false) }
            var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }

            val isDark = when (uiState.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            ExpenseTrackerTheme(
                dynamicColor = uiState.dynamicColorEnabled,
                themeColor = uiState.themeColor,
                amoledDarkModeEnabled = uiState.amoledDarkModeEnabled,
                themeMode = uiState.themeMode
            ) {
                val appLockManager = (application as ExpenseTrackerApplication).appLockManager
                val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        if (detailItem != null) "${detailItem!!.startDate} ~ ${detailItem!!.endDate}"
                                        else "AI 财务分析"
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        if (detailItem != null) {
                                            webViewRef?.destroy()
                                            webViewRef = null
                                            detailItem = null
                                        } else {
                                            finish()
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        },
                        floatingActionButton = {
                            if (detailItem == null) {
                                FloatingActionButton(onClick = { showRangeDialog = true }) {
                                    Icon(Icons.Default.Add, "新建分析")
                                }
                            }
                        }
                    ) { padding ->
                        Crossfade(
                            targetState = detailItem,
                            modifier = Modifier.padding(padding),
                            label = "detail"
                        ) { item ->
                            if (item == null) {
                                // History list
                                if (analyses.isEmpty()) {
                                    EmptyState()
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp, vertical = 8.dp
                                        )
                                    ) {
                                        items(analyses, key = { it.id }) { record ->
                                            AnalysisCard(
                                                item = record,
                                                onClick = {
                                                    if (record.status == AiAnalysisEntity.STATUS_COMPLETED) {
                                                        detailItem = record
                                                    } else if (record.status == AiAnalysisEntity.STATUS_FAILED) {
                                                        // Retry: re-trigger analysis
                                                        startBackgroundAnalysis(
                                                            record.id,
                                                            LocalDate.parse(record.startDate),
                                                            LocalDate.parse(record.endDate)
                                                        )
                                                    }
                                                },
                                                onDelete = { showDeleteConfirm = record.id }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Detail WebView
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { context ->
                                        WebView(context).apply {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(
                                                    view: WebView, url: String?
                                                ) {
                                                    view.evaluateJavascript(
                                                        "setDarkMode($isDark)", null
                                                    )
                                                    injectReport(
                                                        view, item.aiResponseJson,
                                                        item.rawDataJson, isDark
                                                    )
                                                }
                                            }
                                            webViewRef = this
                                            loadUrl("file:///android_asset/ai_report.html")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Back handler
                    BackHandler(enabled = detailItem != null) {
                        webViewRef?.destroy()
                        webViewRef = null
                        detailItem = null
                    }

                    // Lock screen overlay
                    if (isLocked) {
                        LockScreen(
                            sharedPreferences = prefs,
                            onUnlocked = { appLockManager.unlock() },
                            onBiometricClick = {
                                BiometricHelper.authenticate(
                                    activity = this@AiAnalysisActivity,
                                    onSuccess = { appLockManager.unlock() },
                                    onFailure = {}
                                )
                            },
                            biometricEnabled = uiState.biometricUnlockEnabled
                                    && BiometricHelper.canAuthenticate(this@AiAnalysisActivity)
                        )
                    }
                }

                // Range selection dialog
                if (showRangeDialog) {
                    val presetOptions = remember {
                        listOf(
                            "lastWeek" to "近一周",
                            "last30Days" to "近30天",
                            "thisMonth" to "这个月",
                            "lastYear" to "最近一年",
                            "thisYear" to "今年"
                        )
                    }
                    AlertDialog(
                        onDismissRequest = { showRangeDialog = false },
                        title = { Text("选择分析范围") },
                        text = {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                presetOptions.forEach { (key, label) ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            showRangeDialog = false
                                            val (start, end) = computeDateRange(key)
                                            startBackgroundAnalysis(null, start, end)
                                        },
                                        label = { Text(label) }
                                    )
                                }
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        showRangeDialog = false
                                        showDatePicker = true
                                    },
                                    label = { Text("自定义范围") }
                                )
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showRangeDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }

                // Date range picker dialog
                if (showDatePicker) {
                    val dateRangeState = rememberDateRangePickerState()
                    Dialog(
                        onDismissRequest = { showDatePicker = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("取消")
                                    }
                                    Text(
                                        "选择分析范围",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    TextButton(
                                        onClick = {
                                            val startMs =
                                                dateRangeState.selectedStartDateMillis
                                            val endMs =
                                                dateRangeState.selectedEndDateMillis
                                            if (startMs != null && endMs != null) {
                                                val zone = ZoneId.systemDefault()
                                                val start = Instant.ofEpochMilli(startMs)
                                                    .atZone(zone).toLocalDate()
                                                val end = Instant.ofEpochMilli(endMs)
                                                    .atZone(zone).toLocalDate()
                                                showDatePicker = false
                                                startBackgroundAnalysis(null, start, end)
                                            }
                                        },
                                        enabled = dateRangeState.selectedStartDateMillis != null
                                                && dateRangeState.selectedEndDateMillis != null
                                    ) {
                                        Text("确定")
                                    }
                                }
                                DateRangePicker(
                                    state = dateRangeState,
                                    modifier = Modifier.weight(1f),
                                    title = null,
                                    showModeToggle = true
                                )
                            }
                        }
                    }
                }

                // Delete confirmation dialog
                showDeleteConfirm?.let { deleteId ->
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = null },
                        title = { Text("删除分析记录") },
                        text = { Text("确定删除这条分析记录吗?") },
                        confirmButton = {
                            TextButton(onClick = {
                                lifecycleScope.launch {
                                    aiAnalysisDao.deleteById(deleteId)
                                }
                                showDeleteConfirm = null
                            }) {
                                Text("删除")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = null }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * Start analysis in background. If existingId is provided, update that record;
     * otherwise insert a new placeholder record first.
     */
    private fun startBackgroundAnalysis(
        existingId: Long?, startDate: LocalDate, endDate: LocalDate
    ) {
        val endpoint = prefs.getString(
            "ai_api_endpoint", "https://api.openai.com/v1/chat/completions"
        ) ?: "https://api.openai.com/v1/chat/completions"
        val apiKey = prefs.getString("ai_api_key", "") ?: ""
        val model = prefs.getString("ai_api_model", "gpt-4o-mini") ?: "gpt-4o-mini"

        if (apiKey.isBlank()) {
            Toast.makeText(this, "请先在设置中配置 AI API Key", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            // Insert or reset the record to analyzing state
            val recordId = if (existingId != null) {
                withContext(Dispatchers.IO) {
                    aiAnalysisDao.updateResult(
                        existingId, "", "", AiAnalysisEntity.STATUS_ANALYZING
                    )
                }
                existingId
            } else {
                withContext(Dispatchers.IO) {
                    aiAnalysisDao.insert(
                        AiAnalysisEntity(
                            startDate = startDate.toString(),
                            endDate = endDate.toString(),
                            rawDataJson = "",
                            aiResponseJson = "",
                            status = AiAnalysisEntity.STATUS_ANALYZING,
                            createdAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
            }

            // Run analysis in background
            try {
                val dataJson = withContext(Dispatchers.IO) {
                    viewModel.collectAiAnalysisData(startDate, endDate)
                }
                val prompt = buildPrompt(dataJson, startDate, endDate)
                val result = withContext(Dispatchers.IO) {
                    AiApiClient.analyze(endpoint, apiKey, model, prompt)
                }
                result.onSuccess { response ->
                    withContext(Dispatchers.IO) {
                        aiAnalysisDao.updateResult(
                            recordId, response, dataJson,
                            AiAnalysisEntity.STATUS_COMPLETED
                        )
                    }
                }
                result.onFailure { error ->
                    withContext(Dispatchers.IO) {
                        aiAnalysisDao.updateResult(
                            recordId, error.message ?: "API 调用失败", "",
                            AiAnalysisEntity.STATUS_FAILED
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContext(Dispatchers.IO) {
                    aiAnalysisDao.updateResult(
                        recordId, e.message ?: "分析过程出错", "",
                        AiAnalysisEntity.STATUS_FAILED
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyState(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Analytics,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "还没有分析记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "点击右下角 + 开始分析",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }

    @Composable
    private fun AnalysisCard(
        item: AiAnalysisEntity,
        onClick: () -> Unit,
        onDelete: () -> Unit
    ) {
        val createdTime = remember(item.createdAtEpochMillis) {
            val instant = Instant.ofEpochMilli(item.createdAtEpochMillis)
            val dt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
            dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }
        val summary = remember(item.aiResponseJson, item.status) {
            when (item.status) {
                AiAnalysisEntity.STATUS_ANALYZING -> "分析中..."
                AiAnalysisEntity.STATUS_FAILED -> "分析失败: ${item.aiResponseJson}"
                else -> extractSummary(item.aiResponseJson)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${item.startDate} ~ ${item.endDate}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.status == AiAnalysisEntity.STATUS_ANALYZING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.size(6.dp))
                        } else if (item.status == AiAnalysisEntity.STATUS_FAILED) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "重试",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.size(6.dp))
                        }
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (item.status == AiAnalysisEntity.STATUS_FAILED)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        createdTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    private fun extractSummary(aiResponseJson: String): String {
        return try {
            val regex = """"summary"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
            regex.find(aiResponseJson)?.groupValues?.get(1)
                ?.replace("\\n", " ")?.replace("\\\"", "\"") ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun computeDateRange(key: String): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (key) {
            "lastWeek" -> now.minusDays(6) to now
            "last30Days" -> now.minusDays(29) to now
            "thisMonth" -> now.withDayOfMonth(1) to now
            "lastYear" -> now.minusMonths(11).withDayOfMonth(1) to now
            "thisYear" -> LocalDate.of(now.year, 1, 1) to now
            else -> now.withDayOfMonth(1) to now
        }
    }

    private fun buildPrompt(dataJson: String, startDate: LocalDate, endDate: LocalDate): String {
        return """你是一个专业的财务分析师。请基于以下用户记账数据生成一份全景透视报告。

## 分析范围
${startDate} 至 ${endDate}

## 数据
$dataJson

## 请严格返回以下 JSON 格式（不要包含其他文字，不要用 markdown 代码块包裹）：
{
  "summary": "2-3句话概括该时段收支情况",
  "healthScore": 0到100的整数（综合评估财务健康程度），
  "categoryAnalysis": [
    { "name": "分类名", "comment": "该分类的消费分析说明，占比是否合理等" }
  ],
  "structureHealth": ["消费结构健康度分析要点1", "要点2"],
  "anomalies": [
    { "title": "异常标题", "detail": "详细说明", "level": "high/medium/low" }
  ],
  "behaviorPatterns": ["消费偏好与行为规律1", "规律2", "规律3"],
  "insights": ["深度财务洞察1", "洞察2", "洞察3"],
  "predictions": {
    "nextMonthExpense": 预计下月支出数字,
    "nextMonthIncome": 预计下月收入数字,
    "trend": "up/down/stable",
    "detail": "趋势分析说明"
  },
  "conclusion": {
    "grade": "健康等级如A/B/C/D",
    "points": ["综合结论要点1", "要点2"]
  },
  "actionPlan": [
    { "title": "行动项标题", "detail": "具体描述", "priority": "high/medium/low" }
  ]
}

## 分析要求
- healthScore 根据储蓄率、消费结构合理性、异常消费等综合评分
- categoryAnalysis 必须覆盖数据中出现的每个消费分类
- anomalies 识别异常大额消费、占比过高的分类等
- behaviorPatterns 分析消费习惯、时间规律、偏好等
- actionPlan 给出3-5条具体可执行的改善建议，按优先级排列"""
    }

    private fun toBase64(text: String): String {
        return Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun injectReport(
        webView: WebView, aiResponse: String, rawDataJson: String, isDark: Boolean
    ) {
        val responseB64 = toBase64(aiResponse)
        val dataB64 = toBase64(rawDataJson)
        webView.evaluateJavascript(
            "setDarkMode($isDark);renderReport(decodeBase64('$responseB64'),decodeBase64('$dataB64'))",
            null
        )
    }

    override fun onDestroy() {
        webViewRef?.destroy()
        webViewRef = null
        super.onDestroy()
    }
}
