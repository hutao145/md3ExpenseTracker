package com.example.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.util.getCategoryIcon
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults

import com.example.expensetracker.data.local.AssetEntity

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    assets: List<AssetEntity>,
    expenseId: Long,
    initialAmountInput: String,
    initialType: Int,
    initialCategory: String,
    initialNote: String,
    initialAssetId: Long?,
    isAmountValid: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (id: Long, amountInput: String, type: Int, category: String, note: String, assetId: Long?) -> Unit
) {
    var amountInput by rememberSaveable(expenseId) { mutableStateOf(initialAmountInput) }
    var selectedType by rememberSaveable(expenseId) { mutableStateOf(initialType) }
    var isCustomCategory by rememberSaveable(expenseId) {
        mutableStateOf(!listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "薪资", "奖金", "理财", "收债", "其他", "").contains(initialCategory))
    }
    var categoryInput by rememberSaveable(expenseId) {
        mutableStateOf(if (isCustomCategory) "自定义" else initialCategory.ifBlank { "其他" })
    }
    var customCategoryInput by rememberSaveable(expenseId) {
        mutableStateOf(if (isCustomCategory) initialCategory else "")
    }
    var noteInput by rememberSaveable(expenseId) { mutableStateOf(initialNote) }
    var selectedAssetId by rememberSaveable(expenseId) { mutableStateOf(initialAssetId) }

    val expenseSuggestions = listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "自定义", "其他")
    val incomeSuggestions = listOf("薪资", "奖金", "理财", "收债", "自定义", "其他")
    val currentSuggestions = if (selectedType == 0) expenseSuggestions else incomeSuggestions

    // Ensure currently selected category is valid for type
    LaunchedEffect(selectedType) {
        if (!currentSuggestions.contains(categoryInput) && categoryInput != "其他" && categoryInput != "自定义") {
            categoryInput = "其他"
        }
    }

    val amountValid = isAmountValid(amountInput)
    val categoryValid = categoryInput.trim().isNotEmpty()
    val showAmountError = amountInput.isNotBlank() && !amountValid
    val showCategoryError = categoryInput.isBlank()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.secondary)
                    
                    Text(text = "编辑记录", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

                    // Type Selector (Income/Expense)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = selectedType == 0,
                            onClick = { selectedType = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("支出")
                        }
                        SegmentedButton(
                            modifier = Modifier.weight(1f),
                            selected = selectedType == 1,
                            onClick = { selectedType = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("收入")
                        }
                    }

                    // Amount Input - Prominent
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "金额") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == 1) androidx.compose.ui.graphics.Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurface 
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = showAmountError,
                        supportingText = {
                            if (showAmountError) {
                                Text(text = "请输入有效的金额")
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )

                    // Category Selection with Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "选择分类",
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
                                                Icon(
                                                    imageVector = getCategoryIcon(label),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        )
                                    }
                                    repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                                }
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

                    // Asset Selection
                    if (assets.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "关联资产 (可选)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    }

                    // Note Input
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "备注 (可选)") },
                        placeholder = { Text("例如：午餐") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    if (showCategoryError) {
                         Text(
                            text = "请选择一个分类",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                            }
                        ) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(
                            onClick = {
                                if (amountValid && categoryValid) {
                                    val finalCategory = if (categoryInput == "自定义") {
                                        customCategoryInput.trim().ifBlank { "其他" }
                                    } else categoryInput
                                    onConfirm(expenseId, amountInput, selectedType, finalCategory, noteInput, selectedAssetId)
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                                }
                            },
                            enabled = amountValid && categoryValid
                        ) {
                            Text("保存")
                        }
                    }
        }
    }
}
