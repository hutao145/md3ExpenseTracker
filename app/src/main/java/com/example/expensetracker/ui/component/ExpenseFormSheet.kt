package com.example.expensetracker.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.local.AssetEntity
import com.example.expensetracker.ui.util.getCategoryIcon
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val expenseSuggestions = listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "自定义", "其他")
private val incomeSuggestions = listOf("薪资", "奖金", "理财", "收债", "自定义", "其他")
private val knownCategories = listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "薪资", "奖金", "理财", "收债", "其他", "")

private val md3EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val md3ExitEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

/**
 * Unified expense form for both adding and editing.
 * - Add mode: editId = null, shows date picker
 * - Edit mode: editId != null, shows current date picker
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    assets: List<AssetEntity>,
    isAmountValid: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    // Add mode callback
    onAdd: ((amountInput: String, type: Int, category: String, note: String, assetId: Long?, dateMillis: Long) -> Unit)? = null,
    // Edit mode callback
    onUpdate: ((id: Long, amountInput: String, type: Int, category: String, note: String, assetId: Long?, dateMillis: Long) -> Unit)? = null,
    // Edit mode initial values
    editId: Long? = null,
    initialAmount: String = "",
    initialType: Int = 0,
    initialCategory: String = "其他",
    initialNote: String = "",
    initialAssetId: Long? = null,
    initialDateMillis: Long? = null
) {
    val isEditMode = editId != null
    val stateKey = editId ?: -1L

    val isInitialCustom = isEditMode && !knownCategories.contains(initialCategory)

    var amountInput by rememberSaveable(stateKey) { mutableStateOf(initialAmount) }
    var selectedType by rememberSaveable(stateKey) { mutableStateOf(initialType) }
    var categoryInput by rememberSaveable(stateKey) {
        mutableStateOf(if (isInitialCustom) "自定义" else initialCategory.ifBlank { "其他" })
    }
    var customCategoryInput by rememberSaveable(stateKey) {
        mutableStateOf(if (isInitialCustom) initialCategory else "")
    }
    var noteInput by rememberSaveable(stateKey) { mutableStateOf(initialNote) }
    var selectedAssetId by rememberSaveable(stateKey) { mutableStateOf(initialAssetId) }
    var dateMillis by rememberSaveable(stateKey) { mutableStateOf(initialDateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val currentSuggestions = if (selectedType == 0) expenseSuggestions else incomeSuggestions

    LaunchedEffect(selectedType) {
        if (!currentSuggestions.contains(categoryInput) && categoryInput != "自定义") {
            categoryInput = "其他"
        }
    }

    val amountValid = isAmountValid(amountInput)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA).withZone(ZoneId.systemDefault())
    }

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
    }

    fun submit() {
        val finalCategory = if (categoryInput == "自定义") {
            customCategoryInput.trim().ifBlank { "其他" }
        } else categoryInput

        if (isEditMode) {
            onUpdate?.invoke(editId!!, amountInput, selectedType, finalCategory, noteInput, selectedAssetId, dateMillis)
        } else {
            onAdd?.invoke(amountInput, selectedType, finalCategory, noteInput, selectedAssetId, dateMillis)
        }
        dismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .then(if (isEditMode) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    modifier = Modifier.weight(1f),
                    selected = selectedType == 0,
                    onClick = { selectedType = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("支出") }
                SegmentedButton(
                    modifier = Modifier.weight(1f),
                    selected = selectedType == 1,
                    onClick = { selectedType = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("收入") }
            }

            // Amount input
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("金额") },
                placeholder = { Text("0.00") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selectedType == 1) Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountInput.isNotBlank() && !amountValid,
                supportingText = if (amountInput.isNotBlank() && !amountValid) {
                    { Text("请输入有效的金额") }
                } else null
            )

            // Category selection
            Text(
                "选择分类",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentSuggestions.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { label ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = categoryInput == label,
                                onClick = { categoryInput = label },
                                label = { Text(label, maxLines = 1) },
                                leadingIcon = {
                                    Icon(getCategoryIcon(label), null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }

                // Custom category input
                AnimatedContent(
                    targetState = categoryInput == "自定义",
                    transitionSpec = {
                        if (targetState) {
                            (fadeIn(tween(250, easing = md3EnterEasing)) +
                                    slideInVertically(tween(300, easing = md3EnterEasing)) { -it / 3 })
                                .togetherWith(fadeOut(tween(150, easing = md3ExitEasing)))
                        } else {
                            fadeIn(tween(200, delayMillis = 50, easing = md3EnterEasing))
                                .togetherWith(
                                    fadeOut(tween(150, easing = md3ExitEasing)) +
                                            slideOutVertically(tween(200, easing = md3ExitEasing)) { -it / 3 }
                                )
                        }.using(SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(300, easing = md3EnterEasing) }))
                    },
                    label = "customCategoryTransition"
                ) { isCustom ->
                    if (isCustom) {
                        OutlinedTextField(
                            value = customCategoryInput,
                            onValueChange = { customCategoryInput = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            label = { Text("输入自定义分类") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }

            // Asset selection
            if (assets.isNotEmpty()) {
                Text(
                    "关联资产 (可选)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedAssetId == null,
                        onClick = { selectedAssetId = null },
                        label = { Text("不关联") }
                    )
                    assets.forEach { asset ->
                        FilterChip(
                            selected = selectedAssetId == asset.id,
                            onClick = { selectedAssetId = asset.id },
                            label = { Text(asset.name) }
                        )
                    }
                }
            }

            if (isEditMode) {
                OutlinedTextField(
                    value = dateFormatter.format(Instant.ofEpochMilli(dateMillis)),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("日期") },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "选择日期")
                        }
                    }
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注 (可选)") },
                    placeholder = { Text("例如：午餐") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            } else {
                // Add mode: date + note side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dateFormatter.format(Instant.ofEpochMilli(dateMillis)),
                        onValueChange = {},
                        modifier = Modifier.weight(1f),
                        label = { Text("日期") },
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, "选择日期")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("备注 (可选)") },
                        singleLine = true
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = ::dismiss) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = ::submit,
                    enabled = amountValid
                ) { Text("保存") }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}
