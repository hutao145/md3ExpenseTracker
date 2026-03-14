package com.example.expensetracker.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.local.AssetEntity
import com.example.expensetracker.ui.util.centToYuanString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAssetDialog(
    asset: AssetEntity,
    isAmountValid: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (id: Long, name: String, amount: String, type: Int) -> Unit,
    onDelete: (id: Long) -> Unit
) {
    var nameInput by remember { mutableStateOf(asset.name) }

    val initialAmount = centToYuanString(asset.amountCent)
    var amountInput by remember { mutableStateOf(initialAmount) }

    var selectedType by remember { mutableStateOf(asset.type) }
    var isDeleteConfirming by remember { mutableStateOf(false) }

    val assetTypes = listOf("资产", "负债", "借出")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            isDeleteConfirming = false
            onDismissRequest()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "编辑资产",
                fontWeight = FontWeight.SemiBold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                assetTypes.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedType == index,
                        onClick = { selectedType = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = assetTypes.size)
                    ) {
                        Text(label)
                    }
                }
            }

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("名称 (如: 微信, 支付宝)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("金额") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountInput.isNotEmpty() && !isAmountValid(amountInput),
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedContent(
                targetState = isDeleteConfirming,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                label = "DeleteConfirmTransition"
            ) { confirming ->
                if (confirming) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "确定删除该资产？此操作不可撤销。"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { isDeleteConfirming = false }
                            ) {
                                Text("取消")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onDelete(asset.id) },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("确认删除")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { isDeleteConfirming = true },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("删除")
                        }
                        TextButton(
                            onClick = {
                                onConfirm(asset.id, nameInput, amountInput, selectedType)
                            },
                            enabled = nameInput.isNotBlank() && isAmountValid(amountInput)
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}
