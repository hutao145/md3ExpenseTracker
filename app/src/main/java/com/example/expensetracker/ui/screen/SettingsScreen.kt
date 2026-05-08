package com.example.expensetracker.ui.screen

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import android.content.SharedPreferences
import com.example.expensetracker.security.BiometricHelper
import com.example.expensetracker.security.PinManager
import com.example.expensetracker.ui.component.SetPinDialog
import com.example.expensetracker.ui.component.VerifyPinDialog
import com.example.expensetracker.theme.*
import com.example.expensetracker.ui.viewmodel.AiConfigState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.heightIn
import com.example.expensetracker.ui.util.CATEGORY_ICON_UNIQUE_COUNT
import com.example.expensetracker.ui.util.categoryIconPreviewCategories
import com.example.expensetracker.ui.util.getCategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: com.example.expensetracker.ui.viewmodel.ExpenseUiState,
    aiConfigState: AiConfigState,
    sharedPreferences: SharedPreferences,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeColorChange: (String) -> Unit,
    onAmoledDarkModeChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onBiometricUnlockChange: (Boolean) -> Unit,
    onAssetPageChange: (Boolean) -> Unit,
    onAutoSyncOnForegroundChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAiAnalysisClick: () -> Unit,
    onAiBaseUrlChange: (String) -> Unit,
    onAiApiKeyChange: (String) -> Unit,
    onAiModelChange: (String) -> Unit,
    onFetchAiModels: () -> Unit,
    onTestAiConnection: () -> Unit,
    onClearAiTestMessage: () -> Unit,
    onAutoTrackLogClick: () -> Unit,
    onGenerateTestData: () -> Unit
) {
    val context = LocalContext.current
    var showCategoryIconPreview by remember { mutableStateOf(false) }
    val autoTrackLogEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("autotrack_in_app_log_enabled", true))
    }
    var showAiConfig by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showVerifyPinForDisable by remember { mutableStateOf(false) }
    var showVerifyPinForChange by remember { mutableStateOf(false) }
    var showSetNewPinAfterVerify by remember { mutableStateOf(false) }
    val biometricAvailable = BiometricHelper.canAuthenticate(context)

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
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "自动记账", topPadding = 16.dp) {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "无障碍自动记账",
                    subtitle = "开启 MD3记账本 服务后，可识别微信和支付宝支付页面并自动入账",
                    onClick = {
                        context.startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                SettingsDivider()

                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "支付通知自动记账",
                    subtitle = "无需 root。开启通知使用权后，可从微信/支付宝等支付通知识别金额并入账",
                    onClick = {
                        context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Icons.Default.Sync,
                    title = "前台自动同步账单",
                    subtitle = "应用回到前台时自动从 AutoAccounting 拉取本机账单",
                    checked = uiState.autoSyncOnForegroundEnabled,
                    onCheckedChange = onAutoSyncOnForegroundChange
                )

                SettingsDivider()

                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "自动记账日志",
                    subtitle = if (autoTrackLogEnabled) "查看、复制或清空无障碍页面日志" else "日志记录已关闭",
                    onClick = onAutoTrackLogClick
                )
            }

            SettingsSection(title = "数据与页面") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "备份与恢复",
                    subtitle = "使用 WebDAV 或本地存储进行数据备份与恢复",
                    onClick = onBackupClick
                )

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Icons.Default.AccountBalance,
                    title = "资产页面",
                    subtitle = "在主界面显示资产管理页面",
                    checked = uiState.assetPageEnabled,
                    onCheckedChange = onAssetPageChange
                )
            }

            SettingsSection(title = "AI 功能") {
                SettingsItem(
                    icon = Icons.Default.Psychology,
                    title = "AI 财务分析",
                    subtitle = "使用 AI 分析收支数据并生成可视化报告",
                    onClick = onAiAnalysisClick
                )

                SettingsDivider()

                SettingsItem(
                    icon = Icons.Default.Psychology,
                    title = "AI API 配置",
                    subtitle = if (showAiConfig) "收起配置项" else "配置 API 域名、密钥和模型",
                    onClick = { showAiConfig = !showAiConfig }
                )
                AnimatedVisibility(
                    visible = showAiConfig,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    var showApiKey by remember { mutableStateOf(false) }
                    var modelsExpanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = aiConfigState.baseUrl,
                            onValueChange = { onAiBaseUrlChange(it) },
                            label = { Text("API 域名 / 基础地址") },
                            supportingText = {
                                Text("只需填写域名，系统会自动补全 /v1/chat/completions 和 /v1/models")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = aiConfigState.apiKey,
                            onValueChange = { onAiApiKeyChange(it) },
                            label = { Text("API Key") },
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None
                                else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = if (showApiKey) "隐藏" else "显示"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = aiConfigState.model,
                                onValueChange = {
                                    onAiModelChange(it)
                                    onClearAiTestMessage()
                                    modelsExpanded = false
                                },
                                label = { Text("模型名称") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (aiConfigState.availableModels.isNotEmpty()) {
                                                modelsExpanded = !modelsExpanded
                                            }
                                        }
                                    ) {
                                        Text(if (modelsExpanded && aiConfigState.availableModels.isNotEmpty()) "▲" else "▼")
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = modelsExpanded && aiConfigState.availableModels.isNotEmpty(),
                                onDismissRequest = { modelsExpanded = false }
                            ) {
                                aiConfigState.availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            onAiModelChange(model)
                                            onClearAiTestMessage()
                                            modelsExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    modelsExpanded = false
                                    onFetchAiModels()
                                },
                                enabled = !aiConfigState.isFetchingModels,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (aiConfigState.isFetchingModels) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("拉取模型")
                                }
                            }

                            Button(
                                onClick = {
                                    modelsExpanded = false
                                    onTestAiConnection()
                                },
                                enabled = !aiConfigState.isTestingAiConnection,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (aiConfigState.isTestingAiConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("测试连通性")
                                }
                            }
                        }

                        if (aiConfigState.availableModels.isNotEmpty()) {
                            Text(
                                text = "已加载 ${aiConfigState.availableModels.size} 个模型，可下拉选择，也可手动输入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        aiConfigState.aiTestMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (aiConfigState.isAiTestError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }

            SettingsSection(title = "隐私与安全") {
                SettingsSwitchItem(
                    icon = Icons.Default.Lock,
                    title = "应用锁",
                    subtitle = "启用后每次打开应用都需要验证",
                    checked = uiState.appLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showSetPinDialog = true
                        } else {
                            showVerifyPinForDisable = true
                        }
                    }
                )

                AnimatedVisibility(visible = uiState.appLockEnabled) {
                    Column {
                        SettingsDivider()
                        SettingsItem(
                            icon = Icons.Default.Password,
                            title = "修改 PIN 密码",
                            subtitle = "修改应用锁 PIN 密码",
                            onClick = { showVerifyPinForChange = true }
                        )
                    }
                }

                AnimatedVisibility(visible = uiState.appLockEnabled && biometricAvailable) {
                    Column {
                        SettingsDivider()
                        SettingsSwitchItem(
                            icon = Icons.Default.Fingerprint,
                            title = "生物识别解锁",
                            subtitle = "使用指纹或面容识别解锁",
                            checked = uiState.biometricUnlockEnabled,
                            onCheckedChange = onBiometricUnlockChange
                        )
                    }
                }
            }

            if (showSetPinDialog) {
                SetPinDialog(
                    sharedPreferences = sharedPreferences,
                    onDismissRequest = { showSetPinDialog = false },
                    onPinSet = {
                        showSetPinDialog = false
                        onAppLockChange(true)
                        Toast.makeText(context, "应用锁已启用", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (showVerifyPinForDisable) {
                VerifyPinDialog(
                    sharedPreferences = sharedPreferences,
                    title = "关闭应用锁",
                    onDismissRequest = { showVerifyPinForDisable = false },
                    onVerified = {
                        showVerifyPinForDisable = false
                        onAppLockChange(false)
                        onBiometricUnlockChange(false)
                        PinManager.clearPin(sharedPreferences)
                        Toast.makeText(context, "应用锁已关闭", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (showVerifyPinForChange) {
                VerifyPinDialog(
                    sharedPreferences = sharedPreferences,
                    title = "验证当前密码",
                    onDismissRequest = { showVerifyPinForChange = false },
                    onVerified = {
                        showVerifyPinForChange = false
                        showSetNewPinAfterVerify = true
                    }
                )
            }

            if (showSetNewPinAfterVerify) {
                SetPinDialog(
                    sharedPreferences = sharedPreferences,
                    onDismissRequest = { showSetNewPinAfterVerify = false },
                    onPinSet = {
                        showSetNewPinAfterVerify = false
                        Toast.makeText(context, "PIN 密码已更新", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            SettingsSection(title = "外观") {
                SettingsSwitchItem(
                    icon = Icons.Default.Settings,
                    title = "动态配色",
                    subtitle = "跟随系统壁纸生成 Material You 配色",
                    checked = uiState.dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChange
                )

                SettingsDivider()

                AnimatedVisibility(
                    visible = !uiState.dynamicColorEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
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
                                ThemePaletteConfig("Autumn", "秋叶黄", PrimaryAutumn, PrimaryContainerAutumn, SecondaryContainerAutumn, TertiaryContainerAutumn, SecondaryAutumn),
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }

                SettingsDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "主题模式",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "系统 / 浅色 / 深色",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    var expanded by remember { mutableStateOf(false) }
                    val options = listOf(
                        "system" to "系统",
                        "light" to "浅色",
                        "dark" to "深色"
                    )
                    val selectedLabel = options.firstOrNull { it.first == uiState.themeMode }?.second ?: "系统"


                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .width(140.dp)
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            options.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        expanded = false
                                        onThemeModeChange(value)
                                    }
                                )
                            }
                        }
                    }
                }

                SettingsDivider()

                SettingsSwitchItem(
                    icon = Icons.Default.Settings,
                    title = "AMOLED 暗色模式",
                    subtitle = "在暗色主题中使用纯黑背景，更适合 AMOLED 屏幕",
                    checked = uiState.amoledDarkModeEnabled,
                    onCheckedChange = onAmoledDarkModeChange
                )
            }

            SettingsSection(title = "开发与测试") {
                SettingsItem(
                    icon = Icons.Default.Category,
                    title = "分类图标预览",
                    subtitle = "一次性查看 $CATEGORY_ICON_UNIQUE_COUNT 个图标和 ${categoryIconPreviewCategories.size} 个分类关键词",
                    onClick = { showCategoryIconPreview = true }
                )

                SettingsDivider()

                SettingsItem(
                    icon = Icons.Default.Science,
                    title = "生成测试数据（15 条）",
                    subtitle = "仅供调试。为当前月份随机生成测试用的收支记录",
                    onClick = {
                        onGenerateTestData()
                        Toast.makeText(context, "已成功生成本月测试数据！", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showCategoryIconPreview) {
        CategoryIconPreviewDialog(
            onDismissRequest = { showCategoryIconPreview = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    topPadding: androidx.compose.ui.unit.Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = topPadding, bottom = 12.dp)
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
        content()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTrackLogScreen(
    sharedPreferences: SharedPreferences,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var logText by remember {
        mutableStateOf(sharedPreferences.getString("autotrack_in_app_log_text", "") ?: "")
    }
    var enabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("autotrack_in_app_log_enabled", true))
    }

    fun refreshLog() {
        logText = sharedPreferences.getString("autotrack_in_app_log_text", "") ?: ""
        enabled = sharedPreferences.getBoolean("autotrack_in_app_log_enabled", true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自动记账日志") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "记录页面文本",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "可能包含金额、昵称等隐私信息，请按需开启",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            sharedPreferences.edit().putBoolean("autotrack_in_app_log_enabled", it).apply()
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { refreshLog() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("刷新")
                }
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("自动记账日志", logText))
                        Toast.makeText(context, "日志已复制", Toast.LENGTH_SHORT).show()
                    },
                    enabled = logText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("复制")
                }
                OutlinedButton(
                    onClick = {
                        sharedPreferences.edit().remove("autotrack_in_app_log_text").apply()
                        logText = ""
                        Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show()
                    },
                    enabled = logText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("清空")
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = if (logText.isBlank()) "暂无日志。开启后去微信/支付宝等页面停留 1-2 秒，再回到这里点刷新。" else logText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryIconPreviewDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("分类图标预览") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "$CATEGORY_ICON_UNIQUE_COUNT 个图标，${categoryIconPreviewCategories.size} 个分类关键词",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryIconPreviewCategories.chunked(2)) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { category ->
                                CategoryIconPreviewCell(
                                    category = category,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun CategoryIconPreviewCell(
    category: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getCategoryIcon(category),
            contentDescription = category,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
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
        Column(modifier = Modifier.weight(1f)) {
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
                    contentDescription = "已选中",
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


