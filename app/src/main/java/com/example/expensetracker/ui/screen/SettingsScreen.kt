package com.example.expensetracker.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.expensetracker.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: com.example.expensetracker.ui.viewmodel.ExpenseUiState,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeColorChange: (String) -> Unit,
    onAmoledDarkModeChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onBackupClick: () -> Unit,
    onGenerateTestData: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "备份与恢复",
                subtitle = "使用 WebDAV 或本地存储进行数据备份与恢复",
                onClick = onBackupClick
            )
            HorizontalDivider()
            
            // Theme and Appearance header
            Text(
                text = "主题设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                // Dynamic Color Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "动态颜色",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "是否使用动态颜色",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.dynamicColorEnabled,
                        onCheckedChange = { onDynamicColorChange(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))

                // Theme preset palettes
                AnimatedVisibility(
                    visible = !uiState.dynamicColorEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val palettes = listOf(
                            ThemePaletteConfig("Pink", "樱花粉", PrimaryPink, PrimaryContainerPink, SecondaryContainerPink, TertiaryContainerPink, SecondaryPink),
                            ThemePaletteConfig("Gulf", "海湾蓝", PrimaryGulf, PrimaryContainerGulf, SecondaryContainerGulf, TertiaryContainerGulf, SecondaryGulf),
                            ThemePaletteConfig("Field", "原野绿", PrimaryField, PrimaryContainerField, SecondaryContainerField, TertiaryContainerField, SecondaryField),
                            ThemePaletteConfig("Autumn", "秋黄", PrimaryAutumn, PrimaryContainerAutumn, SecondaryContainerAutumn, TertiaryContainerAutumn, SecondaryAutumn),
                            ThemePaletteConfig("Neutral", "中性黑", PrimaryNeutral, PrimaryContainerNeutral, SecondaryContainerNeutral, TertiaryContainerNeutral, SecondaryNeutral)
                        )
                        items(palettes) { palette ->
                            ThemeBadge(
                                config = palette,
                                isSelected = uiState.themeColor == palette.id,
                                onClick = { onThemeColorChange(palette.id) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))

                // AMOLED Dark Mode Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AMOLED 暗色模式",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "在暗色主题中使用纯黑背景，适合 AMOLED 屏幕",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.amoledDarkModeEnabled,
                        onCheckedChange = { onAmoledDarkModeChange(it) }
                    )
                }
            }

            // Advanced options header
            Text(
                text = "高级与测试",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
            
            SettingsItem(
                icon = Icons.Default.Science,
                title = "生成测试数据 (15条)",
                subtitle = "仅供调试。为当前月份随机生成测试用的收支记录",
                onClick = {
                    onGenerateTestData()
                    Toast.makeText(context, "已成功生成本月测试数据！", Toast.LENGTH_SHORT).show()
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)

        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ThemePaletteConfig(
    val id: String,
    val label: String,
    val topLeft: Color,
    val topRight: Color,
    val bottomLeft: Color,
    val bottomRight: Color,
    val center: Color
)

@Composable
fun ThemeBadge(config: ThemePaletteConfig, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                // Top Right
                drawArc(
                    color = config.topRight,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f)
                )
                // Bottom Right
                drawArc(
                    color = config.bottomRight,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f)
                )
                // Bottom Left
                drawArc(
                    color = config.bottomLeft,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f)
                )
                // Top Left
                drawArc(
                    color = config.topLeft,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f)
                )
                // Center Circle
                drawCircle(
                    color = config.center,
                    radius = radius * 0.4f
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        .padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = config.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
