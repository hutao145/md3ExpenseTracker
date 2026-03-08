package com.example.expensetracker.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.component.AddExpenseDialog
import com.example.expensetracker.ui.component.EditExpenseDialog
import com.example.expensetracker.ui.model.CategorySummaryUiModel
import com.example.expensetracker.ui.model.DailyExpenseUiModel
import com.example.expensetracker.ui.model.ExpenseItemUiModel
import com.example.expensetracker.ui.viewmodel.ExpenseUiState
import com.example.expensetracker.ui.util.getCategoryIcon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.background
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.expensetracker.ui.viewmodel.ExpenseViewModel


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseListScreen(
    uiState: ExpenseUiState,
    isAmountValid: (String) -> Boolean,
    searchQuery: String,
    addExpenseTrigger: Long = 0L,
    onSearchQueryChange: (String) -> Unit,
    onAddExpense: (amountInput: String, type: Int, category: String, note: String, assetId: Long?, dateMillis: Long) -> Unit,
    onUpdateExpense: (id: Long, amountInput: String, type: Int, category: String, note: String, assetId: Long?) -> Unit,
    onDeleteExpense: (id: Long) -> Unit,
    onDeleteMultipleExpenses: (ids: Set<Long>) -> Unit,
    onApplyDateRange: (startDateInput: String, endDateInput: String) -> Unit,
    onClearDateRange: () -> Unit,
    onSetMonthlyBudget: (amountInput: String) -> Unit,
    onClearMonthlyBudget: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    viewModel: ExpenseViewModel
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ExpenseItemUiModel?>(null) }
    var deletingItem by remember { mutableStateOf<ExpenseItemUiModel?>(null) }
    var startDateInput by rememberSaveable { mutableStateOf(uiState.startDateInput) }
    var endDateInput by rememberSaveable { mutableStateOf(uiState.endDateInput) }
    var searchInput by rememberSaveable { mutableStateOf(searchQuery) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    val isSelectionMode = selectedIds.isNotEmpty()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.startDateInput) {
        startDateInput = uiState.startDateInput
    }
    LaunchedEffect(uiState.endDateInput) {
        endDateInput = uiState.endDateInput
    }
    LaunchedEffect(uiState.searchQuery) {
        searchInput = uiState.searchQuery
        if (uiState.searchQuery.isNotBlank()) {
            // isSearchActive = true // Original line, will be replaced by new state management
        }
    }
    var showBudgetDialog by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val dateRangeFilterState by viewModel.dateRangeFilterState.collectAsState()
    
    val syncMessage by viewModel.syncMessage.collectAsState()

    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    LaunchedEffect(addExpenseTrigger) {
        if (addExpenseTrigger > 0L) {
            showAddDialog = true
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                androidx.compose.material3.BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { selectedIds = emptySet() }
                        ) {
                            Text(
                                text = "取消选择",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        val allIds = uiState.dailySummaries.flatMap { it.items }.map { it.id }.toSet()
                        val allSelected = allIds.isNotEmpty() && selectedIds.containsAll(allIds)
                        TextButton(
                            onClick = {
                                selectedIds = if (allSelected) emptySet() else allIds
                            }
                        ) {
                            AnimatedContent(
                                targetState = allSelected,
                                label = "SelectAllText",
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }
                            ) { isAllSelected ->
                                Text(
                                    text = if (isAllSelected) "取消全选" else "全选",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        TextButton(
                            onClick = { showBatchDeleteConfirm = true }
                        ) {
                            AnimatedContent(
                                targetState = selectedIds.size,
                                label = "DeleteCountText",
                                transitionSpec = {
                                    (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                                }
                            ) { count ->
                                Text(
                                    text = "删除 $count 条",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Section
                item {
                    HomeHeader(
                        totalExpenseCent = uiState.totalExpenseCent,
                        totalIncomeCent = uiState.totalIncomeCent,
                        monthlyBudgetCent = uiState.monthlyBudgetCent,
                        currentMonthLabel = uiState.currentMonthLabel,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onCurrentMonth = onCurrentMonth,
                        onBudgetClick = { showBudgetDialog = true },
                        onSearchClick = { isSearchActive = !isSearchActive },
                        onFilterClick = { showFilterDialog = true },
                        onSettingsClick = onSettingsClick,
                        onSyncClick = { viewModel.syncFromAutoAccounting() }
                    )
                }

                // Active Filter Indicator
                item {
                    AnimatedVisibility(
                        visible = uiState.isDateRangeApplied && !uiState.isMonthMode,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            InputChip(
                                selected = true,
                                onClick = { showFilterDialog = true },
                                label = { Text(formatDateRangeLabel(uiState.startDateInput, uiState.endDateInput)) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear Filter",
                                        modifier = Modifier.size(16.dp).clickable { onClearDateRange() }
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // Search Bar (Visible when active)
                item {
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                             OutlinedTextField(
                                value = searchInput,
                                onValueChange = {
                                    searchInput = it
                                    onSearchQueryChange(it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(text = "搜索分类 / 备注") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        searchInput = ""
                                        onSearchQueryChange("")
                                        isSearchActive = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "关闭搜索")
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        CategorySummaryCard(
                            categorySummaries = uiState.categorySummaries,
                            totalExpenseCent = uiState.totalExpenseCent,
                            onStatisticsClick = onStatisticsClick,
                            onCategoryClick = { categoryName ->
                                searchInput = categoryName
                                onSearchQueryChange(categoryName)
                                isSearchActive = true
                            }
                        )
                    }
                }

                if (uiState.dailySummaries.isEmpty()) {
                    item {
                         Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EmptyStateCard(isDateRangeApplied = uiState.isDateRangeApplied)
                        }
                    }
                } else {
                    items(
                        items = uiState.dailySummaries,
                        key = { it.date }
                    ) { item ->
                         Box(modifier = Modifier.padding(horizontal = 16.dp).animateItem()) {
                            DailySummaryCard(
                                item = item,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onToggleSelect = { id ->
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                },
                                onEnterSelection = { id ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedIds = setOf(id)
                                },
                                onEditExpense = { selected -> editingItem = selected },
                                onDeleteExpense = { selected -> deletingItem = selected }
                            )
                        }
                    }
                }
            }
        }
            
            // FAB 独立于 Scaffold，避免被底部操作栏顶起而产生跳跃
            // 不加任何 AnimatedVisibility——直接用 alpha/scale 诚主进出场动画
            val fabAlpha by animateFloatAsState(
                targetValue = if (isSelectionMode) 0f else 1f,
                animationSpec = tween(durationMillis = 200),
                label = "fabAlpha"
            )
            val fabScale by animateFloatAsState(
                targetValue = if (isSelectionMode) 0.8f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                label = "fabScale"
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
                    .graphicsLayer {
                        alpha = fabAlpha
                        scaleX = fabScale
                        scaleY = fabScale
                    },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 回到顶部按钮
                AnimatedVisibility(
                    visible = listState.firstVisibleItemIndex > 0 && !isSelectionMode,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "回到顶部")
                    }
                }

                FloatingActionButton(
                    onClick = { if (!isSelectionMode) showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text(text = "批量删除") },
            text = { Text(text = "确定要删除已选的 ${selectedIds.size} 条记录吗？该操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMultipleExpenses(selectedIds)
                        selectedIds = emptySet()
                        showBatchDeleteConfirm = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(text = "删除") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text(text = "取消") }
            }
        )
    }

    if (showAddDialog) {
        AddExpenseDialog(
            assets = uiState.assets,
            isAmountValid = isAmountValid,
            onDismissRequest = { showAddDialog = false },
            onConfirm = { amountInput, type, category, note, assetId, dateMillis ->
                onAddExpense(amountInput, type, category, note, assetId, dateMillis)
                showAddDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        MonthlyBudgetDialog(
            currentBudgetCent = uiState.monthlyBudgetCent,
            isAmountValid = isAmountValid,
            onDismissRequest = { showBudgetDialog = false },
            onConfirm = { amountInput ->
                onSetMonthlyBudget(amountInput)
                showBudgetDialog = false
            },
            onClear = {
                onClearMonthlyBudget()
                showBudgetDialog = false
            }
        )
    }

    if (showFilterDialog) {
        DateRangeFilterDialog(
            startDateInput = startDateInput,
            endDateInput = endDateInput,
            onStartDateChange = { startDateInput = it },
            onEndDateChange = { endDateInput = it },
            onApply = { onApplyDateRange(startDateInput, endDateInput) },
            onDismissRequest = { showFilterDialog = false }
        )
    }

    editingItem?.let { target ->
        EditExpenseDialog(
            assets = uiState.assets,
            expenseId = target.id,
            initialAmountInput = formatAmountInput(target.amountCent),
            initialType = target.type,
            initialCategory = target.category,
            initialNote = target.note,
            initialAssetId = target.assetId,
            isAmountValid = isAmountValid,
            onDismissRequest = { editingItem = null },
            onConfirm = { id, amountInput, type, category, note, assetId ->
                 onUpdateExpense(id, amountInput, type, category, note, assetId)
                 editingItem = null
            }
        )
    }

    deletingItem?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text(text = "删除消费") },
            text = {
                Text(text = "确定删除这条记录吗？该操作无法撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(target.id)
                        deletingItem = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSummaryCard(
    totalExpenseCent: Long,
    totalIncomeCent: Long,
    startDateInput: String,
    endDateInput: String,
    dateRangeError: String?,
    isDateRangeApplied: Boolean,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "按日期区间统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            DatePickerTriggerField(
                label = "开始日期",
                value = startDateInput,
                onSelectClick = { showStartDatePicker = true }
            )

            DatePickerTriggerField(
                label = "结束日期",
                value = endDateInput,
                onSelectClick = { showEndDatePicker = true }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = isDateRangeApplied || startDateInput.isNotBlank() || endDateInput.isNotBlank(),
                    onClick = onClear
                ) {
                    Text(text = "清空")
                }
                TextButton(onClick = onApply) {
                    Text(text = "应用区间")
                }
            }

            if (isDateRangeApplied) {
                Text(
                    text = "当前区间：${formatDateRangeLabel(startDateInput, endDateInput)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (dateRangeError != null) {
                Text(
                    text = dateRangeError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (isDateRangeApplied) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "总收入：+${formatAmount(totalIncomeCent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color(0xFF388E3C)
                    )
                    Text(
                        text = "总支出：-${formatAmount(totalExpenseCent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showStartDatePicker) {
        val startDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateInputToUtcMillis(startDateInput)
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { selectedMillis ->
                            onStartDateChange(utcMillisToDateInput(selectedMillis))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(text = "取消")
                }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                showModeToggle = false
            )
        }
    }

    if (showEndDatePicker) {
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateInputToUtcMillis(endDateInput)
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { selectedMillis ->
                            onEndDateChange(utcMillisToDateInput(selectedMillis))
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(text = "取消")
                }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                showModeToggle = false
            )
        }
    }
}

@Composable
private fun DatePickerTriggerField(
    label: String,
    value: String,
    onSelectClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelectClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value.ifBlank { "请选择" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHeader(
    totalExpenseCent: Long,
    totalIncomeCent: Long,
    monthlyBudgetCent: Long?,
    currentMonthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onBudgetClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)
        ) {
            // Top Row: Month Selector & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "上个月",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(onClick = onCurrentMonth) {
                        Text(
                            text = currentMonthLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "下个月",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "日期筛选",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val netBalance = totalIncomeCent - totalExpenseCent

            // Total Balance
            Column(horizontalAlignment = Alignment.Start) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "总结余",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatAmount(netBalance),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = onSyncClick,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "自动同步账单",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                         Text(
                            text = "收入",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "+${formatAmount(totalIncomeCent)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = androidx.compose.ui.graphics.Color(0xFF388E3C)
                        )
                    }
                    Column {
                         Text(
                            text = "支出",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "-${formatAmount(totalExpenseCent)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Budget Progress
            if (monthlyBudgetCent != null && monthlyBudgetCent > 0) {
                val targetProgress = (totalExpenseCent.toFloat() / monthlyBudgetCent.toFloat()).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "BudgetProgress"
                )
                val remaining = monthlyBudgetCent - totalExpenseCent
                
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "预算剩余 ${formatAmount(remaining)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${(targetProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onBudgetClick() },
                        color = if (targetProgress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    )
                }
            } else {
                 OutlinedButton(
                    onClick = onBudgetClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                 ) {
                     Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                     Spacer(modifier = Modifier.width(8.dp))
                     Text("设置本月预算")
                 }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilterDialog(
    startDateInput: String,
    endDateInput: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onApply: () -> Unit,
    onDismissRequest: () -> Unit
) {
    // Current State
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = dateInputToUtcMillis(startDateInput),
        initialSelectedEndDateMillis = dateInputToUtcMillis(endDateInput)
    )

    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        showDialog = true
    }

    fun dismiss() {
        showDialog = false
        scope.launch {
            delay(300)
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = showDialog,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = scaleOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.98f) // Increase width to avoid clipping
                    .fillMaxHeight(0.85f)
                    .padding(4.dp), // Reduce padding
                shape = RoundedCornerShape(28.dp),
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "选择日期范围",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getDateRangeHeaderText(dateRangePickerState),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { dismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // DateRangePicker
                    androidx.compose.material3.DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.weight(1f), // Use weight to fill available space
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onStartDateChange("")
                                onEndDateChange("")
                                onApply()
                                dismiss()
                            }
                        ) {
                            Text("重置")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val start = dateRangePickerState.selectedStartDateMillis?.let { utcMillisToDateInput(it) } ?: ""
                                val end = dateRangePickerState.selectedEndDateMillis?.let { utcMillisToDateInput(it) } ?: ""
                                onStartDateChange(start)
                                onEndDateChange(end)
                                onApply()
                                dismiss()
                            }
                        ) {
                            Text("确认")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun getDateRangeHeaderText(state: androidx.compose.material3.DateRangePickerState): String {
    val startMillis = state.selectedStartDateMillis
    val endMillis = state.selectedEndDateMillis

    if (startMillis == null) {
        return "请选择开始日期"
    }
    
    val startText = utcMillisToDateInput(startMillis)
    if (endMillis == null) {
        return "$startText - 请选择"
    }

    val endText = utcMillisToDateInput(endMillis)
    return "$startText - $endText"
}



@Composable
private fun EmptyStateCard(isDateRangeApplied: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isDateRangeApplied) "该区间暂无消费记录" else "本月暂无消费，记一笔吧",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategorySummaryCard(
    categorySummaries: List<CategorySummaryUiModel>,
    totalExpenseCent: Long,
    onStatisticsClick: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "分类统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onStatisticsClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "查看图表统计",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (totalExpenseCent <= 0L) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categorySummaries.forEach { summary ->
                    val percent = summary.totalExpenseCent.toDouble() / totalExpenseCent.toDouble()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clickable { onCategoryClick(summary.category) }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getCategoryIcon(summary.category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = summary.category,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = formatAmount(summary.totalExpenseCent),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                             androidx.compose.material3.LinearProgressIndicator(
                                progress = { percent.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .align(Alignment.CenterVertically),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${"%.1f".format(percent * 100)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBudgetDialog(
    currentBudgetCent: Long?,
    isAmountValid: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (amountInput: String) -> Unit,
    onClear: () -> Unit
) {
    var amountInput by rememberSaveable {
        mutableStateOf(
            currentBudgetCent?.takeIf { it > 0L }?.let { formatAmountInput(it) } ?: ""
        )
    }

    val amountValid = isAmountValid(amountInput)
    val showAmountError = amountInput.isNotBlank() && !amountValid

    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        showDialog = true
    }

    fun dismiss() {
        showDialog = false
        scope.launch {
            delay(300)
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = showDialog,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = scaleOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "设置本月预算", style = MaterialTheme.typography.headlineSmall)
                    
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "预算金额（元）") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        isError = showAmountError,
                        supportingText = {
                            if (showAmountError) {
                                Text(text = "请输入大于 0 的合法金额")
                            } else if (currentBudgetCent != null && currentBudgetCent > 0L) {
                                Text(text = "当前预算：${formatAmount(currentBudgetCent)}")
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                         TextButton(onClick = {
                            onClear()
                            dismiss()
                        }) {
                            Text(text = "清除预算")
                        }
                        TextButton(onClick = { dismiss() }) {
                            Text(text = "取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            enabled = amountValid,
                            onClick = { 
                                onConfirm(amountInput.trim()) 
                                dismiss()
                            }
                        ) {
                            Text(text = "保存")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(
    item: DailyExpenseUiModel,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onEnterSelection: (Long) -> Unit,
    onEditExpense: (ExpenseItemUiModel) -> Unit,
    onDeleteExpense: (ExpenseItemUiModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.date,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "支出: ${formatAmount(item.totalExpenseCent)} / 收入: ${formatAmount(item.totalIncomeCent)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                item.items.forEachIndexed { index, expense ->
                    ExpenseItemRow(
                        item = expense,
                        isSelected = expense.id in selectedIds,
                        isSelectionMode = isSelectionMode,
                        onToggleSelect = { onToggleSelect(expense.id) },
                        onEnterSelection = { onEnterSelection(expense.id) },
                        onEditClick = { onEditExpense(expense) },
                        onDeleteClick = { onDeleteExpense(expense) }
                    )
                    if (index < item.items.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseItemRow(
    item: ExpenseItemUiModel,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEnterSelection: () -> Unit = {},
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                      else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "RowBackground"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onEditClick() },
                onLongClick = { if (!isSelectionMode) onEnterSelection() }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Crossfade between Checkbox and category icon
        Crossfade(
            targetState = isSelectionMode,
            animationSpec = tween(200),
            label = "IconCheckboxCrossfade"
        ) { inSelectionMode ->
            if (inSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(item.category),
                            contentDescription = item.category,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.assetName != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.assetName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (item.note.isNotBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Amount
        val amountColor = if (item.type == 1) androidx.compose.ui.graphics.Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurface
        val prefix = if (item.type == 1) "+" else "-"
        Text(
            text = "$prefix ${formatAmount(item.amountCent)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

private fun dateInputToUtcMillis(dateInput: String): Long? {
    if (dateInput.isBlank()) {
        return null
    }

    return runCatching {
        LocalDate.parse(dateInput)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private fun utcMillisToDateInput(utcMillis: Long): String {
    return Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()
}

private fun formatDateRangeLabel(startDateInput: String, endDateInput: String): String {
    return when {
        startDateInput.isNotBlank() && endDateInput.isNotBlank() -> "$startDateInput 至 $endDateInput"
        startDateInput.isNotBlank() -> "$startDateInput 起"
        endDateInput.isNotBlank() -> "截至 $endDateInput"
        else -> "全部时间"
    }
}

private fun formatAmount(amountCent: Long): String {
    val amountYuan = amountCent / 100.0
    return String.format(Locale.CHINA, "¥%.2f", amountYuan)
}

private fun formatAmountInput(amountCent: Long): String {
    val amountYuan = amountCent / 100.0
    return String.format(Locale.CHINA, "%.2f", amountYuan)
}
