package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.ui.model.CategorySummaryUiModel
import com.example.expensetracker.ui.model.DailyExpenseUiModel
import com.example.expensetracker.ui.model.ExpenseItemUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.expensetracker.data.local.ExpenseEntity

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
    val isDateRangeApplied: Boolean = false
)

private data class DateRangeFilterState(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startDateInput: String = "",
    val endDateInput: String = "",
    val errorMessage: String? = null
)

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
    private val dateInputFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("uuuu-MM-dd", Locale.CHINA)
        .withResolverStyle(ResolverStyle.STRICT)

    private val dateRangeFilterState = MutableStateFlow(DateRangeFilterState())
    private val monthlyBudgetCentState = MutableStateFlow<Long?>(null)
    private val searchQueryState = MutableStateFlow("")

    init {
        goToCurrentMonth()
    }

    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.observeExpenses(),
        dateRangeFilterState,
        monthlyBudgetCentState,
        searchQueryState
    ) { expenses, dateRangeFilter, monthlyBudgetCent, searchQuery ->
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
                                note = item.note
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
            isDateRangeApplied = dateRangeFilter.startDate != null || dateRangeFilter.endDate != null
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseUiState()
        )

    fun addExpense(amountInput: String, type: Int, category: String, note: String, dateMillis: Long) {
        val amountCent = parseAmountToCent(amountInput) ?: return

        viewModelScope.launch {
            repository.addExpense(
                amountCent = amountCent,
                type = type,
                category = category,
                note = note,
                dateMillis = dateMillis
            )
        }
    }

    fun updateExpense(id: Long, amountInput: String, type: Int, category: String, note: String) {
        val amountCent = parseAmountToCent(amountInput) ?: return

        viewModelScope.launch {
            repository.updateExpense(
                id = id,
                amountCent = amountCent,
                type = type,
                category = category,
                note = note
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
            ids.forEach { id -> repository.deleteExpense(id) }
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

                val amount = BigDecimal(entity.amountCent)
                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                    .toString()
                
                val typeStr = if (entity.type == 1) "收入" else "支出"

                // Escape quotes and commas in note
                val note = entity.note.replace("\"", "\"\"")
                val safeNote = if (note.contains(",") || note.contains("\n")) "\"$note\"" else note

                sb.append("$date,$typeStr,${entity.category},$amount,$safeNote\n")
            }

            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(sb.toString().toByteArray())
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
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            // Skip header
                            reader.readLine()

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
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString())
        return tokens
            .map { it.replace("\"\"", "\"") } // Unescape double quotes
            .map { if (it.startsWith("\"") && it.endsWith("\"")) it.substring(1, it.length - 1) else it } // Remove surrounding quotes
    }

    private fun parseAmountToCent(amountInput: String): Long? {
        val normalized = amountInput.trim()
        if (normalized.isEmpty()) {
            return null
        }

        return try {
            val amountYuan = normalized.toBigDecimal()
            if (amountYuan <= BigDecimal.ZERO) {
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

    companion object {
        fun factory(repository: ExpenseRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                        return ExpenseViewModel(repository) as T
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
}
