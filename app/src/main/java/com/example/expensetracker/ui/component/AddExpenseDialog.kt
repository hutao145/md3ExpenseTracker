package com.example.expensetracker.ui.component

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.TextFieldDefaults
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    assets: List<AssetEntity>,
    isAmountValid: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (amountInput: String, type: Int, category: String, note: String, assetId: Long?, dateMillis: Long) -> Unit
) {
    var amountInput by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(0) }
    var categoryInput by rememberSaveable { mutableStateOf("其他") }
    var customCategoryInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var selectedAssetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val expenseSuggestions = listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "自定义", "其他")
    val incomeSuggestions = listOf("薪资", "奖金", "理财", "收债", "自定义", "其他")
    val currentSuggestions = if (selectedType == 0) expenseSuggestions else incomeSuggestions

    LaunchedEffect(selectedType) {
        if (!currentSuggestions.contains(categoryInput)) categoryInput = "其他"
    }

    val amountValid = isAmountValid(amountInput)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA).withZone(ZoneId.systemDefault())
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
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 类型选择
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

            // 金额输入
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

            // 分类
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
                        // 如果这一行不足4个，补齐占位
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = categoryInput == "自定义") {
                    OutlinedTextField(
                        value = customCategoryInput,
                        onValueChange = { customCategoryInput = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        label = { Text("输入自定义分类") },
                        singleLine = true
                    )
                }
            }

            // 关联资产
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

            // 日期 + 备注（同一行）
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
                    },
                    enabled = false,
                    colors = TextFieldDefaults.colors(
                        disabledContainerColor = Color.Transparent,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("备注 (可选)") },
                    singleLine = true
                )
            }

            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                }) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (amountValid) {
                            val finalCategory = if (categoryInput == "自定义") {
                                customCategoryInput.trim().ifBlank { "其他" }
                            } else categoryInput
                            
                            onConfirm(amountInput, selectedType, finalCategory, noteInput, selectedAssetId, dateMillis)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                        }
                    },
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
