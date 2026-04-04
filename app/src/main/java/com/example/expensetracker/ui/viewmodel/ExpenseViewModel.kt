package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.ui.model.CategorySummaryUiModel
import com.example.expensetracker.ui.model.DailyExpenseUiModel
import com.example.expensetracker.ui.model.ExpenseItemUiModel
import com.example.expensetracker.data.local.AssetEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.expensetracker.data.local.ExpenseEntity
import com.example.expensetracker.data.remote.AiApiClient
import com.example.expensetracker.ui.util.centToYuanString
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class ExpenseUiState(
    val dailySummaries: List<DailyExpenseUiModel> = emptyList(),
    val totalExpenseCent: Long = 0,
    val totalIncomeCent: Long = 0,
    val categorySummaries: List<CategorySummaryUiModel> = emptyList(),
    val todayExpenseCent: Long = 0,
    val todayIncomeCent: Long = 0,
    val thisMonthExpenseCent: Long = 0,
    val thisMonthIncomeCent: Long = 0,
    val monthlyBudgetCent: Long? = null,
    val currentMonthLabel: String = "",
    val isMonthMode: Boolean = false,
    val searchQuery: String = "",
    val startDateInput: String = "",
    val endDateInput: String = "",
    val dateRangeError: String? = null,
    val isDateRangeApplied: Boolean = false,
    val assets: List<AssetEntity> = emptyList(),
    val dynamicColorEnabled: Boolean = true,
    val themeColor: String = "Pink",
    val amoledDarkModeEnabled: Boolean = false,
    val themeMode: String = "system",
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalHours: Int = 24,
    val lastAutoBackupTime: Long = 0L,
    val assetPageEnabled: Boolean = true
)

data class DateRangeFilterState(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startDateInput: String = "",
    val endDateInput: String = "",
    val errorMessage: String? = null
)

data class WebDavState(
    val url: String = "https://data.cstcloud.cn/dav",
    val username: String = "",
    val password: String = "",
    val path: String = "md3_expense_backup", // now acts as a directory
    val isTesting: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isFetchingFiles: Boolean = false,
    val isDeletingFile: Boolean = false,
    val fileList: List<com.example.expensetracker.data.remote.WebDavFileItem> = emptyList(),
    val showRestoreDialog: Boolean = false,
    val message: String? = null
)

data class AiConfigState(
    val baseUrl: String = "https://api.openai.com",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val availableModels: List<String> = emptyList(),
    val isFetchingModels: Boolean = false,
    val isTestingAiConnection: Boolean = false,
    val aiTestMessage: String? = null,
    val isAiTestError: Boolean = false
)

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val defaultAiBaseUrl = "https://api.openai.com"

    private val _webDavState = MutableStateFlow(
        WebDavState(
            url = sharedPreferences.getString("webdav_url", "https://data.cstcloud.cn/dav") ?: "https://data.cstcloud.cn/dav",
            username = sharedPreferences.getString("webdav_username", "") ?: "",
            password = sharedPreferences.getString("webdav_password", "") ?: "",
            path = sharedPreferences.getString("webdav_path", "md3_expense_backup") ?: "md3_expense_backup"
        )
    )
    val webDavState: StateFlow<WebDavState> = _webDavState.asStateFlow()
    private val webDavClient = com.example.expensetracker.data.remote.WebDavClient()

    private val aiBaseUrlState = MutableStateFlow(loadAiBaseUrl())
    private val aiApiKeyState = MutableStateFlow(sharedPreferences.getString("ai_api_key", "") ?: "")
    private val aiModelState = MutableStateFlow(sharedPreferences.getString("ai_api_model", "gpt-4o-mini") ?: "gpt-4o-mini")
    private val aiAvailableModelsState = MutableStateFlow<List<String>>(emptyList())
    private val aiFetchingModelsState = MutableStateFlow(false)
    private val aiTestingConnectionState = MutableStateFlow(false)
    private val aiTestMessageState = MutableStateFlow<String?>(null)
    private val aiTestErrorState = MutableStateFlow(false)

    val aiConfigState: StateFlow<AiConfigState> = combine(
        combine(aiBaseUrlState, aiApiKeyState, aiModelState) { baseUrl, apiKey, model ->
            Triple(baseUrl, apiKey, model)
        },
        combine(aiAvailableModelsState, aiFetchingModelsState, aiTestingConnectionState) { availableModels, isFetchingModels, isTestingAiConnection ->
            Triple(availableModels, isFetchingModels, isTestingAiConnection)
        },
        combine(aiTestMessageState, aiTestErrorState) { aiTestMessage, isAiTestError ->
            Pair(aiTestMessage, isAiTestError)
        }
    ) { baseConfig, loadingConfig, messageConfig ->
        AiConfigState(
            baseUrl = baseConfig.first,
            apiKey = baseConfig.second,
            model = baseConfig.third,
            availableModels = loadingConfig.first,
            isFetchingModels = loadingConfig.second,
            isTestingAiConnection = loadingConfig.third,
            aiTestMessage = messageConfig.first,
            isAiTestError = messageConfig.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiConfigState(
            baseUrl = aiBaseUrlState.value,
            apiKey = aiApiKeyState.value,
            model = aiModelState.value
        )
    )

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val dateInputFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd", Locale.CHINA)
        .withResolverStyle(ResolverStyle.STRICT)

    val dateRangeFilterState = MutableStateFlow(DateRangeFilterState())
    private val monthlyBudgetCentState = MutableStateFlow<Long?>(null)
    private val searchQueryState = MutableStateFlow("")
    private val dynamicColorEnabledState = MutableStateFlow(sharedPreferences.getBoolean("theme_dynamic_color", true))
    private val themeColorState = MutableStateFlow(sharedPreferences.getString("theme_color_seed", "Pink") ?: "Pink")
    private val amoledDarkModeEnabledState = MutableStateFlow(sharedPreferences.getBoolean("theme_amoled_dark_mode", false))
    private val themeModeState = MutableStateFlow(sharedPreferences.getString("theme_mode", "system") ?: "system")
    private val appLockEnabledState = MutableStateFlow(sharedPreferences.getBoolean("app_lock_enabled", false))
    private val biometricUnlockEnabledState = MutableStateFlow(sharedPreferences.getBoolean("biometric_unlock_enabled", false))
    private val autoBackupEnabledState = MutableStateFlow(sharedPreferences.getBoolean("auto_backup_enabled", false))
    private val autoBackupIntervalHoursState = MutableStateFlow(sharedPreferences.getInt("auto_backup_interval_hours", 24))
    private val lastAutoBackupTimeState = MutableStateFlow(sharedPreferences.getLong("last_auto_backup_time", 0L))
    private val assetPageEnabledState = MutableStateFlow(sharedPreferences.getBoolean("asset_page_enabled", true))

    init {
        goToCurrentMonth()
    }

    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.observeExpenses(),
        repository.observeAssets(),
        dateRangeFilterState,
        monthlyBudgetCentState,
        combine(
            searchQueryState,
            dynamicColorEnabledState,
            themeColorState,
            combine(
                amoledDarkModeEnabledState,
                themeModeState,
                appLockEnabledState,
                biometricUnlockEnabledState
            ) { a, m, lock, bio ->
                data class Extra1(val a: Boolean, val m: String, val lock: Boolean, val bio: Boolean)
                Extra1(a, m, lock, bio)
            },
            combine(
                autoBackupEnabledState,
                autoBackupIntervalHoursState,
                lastAutoBackupTimeState,
                assetPageEnabledState
            ) { enabled, interval, lastTime, assetPage ->
                data class Extra2(val enabled: Boolean, val interval: Int, val lastTime: Long, val assetPage: Boolean)
                Extra2(enabled, interval, lastTime, assetPage)
            }
        ) { q, d, t, extra1, extra2 ->
            data class Config(
                val q: String, val d: Boolean, val t: String,
                val a: Boolean, val m: String, val lock: Boolean, val bio: Boolean,
                val autoBackup: Boolean, val autoInterval: Int, val lastAutoTime: Long,
                val assetPage: Boolean
            )
            Config(q, d, t, extra1.a, extra1.m, extra1.lock, extra1.bio, extra2.enabled, extra2.interval, extra2.lastTime, extra2.assetPage)
        }
    ) { expenses, assets, dateRangeFilter, monthlyBudgetCent, config ->
        val searchQuery = config.q
        val dynamicColorEnabled = config.d
        val themeColor = config.t
        val amoledDarkModeEnabled = config.a
        val themeMode = config.m
        val appLockEnabled = config.lock
        val biometricUnlockEnabled = config.bio
        val filteredByDate = expenses.filter { entity ->
            val expenseDate = Instant.ofEpochMilli(entity.createdAtEpochMillis)
                .atZone(zoneId)
                .toLocalDate()

            val afterStart = dateRangeFilter.startDate?.let { !expenseDate.isBefore(it) } ?: true
            val beforeEnd = dateRangeFilter.endDate?.let { !expenseDate.isAfter(it) } ?: true
            afterStart && beforeEnd
        }

        val normalizedQuery = searchQuery.trim()
        val filteredExpenses = if (normalizedQuery.isBlank()) {
            filteredByDate
        } else {
            val q = normalizedQuery.lowercase(Locale.getDefault())
            filteredByDate.filter { entity ->
                entity.category.lowercase(Locale.getDefault()).contains(q) ||
                    entity.note.lowercase(Locale.getDefault()).contains(q)
            }
        }

        val groupedByDate = filteredExpenses.groupBy { entity ->
            Instant.ofEpochMilli(entity.createdAtEpochMillis)
                .atZone(zoneId)
                .toLocalDate()
        }

        val today = LocalDate.now(zoneId)
        val todayEntities = expenses.filter { entity ->
            Instant.ofEpochMilli(entity.createdAtEpochMillis).atZone(zoneId).toLocalDate() == today
        }
        val todayExpenseCent = todayEntities.filter { it.type == 0 }.sumOf { it.amountCent }
        val todayIncomeCent = todayEntities.filter { it.type == 1 }.sumOf { it.amountCent }

        val thisMonthEntities = expenses.filter { entity ->
            val date = Instant.ofEpochMilli(entity.createdAtEpochMillis).atZone(zoneId).toLocalDate()
            date.year == today.year && date.month == today.month
        }
        val thisMonthExpenseCent = thisMonthEntities.filter { it.type == 0 }.sumOf { it.amountCent }
        val thisMonthIncomeCent = thisMonthEntities.filter { it.type == 1 }.sumOf { it.amountCent }

        val isMonthMode = dateRangeFilter.startDate != null &&
            dateRangeFilter.endDate != null &&
            dateRangeFilter.startDate.year == dateRangeFilter.endDate.year &&
            dateRangeFilter.startDate.month == dateRangeFilter.endDate.month &&
            dateRangeFilter.startDate.dayOfMonth == 1 &&
            dateRangeFilter.endDate.dayOfMonth == dateRangeFilter.endDate.lengthOfMonth()

        val baseMonth = when {
            isMonthMode -> dateRangeFilter.startDate!!
            else -> today.withDayOfMonth(1)
        }
        val currentMonthLabel = String.format(
            Locale.CHINA,
            "%d年%02d月",
            baseMonth.year,
            baseMonth.monthValue
        )

        val categorySummaries = filteredExpenses
            .filter { it.type == 0 } // Default category summary to Expenses only
            .groupBy { it.category.ifBlank { "其他" } }
            .map { (category, items) ->
                CategorySummaryUiModel(
                    category = category,
                    totalExpenseCent = items.sumOf { it.amountCent },
                    count = items.size
                )
            }
            .sortedByDescending { it.totalExpenseCent }

        ExpenseUiState(
            dailySummaries = groupedByDate
                .entries
                .sortedByDescending { it.key }
                .map { (localDate, entities) ->
                    DailyExpenseUiModel(
                        date = localDate.format(dateFormatter),
                        totalExpenseCent = entities.filter { it.type == 0 }.sumOf { it.amountCent },
                        totalIncomeCent = entities.filter { it.type == 1 }.sumOf { it.amountCent },
                        items = entities.map { item ->
                            ExpenseItemUiModel(
                                id = item.id,
                                amountCent = item.amountCent,
                                type = item.type,
                                category = item.category,
                                note = item.note,
                                assetId = item.assetId,
                                assetName = item.assetId?.let { assetId -> assets.find { it.id == assetId }?.name }
                            )
                        }
                    )
                },
            totalExpenseCent = filteredExpenses.filter { it.type == 0 }.sumOf { it.amountCent },
            totalIncomeCent = filteredExpenses.filter { it.type == 1 }.sumOf { it.amountCent },
            categorySummaries = categorySummaries,
            todayExpenseCent = todayExpenseCent,
            todayIncomeCent = todayIncomeCent,
            thisMonthExpenseCent = thisMonthExpenseCent,
            thisMonthIncomeCent = thisMonthIncomeCent,
            monthlyBudgetCent = monthlyBudgetCent,
            currentMonthLabel = currentMonthLabel,
            isMonthMode = isMonthMode,
            searchQuery = searchQuery,
            startDateInput = dateRangeFilter.startDateInput,
            endDateInput = dateRangeFilter.endDateInput,
            dateRangeError = dateRangeFilter.errorMessage,
            isDateRangeApplied = dateRangeFilter.startDate != null || dateRangeFilter.endDate != null,
            assets = assets,
            dynamicColorEnabled = dynamicColorEnabled,
            themeColor = themeColor,
            amoledDarkModeEnabled = amoledDarkModeEnabled,
            themeMode = themeMode,
            appLockEnabled = appLockEnabled,
            biometricUnlockEnabled = biometricUnlockEnabled,
            autoBackupEnabled = config.autoBackup,
            autoBackupIntervalHours = config.autoInterval,
            lastAutoBackupTime = config.lastAutoTime,
            assetPageEnabled = config.assetPage
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseUiState()
        )

    fun updateDynamicColor(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("theme_dynamic_color", enabled).apply()
        dynamicColorEnabledState.value = enabled
    }

    fun updateAiBaseUrl(value: String) {
        val normalized = value.trim().let {
            when {
                it.isBlank() -> ""
                else -> normalizeAiBaseUrl(it)
            }
        }
        sharedPreferences.edit().putString("ai_api_endpoint", normalized).apply()
        aiBaseUrlState.value = normalized
        clearAiTestMessage()
    }

    fun updateAiApiKey(value: String) {
        sharedPreferences.edit().putString("ai_api_key", value).apply()
        aiApiKeyState.value = value
        clearAiTestMessage()
    }

    fun updateAiModel(value: String) {
        sharedPreferences.edit().putString("ai_api_model", value).apply()
        aiModelState.value = value
        clearAiTestMessage()
    }

    fun fetchAiModels() {
        val baseUrl = aiBaseUrlState.value
        val apiKey = aiApiKeyState.value.trim()

        if (baseUrl.isBlank()) {
            setAiTestMessage("请先填写 API 域名 / 基础地址", true)
            return
        }
        if (apiKey.isBlank()) {
            setAiTestMessage("请先填写 API Key", true)
            return
        }

        viewModelScope.launch {
            aiFetchingModelsState.value = true
            clearAiTestMessage()

            val result = withContext(Dispatchers.IO) {
                AiApiClient.fetchModels(baseUrl = baseUrl, apiKey = apiKey)
            }

            result.onSuccess { models ->
                aiAvailableModelsState.value = models
                if (aiModelState.value.isBlank() && models.isNotEmpty()) {
                    updateAiModel(models.first())
                }
                setAiTestMessage("已拉取 ${models.size} 个模型", false)
            }.onFailure { error ->
                setAiTestMessage("模型拉取失败：${error.message ?: "未知错误"}", true)
            }

            aiFetchingModelsState.value = false
        }
    }

    fun testAiConnection() {
        val baseUrl = aiBaseUrlState.value
        val apiKey = aiApiKeyState.value.trim()
        val model = aiModelState.value.trim()

        if (baseUrl.isBlank()) {
            setAiTestMessage("请先填写 API 域名 / 基础地址", true)
            return
        }
        if (apiKey.isBlank()) {
            setAiTestMessage("请先填写 API Key", true)
            return
        }
        if (model.isBlank()) {
            setAiTestMessage("请先填写或选择模型名称", true)
            return
        }

        viewModelScope.launch {
            aiTestingConnectionState.value = true
            clearAiTestMessage()

            val result = withContext(Dispatchers.IO) {
                AiApiClient.testConnection(baseUrl = baseUrl, apiKey = apiKey, model = model)
            }

            result.onSuccess { reply ->
                val preview = reply.replace("\n", " ").trim().ifEmpty { "收到成功响应" }.take(120)
                setAiTestMessage("测试成功：$preview", false)
            }.onFailure { error ->
                setAiTestMessage("测试失败：${error.message ?: "未知错误"}", true)
            }

            aiTestingConnectionState.value = false
        }
    }

    fun clearAiTestMessage() {
        aiTestMessageState.value = null
        aiTestErrorState.value = false
    }

    fun updateThemeColor(color: String) {
        sharedPreferences.edit().putString("theme_color_seed", color).apply()
        themeColorState.value = color
    }
    
    fun updateAmoledDarkMode(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("theme_amoled_dark_mode", enabled).apply()
        amoledDarkModeEnabledState.value = enabled
    }

    fun updateThemeMode(mode: String) {
        sharedPreferences.edit().putString("theme_mode", mode).apply()
        themeModeState.value = mode
    }

    fun updateAppLockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("app_lock_enabled", enabled).apply()
        appLockEnabledState.value = enabled
    }

    fun updateBiometricUnlockEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_unlock_enabled", enabled).apply()
        biometricUnlockEnabledState.value = enabled
    }

    fun updateAutoBackupEnabled(context: Context, enabled: Boolean) {
        sharedPreferences.edit().putBoolean("auto_backup_enabled", enabled).apply()
        autoBackupEnabledState.value = enabled
        if (enabled) {
            com.example.expensetracker.backup.BackupScheduler.schedule(context, autoBackupIntervalHoursState.value)
        } else {
            com.example.expensetracker.backup.BackupScheduler.cancel(context)
        }
    }

    fun updateAutoBackupInterval(context: Context, hours: Int) {
        sharedPreferences.edit().putInt("auto_backup_interval_hours", hours).apply()
        autoBackupIntervalHoursState.value = hours
        if (autoBackupEnabledState.value) {
            com.example.expensetracker.backup.BackupScheduler.schedule(context, hours)
        }
    }

    fun refreshLastAutoBackupTime() {
        lastAutoBackupTimeState.value = sharedPreferences.getLong("last_auto_backup_time", 0L)
    }

    fun updateAssetPageEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("asset_page_enabled", enabled).apply()
        assetPageEnabledState.value = enabled
    }

    fun addExpense(amountInput: String, type: Int, category: String, note: String, assetId: Long?, dateMillis: Long) {
        val amountCent = parseAmountToCent(amountInput) ?: return

        viewModelScope.launch {
            repository.addExpense(
                amountCent = amountCent,
                type = type,
                category = category,
                note = note,
                assetId = assetId,
                dateMillis = dateMillis
            )
        }
    }

    fun updateExpense(id: Long, amountInput: String, type: Int, category: String, note: String, assetId: Long?) {
        val amountCent = parseAmountToCent(amountInput) ?: return

        viewModelScope.launch {
            repository.updateExpense(
                id = id,
                amountCent = amountCent,
                type = type,
                category = category,
                note = note,
                assetId = assetId
            )
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }

    fun deleteMultipleExpenses(ids: Set<Long>) {
        viewModelScope.launch {
            repository.deleteExpenses(ids)
        }
    }

    fun updateCategoriesForIds(ids: Set<Long>, category: String) {
        viewModelScope.launch {
            repository.updateCategories(ids, category)
        }
    }


    fun addAsset(nameInput: String, amountInput: String, type: Int, dateMillis: Long = System.currentTimeMillis()) {
        val amountCent = parseAmountToCent(amountInput) ?: return
        viewModelScope.launch {
            repository.addAsset(nameInput, amountCent, type, dateMillis)
        }
    }

    fun updateAsset(id: Long, nameInput: String, amountInput: String, type: Int) {
        val amountCent = parseAmountToCent(amountInput) ?: return
        viewModelScope.launch {
            repository.updateAsset(id, nameInput, amountCent, type)
        }
    }

    fun deleteAsset(id: Long) {
        viewModelScope.launch {
            repository.deleteAsset(id)
        }
    }

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun syncFromAutoAccounting() {
        viewModelScope.launch {
            // 在 IO 线程执行网路请求
            val currentAssets = uiState.value.assets
            val currentHash = currentAssets.hashCode().toString()
            val lastHash = sharedPreferences.getString("last_synced_assets_hash", "")
            
            if (currentHash != lastHash) {
                val pushSuccess = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.expensetracker.data.remote.AutoAccountingService.syncAssets(currentAssets)
                }
                if (pushSuccess) {
                    sharedPreferences.edit().putString("last_synced_assets_hash", currentHash).apply()
                }
            }

            val newBills = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.expensetracker.data.remote.AutoAccountingService.fetchUnsyncedBills()
            }

            if (newBills.isEmpty()) {
                _syncMessage.value = "没有需要同步的账单"
                return@launch
            }

            var successCount = 0
            for (bill in newBills) {
                // 将服务器金额 (元) 转换为我们应用存储的 (分)
                val amountCent = (bill.money * 100).toLong()
                
                // 拼接备注
                val note = buildString {
                    if (bill.shopName.isNotEmpty()) append(bill.shopName).append(" ")
                    if (bill.shopItem.isNotEmpty()) append(bill.shopItem).append(" ")
                    if (bill.remark.isNotEmpty()) append(bill.remark)
                }.trim()

                // 记录资产映射：优先取账单自带的 accountName，如果没有则进行模糊匹配
                var matchedAssetId: Long? = null
                val searchKeys = listOf(bill.accountName, bill.shopName, bill.remark).filter { it.isNotBlank() }
                
                for (asset in currentAssets) {
                    if (searchKeys.any { it.contains(asset.name, ignoreCase = true) }) {
                        matchedAssetId = asset.id
                        break
                    }
                }

                // 落库
                repository.addExpense(
                    amountCent = amountCent,
                    type = bill.typeId,
                    category = if (bill.cateName.isEmpty()) "其他" else bill.cateName,
                    note = note,
                    dateMillis = bill.time,
                    assetId = matchedAssetId
                )

                // 标记为已同步
                val marked = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.expensetracker.data.remote.AutoAccountingService.markAsSynced(bill.id)
                }
                if (marked) {
                    successCount++
                }
            }
            
            _syncMessage.value = "成功同步了 $successCount 笔新账单"
        }
    }

    fun applyDateRange(startDateInput: String, endDateInput: String) {
        val normalizedStart = startDateInput.trim()
        val normalizedEnd = endDateInput.trim()

        val parsedStart = parseDateInput(normalizedStart)
        val parsedEnd = parseDateInput(normalizedEnd)
        val current = dateRangeFilterState.value

        if (normalizedStart.isNotEmpty() && parsedStart == null) {
            dateRangeFilterState.value = current.copy(
                startDateInput = normalizedStart,
                endDateInput = normalizedEnd,
                errorMessage = "开始日期格式错误，请使用 yyyy-MM-dd"
            )
            return
        }

        if (normalizedEnd.isNotEmpty() && parsedEnd == null) {
            dateRangeFilterState.value = current.copy(
                startDateInput = normalizedStart,
                endDateInput = normalizedEnd,
                errorMessage = "结束日期格式错误，请使用 yyyy-MM-dd"
            )
            return
        }

        if (parsedStart != null && parsedEnd != null && parsedStart.isAfter(parsedEnd)) {
            dateRangeFilterState.value = current.copy(
                startDateInput = normalizedStart,
                endDateInput = normalizedEnd,
                errorMessage = "开始日期不能晚于结束日期"
            )
            return
        }

        dateRangeFilterState.value = DateRangeFilterState(
            startDate = parsedStart,
            endDate = parsedEnd,
            startDateInput = normalizedStart,
            endDateInput = normalizedEnd,
            errorMessage = null
        )
    }

    fun clearDateRange() {
        dateRangeFilterState.value = DateRangeFilterState()
    }

    fun goToPreviousMonth() {
        val current = dateRangeFilterState.value
        val base = if (
            current.startDate != null &&
            current.endDate != null &&
            current.startDate.year == current.endDate.year &&
            current.startDate.month == current.endDate.month &&
            current.startDate.dayOfMonth == 1 &&
            current.endDate.dayOfMonth == current.endDate.lengthOfMonth()
        ) {
            current.startDate
        } else {
            LocalDate.now(zoneId).withDayOfMonth(1)
        }

        val targetMonth = base.minusMonths(1)
        applyWholeMonth(targetMonth)
    }

    fun goToNextMonth() {
        val current = dateRangeFilterState.value
        val base = if (
            current.startDate != null &&
            current.endDate != null &&
            current.startDate.year == current.endDate.year &&
            current.startDate.month == current.endDate.month &&
            current.startDate.dayOfMonth == 1 &&
            current.endDate.dayOfMonth == current.endDate.lengthOfMonth()
        ) {
            current.startDate
        } else {
            LocalDate.now(zoneId).withDayOfMonth(1)
        }

        val targetMonth = base.plusMonths(1)
        applyWholeMonth(targetMonth)
    }

    fun goToCurrentMonth() {
        val currentMonthFirstDay = LocalDate.now(zoneId).withDayOfMonth(1)
        applyWholeMonth(currentMonthFirstDay)
    }

    private fun applyWholeMonth(firstDayOfMonth: LocalDate) {
        val start = firstDayOfMonth
        val end = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth())
        dateRangeFilterState.value = DateRangeFilterState(
            startDate = start,
            endDate = end,
            startDateInput = start.format(dateInputFormatter),
            endDateInput = end.format(dateInputFormatter),
            errorMessage = null
        )
    }

    fun setMonthlyBudget(amountInput: String) {
        val amountCent = parseAmountToCent(amountInput) ?: return
        monthlyBudgetCentState.value = amountCent
    }

    fun clearMonthlyBudget() {
        monthlyBudgetCentState.value = null
    }

    fun isAmountValid(amountInput: String): Boolean = parseAmountToCent(amountInput) != null

    fun updateSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun exportDataToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val expenses = repository.getAllExpensesSnapshot()
            val sb = StringBuilder()
            // Add UTF-8 BOM for Excel compatibility
            sb.append("\uFEFF")
            sb.append("日期,类型,分类,金额(元),备注\n")

            val csvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

            expenses.forEach { entity ->
                val date = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(entity.createdAtEpochMillis),
                    ZoneId.systemDefault()
                ).format(csvFormatter)

                val amount = centToYuanString(entity.amountCent)

                val typeStr = if (entity.type == 1) "收入" else "支出"

                // Escape quotes and commas in note
                val note = entity.note.replace("\"", "\"\"")
                val safeNote = if (note.contains(",") || note.contains("\n")) "\"$note\"" else note

                sb.append("$date,$typeStr,${entity.category},$amount,$safeNote\n")
            }

            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // TODO: Handle error state if needed
                }
            }
        }
    }

    fun importDataFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val expensesToInsert = mutableListOf<ExpenseEntity>()
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                            // Skip header (strip BOM if present)
                            reader.readLine()?.removePrefix("\uFEFF")

                            val csvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            var line = reader.readLine()
                            while (line != null) {
                                try {
                                    // Basic CSV parsing (handling simple quoted strings for Note)
                                    // Expected format: Date,Type,Category,Amount,Note
                                    // Note might be quoted if it contains comma or newline
                                    val parts = parseCsvLine(line)
                                    if (parts.size >= 4) {
                                        // Handle both old (4 columns) and new (5 columns) formats
                                        val hasTypeColumn = parts.size >= 5
                                        val dateStr = parts[0]
                                        val type = if (hasTypeColumn && parts[1] == "收入") 1 else 0
                                        val categoryIdx = if (hasTypeColumn) 2 else 1
                                        val amountIdx = if (hasTypeColumn) 3 else 2
                                        val noteIdx = if (hasTypeColumn) 4 else 3
                                        
                                        val category = parts[categoryIdx]
                                        val amountStr = parts[amountIdx]
                                        val note = parts.getOrNull(noteIdx) ?: ""

                                        val dateTime = LocalDateTime.parse(dateStr, csvFormatter)
                                        val dateMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                                        val amountCent = BigDecimal(amountStr)
                                            .multiply(BigDecimal(100))
                                            .setScale(0, RoundingMode.HALF_UP)
                                            .longValueExact()

                                        expensesToInsert.add(
                                            ExpenseEntity(
                                                amountCent = amountCent,
                                                type = type,
                                                category = category,
                                                note = note,
                                                createdAtEpochMillis = dateMillis
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Continue parsing other lines even if one fails
                                }
                                line = reader.readLine()
                            }
                        }
                    }

                    if (expensesToInsert.isNotEmpty()) {
                        repository.insertAllExpenses(expensesToInsert)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // TODO: Handle error
                }
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '\"' -> {
                    inQuotes = !inQuotes
                    sb.append(c)
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString())
        return tokens
            .map { it.trim() }
            .map { if (it.startsWith("\"") && it.endsWith("\"")) it.substring(1, it.length - 1) else it }
            .map { it.replace("\"\"", "\"") }
    }

    private fun parseAmountToCent(amountInput: String): Long? {
        val normalized = amountInput.trim()
        if (normalized.isEmpty()) {
            return null
        }

        return try {
            val amountYuan = normalized.toBigDecimal()
            if (amountYuan < BigDecimal.ZERO) {
                null
            } else {
                amountYuan
                    .multiply(BigDecimal(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDateInput(dateInput: String): LocalDate? {
        if (dateInput.isBlank()) {
            return null
        }

        return try {
            LocalDate.parse(dateInput, dateInputFormatter)
        } catch (_: Exception) {
            null
        }
    }

    fun dismissRestoreDialog() { _webDavState.value = _webDavState.value.copy(showRestoreDialog = false) }

    fun updateWebDavUrl(url: String) { 
        _webDavState.value = _webDavState.value.copy(url = url)
        sharedPreferences.edit().putString("webdav_url", url).apply()
    }
    fun updateWebDavUsername(username: String) { 
        _webDavState.value = _webDavState.value.copy(username = username)
        sharedPreferences.edit().putString("webdav_username", username).apply()
    }
    // TODO: Migrate to EncryptedSharedPreferences for secure credential storage
    fun updateWebDavPassword(password: String) {
        _webDavState.value = _webDavState.value.copy(password = password)
        sharedPreferences.edit().putString("webdav_password", password).apply()
    }
    fun updateWebDavPath(path: String) { 
        _webDavState.value = _webDavState.value.copy(path = path)
        sharedPreferences.edit().putString("webdav_path", path).apply()
    }
    fun clearWebDavMessage() { _webDavState.value = _webDavState.value.copy(message = null) }

    fun fetchWebDavFileList() {
        val state = _webDavState.value
        if (state.url.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _webDavState.value = state.copy(message = "请填写完整的配置信息")
            return
        }

        _webDavState.value = state.copy(isFetchingFiles = true, showRestoreDialog = true, message = null)
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val fullUrl = if (state.url.endsWith("/")) state.url + state.path else "${state.url}/${state.path}"
                // Add trailing slash if not present to ensure we list the directory properly
                val dirUrl = if (fullUrl.endsWith("/")) fullUrl else "$fullUrl/"
                webDavClient.listFiles(dirUrl, state.username, state.password)
            }
            _webDavState.value = _webDavState.value.copy(
                isFetchingFiles = false,
                fileList = list
            )
        }
    }

    fun deleteWebDavFile(fileName: String) {
        val state = _webDavState.value
        _webDavState.value = state.copy(isDeletingFile = true, message = null)
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                val fullUrl = if (state.url.endsWith("/")) state.url + state.path else "${state.url}/${state.path}"
                val dirUrl = if (fullUrl.endsWith("/")) fullUrl else "$fullUrl/"
                val fileUrl = dirUrl + fileName
                webDavClient.deleteFile(fileUrl, state.username, state.password)
            }
            if (success) {
                _webDavState.value = _webDavState.value.copy(isDeletingFile = false, message = "删除成功")
                fetchWebDavFileList() // refresh list
            } else {
                _webDavState.value = _webDavState.value.copy(isDeletingFile = false, message = "删除失败")
            }
        }
    }

    fun testWebDavConnection() {
        val state = _webDavState.value
        if (state.url.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _webDavState.value = state.copy(message = "请填写完整的配置信息")
            return
        }
        
        _webDavState.value = state.copy(isTesting = true, message = null)
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                webDavClient.testConnection(state.url, state.username, state.password)
            }
            _webDavState.value = _webDavState.value.copy(
                isTesting = false, 
                message = if (success) "测试连接成功" else "测试连接失败"
            )
        }
    }

    fun backupToWebDav(context: Context) {
        val state = _webDavState.value
        if (state.url.isBlank() || state.username.isBlank() || state.password.isBlank() || state.path.isBlank()) {
            _webDavState.value = state.copy(message = "请填写完整的配置信息")
            return
        }

        _webDavState.value = state.copy(isBackingUp = true, message = null)
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val file = java.io.File(context.cacheDir, "temp_backup.csv")
                    val expenses = repository.getAllExpensesSnapshot()
                    val sb = StringBuilder()
                    sb.append("\uFEFF")
                    sb.append("日期,类型,分类,金额(元),备注\n")

                    val csvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    expenses.forEach { entity ->
                        val date = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(entity.createdAtEpochMillis),
                            ZoneId.systemDefault()
                        ).format(csvFormatter)

                        val amount = centToYuanString(entity.amountCent)
                        
                        val typeStr = if (entity.type == 1) "收入" else "支出"
                        val note = entity.note.replace("\"", "\"\"")
                        val safeNote = if (note.contains(",") || note.contains("\n")) "\"$note\"" else note

                        sb.append("$date,$typeStr,${entity.category},$amount,$safeNote\n")
                    }

                    // Use timestamp for file name
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
                    val fileName = "backup_$timestamp.csv"
                    
                    file.writeText(sb.toString())

                    val fullUrl = if (state.url.endsWith("/")) state.url + state.path else "${state.url}/${state.path}"
                    val dirUrl = if (fullUrl.endsWith("/")) fullUrl else "$fullUrl/"
                    val targetUrl = dirUrl + fileName
                    
                    val result = webDavClient.uploadFile(targetUrl, state.username, state.password, file)
                    file.delete()
                    result
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            _webDavState.value = _webDavState.value.copy(
                isBackingUp = false,
                message = if (success) "备份上传成功" else "备份上传失败"
            )
        }
    }

    fun restoreFromWebDav(context: Context, fileName: String) {
        val state = _webDavState.value
        if (state.url.isBlank() || state.username.isBlank() || state.password.isBlank() || state.path.isBlank()) {
            _webDavState.value = state.copy(message = "请填写完整的配置信息")
            return
        }

        _webDavState.value = state.copy(isRestoring = true, showRestoreDialog = false, message = null)
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val file = java.io.File(context.cacheDir, fileName)
                    val fullUrl = if (state.url.endsWith("/")) state.url + state.path else "${state.url}/${state.path}"
                    val dirUrl = if (fullUrl.endsWith("/")) fullUrl else "$fullUrl/"
                    val sourceUrl = dirUrl + fileName

                    val downloaded = webDavClient.downloadFile(sourceUrl, state.username, state.password, file)
                    if (downloaded) {
                        // Parse CSV directly in this IO block to avoid race condition
                        val expensesToInsert = mutableListOf<ExpenseEntity>()
                        val csvFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                        file.bufferedReader(Charsets.UTF_8).use { reader ->
                            // Skip header (strip BOM if present)
                            reader.readLine()?.removePrefix("\uFEFF")

                            var line = reader.readLine()
                            while (line != null) {
                                try {
                                    val parts = parseCsvLine(line)
                                    if (parts.size >= 4) {
                                        val hasTypeColumn = parts.size >= 5
                                        val dateStr = parts[0]
                                        val type = if (hasTypeColumn && parts[1] == "收入") 1 else 0
                                        val categoryIdx = if (hasTypeColumn) 2 else 1
                                        val amountIdx = if (hasTypeColumn) 3 else 2
                                        val noteIdx = if (hasTypeColumn) 4 else 3

                                        val category = parts[categoryIdx]
                                        val amountStr = parts[amountIdx]
                                        val note = parts.getOrNull(noteIdx) ?: ""

                                        val dateTime = LocalDateTime.parse(dateStr, csvFormatter)
                                        val dateMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                                        val amountCent = BigDecimal(amountStr)
                                            .multiply(BigDecimal(100))
                                            .setScale(0, RoundingMode.HALF_UP)
                                            .longValueExact()

                                        expensesToInsert.add(
                                            ExpenseEntity(
                                                amountCent = amountCent,
                                                type = type,
                                                category = category,
                                                note = note,
                                                createdAtEpochMillis = dateMillis
                                            )
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                line = reader.readLine()
                            }
                        }

                        if (expensesToInsert.isNotEmpty()) {
                            repository.insertAllExpenses(expensesToInsert)
                        }
                        file.delete()
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            _webDavState.value = _webDavState.value.copy(
                isRestoring = false,
                message = if (success) "恢复成功" else "从云端恢复失败"
            )
        }
    }

    suspend fun collectAiAnalysisData(startDate: LocalDate, endDate: LocalDate): String {
        val allExpenses = repository.getAllExpensesSnapshot()
        val allAssets = repository.observeAssets().first()
        val budget = monthlyBudgetCentState.value

        val recentExpenses = allExpenses.filter { entity ->
            val date = Instant.ofEpochMilli(entity.createdAtEpochMillis)
                .atZone(zoneId).toLocalDate()
            !date.isBefore(startDate) && !date.isAfter(endDate)
        }

        val monthlyData = recentExpenses.groupBy { entity ->
            val date = Instant.ofEpochMilli(entity.createdAtEpochMillis)
                .atZone(zoneId).toLocalDate()
            "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
        }

        val monthsJson = JSONArray()
        for ((label, expenses) in monthlyData.toSortedMap()) {
            val monthObj = JSONObject()
            monthObj.put("label", label)
            monthObj.put("expense", expenses.filter { it.type == 0 }.sumOf { it.amountCent } / 100.0)
            monthObj.put("income", expenses.filter { it.type == 1 }.sumOf { it.amountCent } / 100.0)

            val categories = JSONArray()
            expenses.filter { it.type == 0 }
                .groupBy { it.category }
                .forEach { (name, items) ->
                    categories.put(JSONObject().apply {
                        put("name", name)
                        put("amount", items.sumOf { it.amountCent } / 100.0)
                    })
                }
            monthObj.put("categories", categories)
            monthsJson.put(monthObj)
        }

        val result = JSONObject()
        result.put("period", "$startDate ~ $endDate")
        result.put("months", monthsJson)
        result.put("budget", if (budget != null) budget / 100.0 else JSONObject.NULL)

        val assetsJson = JSONArray()
        allAssets.forEach { asset ->
            assetsJson.put(JSONObject().apply {
                put("name", asset.name)
                put("amount", asset.amountCent / 100.0)
                put("type", when (asset.type) {
                    0 -> "普通"
                    1 -> "信用卡"
                    2 -> "借出"
                    else -> "其他"
                })
            })
        }
        result.put("assets", assetsJson)

        return result.toString()
    }

    companion object {
        fun factory(repository: ExpenseRepository, sharedPreferences: SharedPreferences): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                        return ExpenseViewModel(repository, sharedPreferences) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    /**
     * Generates random test data for the current month for debugging purposes.
     * Inserts ~15 randomized items (appx 80% expenses, 20% income) 
     * distributed randomly across the days of the active month in the ViewModel.
     */
    fun generateTestData() {
        val expenseCategories = listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "其他")
        val incomeCategories = listOf("薪资", "奖金", "理财", "收债", "其他")

        viewModelScope.launch {
            // Determine the "target month" based on current ui state filtering or default to current month
            val filterState = dateRangeFilterState.value
            val baseMonth = filterState.startDate ?: LocalDate.now(zoneId).withDayOfMonth(1)
            
            val year = baseMonth.year
            val month = baseMonth.monthValue
            val daysInMonth = baseMonth.lengthOfMonth()
            
            val random = kotlin.random.Random(System.currentTimeMillis())

            for (i in 1..15) {
                // Pick a random day in the current month
                val day = random.nextInt(1, daysInMonth + 1)
                
                // Construct timestamp for that day at noon UTC to avoid timezone edge-case jumps
                val localDate = LocalDate.of(year, month, day)
                val dateMillis = localDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Determine type: 80% chance expense (0), 20% chance income (1)
                val isIncome = random.nextInt(100) < 20
                val type = if (isIncome) 1 else 0

                val category = if (isIncome) {
                    incomeCategories.random(random)
                } else {
                    expenseCategories.random(random)
                }

                // Random amount between 15.00 and 500.00 (in cents)
                val amountCent = random.nextLong(1500L, 50000L)
                val note = "测试数据 ${if (isIncome) "收入" else "支出"} #${i}"

                repository.addExpense(
                    amountCent = amountCent,
                    type = type,
                    category = category,
                    note = note,
                    dateMillis = dateMillis
                )
            }
        }
    }

    private fun loadAiBaseUrl(): String {
        val storedValue = sharedPreferences.getString("ai_api_endpoint", defaultAiBaseUrl).orEmpty()
        val normalized = if (storedValue.isBlank()) {
            defaultAiBaseUrl
        } else {
            normalizeAiBaseUrl(storedValue)
        }

        if (normalized != storedValue) {
            sharedPreferences.edit().putString("ai_api_endpoint", normalized).apply()
        }
        return normalized
    }

    private fun normalizeAiBaseUrl(value: String): String {
        return try {
            AiApiClient.buildBaseUrl(value)
        } catch (_: IllegalArgumentException) {
            value.trim()
        }
    }

    private fun setAiTestMessage(message: String, isError: Boolean) {
        aiTestMessageState.value = message
        aiTestErrorState.value = isError
    }
}
