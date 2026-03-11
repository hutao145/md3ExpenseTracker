package com.example.expensetracker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.local.AssetEntity
import com.example.expensetracker.ui.component.AddAssetDialog
import com.example.expensetracker.ui.component.EditAssetDialog
import com.example.expensetracker.ui.viewmodel.ExpenseViewModel

@Composable
fun AssetScreen(
    viewModel: ExpenseViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<AssetEntity?>(null) }

    val assets = uiState.assets
    val regularAssets = assets.filter { it.type == 0 }
    val liabilities = assets.filter { it.type == 1 }
    val lentils = assets.filter { it.type == 2 }

    val totalAssetsCent = regularAssets.sumOf { it.amountCent }
    val totalLiabilitiesCent = liabilities.sumOf { it.amountCent }
    val totalLentCent = lentils.sumOf { it.amountCent }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Text(
                text = "资产",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AssetSummaryCard(
                        totalAssetsCent = totalAssetsCent,
                        totalLiabilitiesCent = totalLiabilitiesCent,
                        totalLentCent = totalLentCent
                    )
                }

                item {
                    Text(
                        text = "我的资产",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                items(assets) { asset ->
                    AssetItemCard(
                        asset = asset,
                        onClick = { editingAsset = asset }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 24.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                ),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加资产")
        }
    }

    if (showAddDialog) {
        AddAssetDialog(
            isAmountValid = viewModel::isAmountValid,
            onDismissRequest = { showAddDialog = false },
            onConfirm = { name, amount, type ->
                viewModel.addAsset(name, amount, type)
                showAddDialog = false
            }
        )
    }

    editingAsset?.let { asset ->
        EditAssetDialog(
            asset = asset,
            isAmountValid = viewModel::isAmountValid,
            onDismissRequest = { editingAsset = null },
            onConfirm = { id, name, amount, type ->
                viewModel.updateAsset(id, name, amount, type)
                editingAsset = null
            },
            onDelete = { id ->
                viewModel.deleteAsset(id)
                editingAsset = null
            }
        )
    }
}

@Composable
fun AssetSummaryCard(
    totalAssetsCent: Long,
    totalLiabilitiesCent: Long,
    totalLentCent: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "总资产",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAmount(totalAssetsCent),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "负债",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatAmount(totalLiabilitiesCent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "借出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatAmount(totalLentCent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun AssetItemCard(
    asset: AssetEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = asset.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatAmount(asset.amountCent),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = when (asset.type) {
                    1 -> MaterialTheme.colorScheme.secondary
                    2 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

private fun formatAmount(cent: Long): String {
    val bd = java.math.BigDecimal(cent).divide(java.math.BigDecimal(100))
    return String.format(java.util.Locale.CHINA, "%.2f", bd)
}
