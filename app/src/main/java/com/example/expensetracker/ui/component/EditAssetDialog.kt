package com.example.expensetracker.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.local.AssetEntity

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
    
    // 初始化金额为浮点数字符串
    val initialAmount = java.math.BigDecimal(asset.amountCent)
        .divide(java.math.BigDecimal(100))
        .toPlainString()
    var amountInput by remember { mutableStateOf(initialAmount) }
    
    var selectedType by remember { mutableStateOf(asset.type) } // 0:资产, 1:负债, 2:借出

    val assetTypes = listOf("资产", "负债", "借出")

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "编辑资产") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(asset.id, nameInput, amountInput, selectedType)
                },
                enabled = nameInput.isNotBlank() && isAmountValid(amountInput)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDelete(asset.id) },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        }
    )
}
