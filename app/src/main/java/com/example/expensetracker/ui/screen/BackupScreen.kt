package com.example.expensetracker.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.expensetracker.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: ExpenseViewModel,
    onBackClick: () -> Unit,
    onExportUri: (Uri) -> Unit,
    onImportUri: (Uri) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            onExportUri(uri)
            Toast.makeText(context, "正在导出数据...", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportUri(uri)
            Toast.makeText(context, "正在导入数据...", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份与恢复") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "WebDAV") },
                    label = { Text("WebDAV") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Save, contentDescription = "本地") },
                    label = { Text("本地") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(animationSpec = tween(300)) { it * direction } + fadeIn(tween(300)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { it * -direction } + fadeOut(tween(200)))
                },
                label = "TabSwitchAnimation"
            ) { tab ->
                when (tab) {
                    0 -> WebDavBackupTab(viewModel)
                    1 -> LocalBackupTab(exportLauncher, importLauncher)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavBackupTab(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val webDavState by viewModel.webDavState.collectAsState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(webDavState.message) {
        webDavState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearWebDavMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 配置卡片
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = webDavState.url,
                    onValueChange = { viewModel.updateWebDavUrl(it) },
                    label = { Text("WebDAV 服务器地址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = webDavState.username,
                    onValueChange = { viewModel.updateWebDavUsername(it) },
                    label = { Text("用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = webDavState.password,
                    onValueChange = { viewModel.updateWebDavPassword(it) },
                    label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, "Toggle password visibility")
                        }
                    }
                )

                OutlinedTextField(
                    value = webDavState.path,
                    onValueChange = { viewModel.updateWebDavPath(it) },
                    label = { Text("路径 (含文件名)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // 备份项目卡片
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("备份项目", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("记账数据") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = false,
                        onClick = { },
                        label = { Text("文件 (敬请期待)") },
                        modifier = Modifier.weight(1f),
                        enabled = false
                    )
                }
            }
        }

        // 记账后自动网络备份
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "记账后自动网络备份",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "每次记账/自动记账后上传到 WebDAV，仅保留最新 3 份",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.autoWebDavBackupOnEntryEnabled,
                    onCheckedChange = { viewModel.updateAutoWebDavBackupOnEntryEnabled(it) }
                )
            }
        }

        // 按钮区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.testWebDavConnection() },
                enabled = !webDavState.isTesting,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = if (webDavState.isTesting) "测试..." else "测试连接",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            OutlinedButton(
                onClick = { viewModel.fetchWebDavFileList() },
                enabled = !webDavState.isFetchingFiles,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = if (webDavState.isFetchingFiles) "获取..." else "恢复",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
                
            Button(
                onClick = { viewModel.backupToWebDav(context) },
                enabled = !webDavState.isBackingUp,
                modifier = Modifier.weight(1.3f),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (webDavState.isBackingUp) "备份中..." else "立即备份",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // 定时备份卡片
        ScheduledBackupCard(viewModel)
    }

    // 恢复列表弹窗
    if (webDavState.showRestoreDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "WebDav 备份文件列表",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                AnimatedContent(
                    targetState = when {
                        webDavState.isFetchingFiles -> 0
                        webDavState.fileList.isEmpty() -> 1
                        else -> 2
                    },
                    label = "FileListStateAnimation"
                ) { targetState ->
                    when (targetState) {
                        0 -> {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        1 -> {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("没有找到备份文件", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        2 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = webDavState.fileList,
                                    key = { it.name }
                                ) { fileItem ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = fileItem.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val dateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA)
                                                val sizeMB = String.format(Locale.getDefault(), "%.2f MB", fileItem.size / (1024f * 1024f))
                                                Text(
                                                    text = "${dateFormat.format(Date(fileItem.dateModified))}  $sizeMB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { viewModel.deleteWebDavFile(fileItem.name) },
                                                    enabled = !webDavState.isDeletingFile,
                                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text(if (webDavState.isDeletingFile) "删除中..." else "删除")
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { viewModel.restoreFromWebDav(context, fileItem.name) },
                                                    enabled = !webDavState.isRestoring
                                                ) {
                                                    Text(if (webDavState.isRestoring) "恢复中..." else "恢复")
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // spacer for bottom edge
                                item { Spacer(modifier = Modifier.height(32.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalBackupTab(
    exportLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("本地备份", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("将数据导出为 CSV 文件保存在本地，或者从本地 CSV 文件恢复数据。", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("expenses_backup_$timestamp.csv")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("导出数据 (CSV)")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv", "text/plain"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("导入数据 (CSV)")
                }
            }
        }
    }
}

@Composable
fun ScheduledBackupCard(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val webDavState by viewModel.webDavState.collectAsState()

    val webDavConfigured = webDavState.url.isNotBlank()
        && webDavState.username.isNotBlank()
        && webDavState.password.isNotBlank()
        && webDavState.path.isNotBlank()

    // Refresh last backup time when this card is shown
    LaunchedEffect(Unit) { viewModel.refreshLastAutoBackupTime() }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("定时备份", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (webDavConfigured) "自动备份到本地 + WebDAV" else "请先配置 WebDAV 以启用云端备份",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.autoBackupEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateAutoBackupEnabled(context, enabled)
                        val msg = if (enabled) "定时备份已开启" else "定时备份已关闭"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Interval slider (animated expand/collapse)
            AnimatedVisibility(
                visible = uiState.autoBackupEnabled,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "备份间隔: ${uiState.autoBackupIntervalHours} 小时",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))

                    var sliderValue by remember(uiState.autoBackupIntervalHours) {
                        mutableFloatStateOf(uiState.autoBackupIntervalHours.toFloat())
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.updateAutoBackupInterval(context, sliderValue.roundToInt())
                        },
                        valueRange = 1f..72f,
                        steps = 70,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("72h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Last backup time
                    if (uiState.lastAutoBackupTime > 0L) {
                        Spacer(Modifier.height(8.dp))
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        Text(
                            text = "上次自动备份: ${dateFormat.format(Date(uiState.lastAutoBackupTime))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
