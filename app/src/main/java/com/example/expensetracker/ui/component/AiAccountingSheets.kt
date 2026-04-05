package com.example.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.util.formatAmountWithSymbol
import com.example.expensetracker.ui.viewmodel.AiAccountingDraft
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val aiExpenseCategories = listOf("餐饮", "交通", "购物", "日用", "娱乐", "住房", "其他")
private val aiIncomeCategories = listOf("薪资", "奖金", "理财", "收债", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAccountingInputSheet(
    inputText: String,
    isLoading: Boolean,
    errorMessage: String?,
    onInputChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { AiSheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("AI 智能记账", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                minLines = 2,
                maxLines = 4,
                textStyle = MaterialTheme.typography.titleMedium,
                placeholder = { Text("例如：10 元吃晚餐 4 元买饮料") },
                trailingIcon = {
                    IconButton(onClick = onSubmit, enabled = inputText.isNotBlank() && !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                    }
                },
                shape = RoundedCornerShape(18.dp)
            )

            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismissRequest, enabled = !isLoading) { Text("取消") }
                Button(onClick = onSubmit, enabled = inputText.isNotBlank() && !isLoading) { Text("开始解析") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAccountingConfirmSheet(
    inputText: String,
    drafts: List<AiAccountingDraft>,
    infoMessage: String?,
    errorMessage: String?,
    isSaving: Boolean,
    onDismissRequest: () -> Unit,
    onBackToEdit: () -> Unit,
    onRetry: () -> Unit,
    onConfirm: (List<AiAccountingDraft>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editableDrafts = remember(drafts) { mutableStateListOf<AiAccountingDraft>().apply { addAll(drafts) } }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { AiSheetHandle() }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            val listMaxHeight = maxHeight * 0.58f

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("AI 智能记账", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleMedium,
                    trailingIcon = {
                        IconButton(onClick = onBackToEdit, enabled = !isSaving) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "返回修改")
                        }
                    },
                    shape = RoundedCornerShape(18.dp)
                )

                HorizontalDivider()

                Text(
                    text = "共解析 ${editableDrafts.size} 条，可修改后确认",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (infoMessage != null) {
                    Text(infoMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = listMaxHeight),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(editableDrafts) { index, draft ->
                        AiAccountingEditableCard(
                            draft = draft,
                            onDraftChange = { updated -> editableDrafts[index] = updated }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isSaving,
                        modifier = Modifier.widthIn(min = 92.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = { onConfirm(editableDrafts.toList()) },
                        enabled = editableDrafts.isNotEmpty() && !isSaving,
                        modifier = Modifier.widthIn(min = 168.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("全部记账（${editableDrafts.size} 条）")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AiAccountingEditableCard(
    draft: AiAccountingDraft,
    onDraftChange: (AiAccountingDraft) -> Unit
) {
    val categories = if (draft.type == 1) aiIncomeCategories else aiExpenseCategories
    val amountColor = if (draft.type == 1) MaterialTheme.colorScheme.primary else Color(0xFFB3261E)
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAmountWithSymbol(draft.amountCent),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = draft.type == 0,
                        onClick = { onDraftChange(draft.copy(type = 0, category = if (draft.category in aiExpenseCategories) draft.category else "其他")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("支出", style = MaterialTheme.typography.labelLarge)
                    }
                    SegmentedButton(
                        selected = draft.type == 1,
                        onClick = { onDraftChange(draft.copy(type = 1, category = if (draft.category in aiIncomeCategories) draft.category else "其他")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("收入", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    FilterChip(
                        selected = draft.category == category,
                        onClick = { onDraftChange(draft.copy(category = category)) },
                        label = { Text(category, style = MaterialTheme.typography.labelLarge) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("备注：${draft.note.ifBlank { "无" }}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "日期：${Instant.ofEpochMilli(draft.dateMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (draft.assetNameOrNull != null) {
                    Text("资产：${draft.assetNameOrNull}", style = MaterialTheme.typography.bodyMedium)
                }
                if (draft.usedFallbackFields.isNotEmpty()) {
                    Text(
                        "默认字段：${draft.usedFallbackFields.joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSheetHandle() {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth(0.1f)
            .heightIn(min = 6.dp)
    ) {}
}
