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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.screen.getCategoryIcon
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditExpenseDialog(
    expenseId: Long,
    initialAmountInput: String,
    initialType: Int,
    initialCategory: String,
    initialNote: String,
    isAmountValid: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (id: Long, amountInput: String, type: Int, category: String, note: String) -> Unit
) {
    var amountInput by rememberSaveable(expenseId) { mutableStateOf(initialAmountInput) }
    var selectedType by rememberSaveable(expenseId) { mutableStateOf(initialType) }
    var categoryInput by rememberSaveable(expenseId) {
        mutableStateOf(initialCategory.ifBlank { "其他" })
    }
    var noteInput by rememberSaveable(expenseId) { mutableStateOf(initialNote) }

    val expenseSuggestions = listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "其他")
    val incomeSuggestions = listOf("薪资", "奖金", "理财", "收债", "其他")
    val currentSuggestions = if (selectedType == 0) expenseSuggestions else incomeSuggestions

    // Ensure currently selected category is valid for type
    LaunchedEffect(selectedType) {
        if (!currentSuggestions.contains(categoryInput) && categoryInput != "其他") {
            categoryInput = "其他"
        }
    }

    val amountValid = isAmountValid(amountInput)
    val categoryValid = categoryInput.trim().isNotEmpty()
    val showAmountError = amountInput.isNotBlank() && !amountValid
    val showCategoryError = categoryInput.isBlank()

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
                    modifier = Modifier
                        .padding(24.dp)
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
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(min = 130.dp)
                        ) {
                            currentSuggestions.forEach { label ->
                                FilterChip(
                                    selected = categoryInput == label,
                                    onClick = { categoryInput = label },
                                    label = { Text(label) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = getCategoryIcon(label),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
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

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { dismiss() }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        TextButton(
                            onClick = {
                                if (amountValid) {
                                    onConfirm(expenseId, amountInput, selectedType, categoryInput, noteInput)
                                    dismiss()
                                }
                            },
                            enabled = amountValid
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}
