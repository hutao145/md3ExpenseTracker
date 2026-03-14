package com.example.expensetracker.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.drawText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.model.CategorySummaryUiModel
import com.example.expensetracker.ui.model.DailyExpenseUiModel
import com.example.expensetracker.ui.util.formatAmount
import com.example.expensetracker.ui.viewmodel.ExpenseUiState
import kotlin.math.min
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun statisticsCategoryColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.primaryContainer,
        scheme.secondaryContainer,
        scheme.tertiaryContainer,
        scheme.surfaceVariant,
        scheme.outline,
        scheme.inversePrimary
    )
}


enum class StatsPeriod { WEEK, MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: ExpenseUiState,
    onBackClick: () -> Unit,
    onDateRangeChanged: (start: String, end: String) -> Unit
) {
    var period by remember { mutableStateOf(StatsPeriod.MONTH) }
    // Local state to manage what date we are focusing on
    var focusDate by remember { mutableStateOf(LocalDate.now(ZoneId.systemDefault())) }

    LaunchedEffect(period, focusDate) {
        val formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.CHINA)
        val (start, end) = when (period) {
            StatsPeriod.WEEK -> {
                val startOfWeek = focusDate.minusDays((focusDate.dayOfWeek.value % 7).toLong())
                val endOfWeek = startOfWeek.plusDays(6)
                startOfWeek to endOfWeek
            }
            StatsPeriod.MONTH -> {
                val startOfMonth = focusDate.withDayOfMonth(1)
                val endOfMonth = focusDate.withDayOfMonth(focusDate.lengthOfMonth())
                startOfMonth to endOfMonth
            }
            StatsPeriod.YEAR -> {
                val startOfYear = focusDate.withDayOfYear(1)
                val endOfYear = focusDate.withDayOfYear(focusDate.lengthOfYear())
                startOfYear to endOfYear
            }
        }
        onDateRangeChanged(start.format(formatter), end.format(formatter))
    }

    // Derive label based on focusDate and period
    val periodLabel = remember(period, focusDate) {
        when (period) {
            StatsPeriod.WEEK -> {
                // A very simplified week label
                val startOfWeek = focusDate.minusDays((focusDate.dayOfWeek.value % 7).toLong())
                val endOfWeek = startOfWeek.plusDays(6)
                "${startOfWeek.monthValue}月${startOfWeek.dayOfMonth}日 - ${endOfWeek.monthValue}月${endOfWeek.dayOfMonth}日"
            }
            StatsPeriod.MONTH -> "${focusDate.year}年${focusDate.monthValue}月"
            StatsPeriod.YEAR -> "${focusDate.year}年"
        }
    }

    val totalExpense = uiState.totalExpenseCent
    val totalIncome = uiState.totalIncomeCent
    val balance = totalIncome - totalExpense
    val categoryColors = statisticsCategoryColors()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = { Text("统计图表", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
                // Period selector
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = period == StatsPeriod.WEEK,
                            onClick = { period = StatsPeriod.WEEK },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) { Text("周") }
                        SegmentedButton(
                            selected = period == StatsPeriod.MONTH,
                            onClick = { period = StatsPeriod.MONTH },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) { Text("月") }
                        SegmentedButton(
                            selected = period == StatsPeriod.YEAR,
                            onClick = { period = StatsPeriod.YEAR },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) { Text("年") }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background // Light gray for Expressive look
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date Navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        focusDate = when (period) {
                            StatsPeriod.WEEK -> focusDate.minusWeeks(1)
                            StatsPeriod.MONTH -> focusDate.minusMonths(1)
                            StatsPeriod.YEAR -> focusDate.minusYears(1)
                        }
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一期")
                    }
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = {
                        focusDate = when (period) {
                            StatsPeriod.WEEK -> focusDate.plusWeeks(1)
                            StatsPeriod.MONTH -> focusDate.plusMonths(1)
                            StatsPeriod.YEAR -> focusDate.plusYears(1)
                        }
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一期")
                    }
                }
            }

            // Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("结余", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatAmount(balance),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("支出", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatAmount(totalExpense),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("收入", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatAmount(totalIncome),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            // Trends Chart
            if (uiState.dailySummaries.isNotEmpty()) {
                item {
                    Text(
                        text = "收支趋势",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            context.startActivity(Intent(context, TrendDetailActivity::class.java))
                        }) {
                            Text("横屏查看")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            TrendLineChart(dailySummaries = uiState.dailySummaries.reversed())
                        }
                    }
                }
            }

            // Donut Chart for Categories
            if (uiState.totalExpenseCent > 0 && uiState.categorySummaries.isNotEmpty()) {
                item {
                    Text(
                        text = "支出构成",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DonutChart(
                                categorySummaries = uiState.categorySummaries,
                                totalExpenseCent = uiState.totalExpenseCent,
                                categoryColors = categoryColors
                            )
                            
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.categorySummaries.take(5).forEachIndexed { index, summary ->
                                    LegendItem(
                                        summary = summary,
                                        color = categoryColors[index % categoryColors.size],
                                        totalExpenseCent = uiState.totalExpenseCent
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "当前区间暂无数据",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun LegendItem(
    summary: CategorySummaryUiModel,
    color: Color,
    totalExpenseCent: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = CircleShape)
            )
            Text(
                text = summary.category,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val percent = if (totalExpenseCent > 0) (summary.totalExpenseCent.toDouble() / totalExpenseCent.toDouble() * 100).toInt() else 0
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatAmount(summary.totalExpenseCent),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DonutChart(
    categorySummaries: List<CategorySummaryUiModel>,
    totalExpenseCent: Long,
    categoryColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(categorySummaries) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp)
        ) {
            val strokeWidth = 36.dp.toPx()
            val canvasSize = min(size.width, size.height)
            val radius = (canvasSize - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f

            categorySummaries.forEachIndexed { index, summary ->
                val sweepAngle = (summary.totalExpenseCent.toFloat() / totalExpenseCent.toFloat()) * 360f
                val color = categoryColors[index % categoryColors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animationProgress.value,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                startAngle += sweepAngle
            }
        }

        val amountText = formatAmount(totalExpenseCent)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            Text(
                text = "总支出",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountText,
                fontSize = when {
                    amountText.length > 9 -> 14.sp
                    amountText.length > 7 -> 18.sp
                    else -> 24.sp
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TrendLineChart(
    dailySummaries: List<DailyExpenseUiModel>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    LaunchedEffect(dailySummaries) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    if (dailySummaries.isEmpty()) return

    val maxVal = dailySummaries.maxOfOrNull { maxOf(it.totalExpenseCent, it.totalIncomeCent) } ?: 0L
    if (maxVal == 0L) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val labelPadding = 24.dp.toPx()
            val canvasHeight = height - labelPadding

            val xStep = width / (dailySummaries.size - 1).coerceAtLeast(1)
            
            val expensePath = Path()
            val incomePath = Path()
            
            val expensePoints = mutableListOf<Offset>()
            val incomePoints = mutableListOf<Offset>()

            dailySummaries.forEachIndexed { index, summary ->
                val x = index * xStep
                
                // Height calculated from bottom up
                val expenseY = canvasHeight - ((summary.totalExpenseCent.toFloat() / maxVal.toFloat()) * canvasHeight * animationProgress.value)
                val incomeY = canvasHeight - ((summary.totalIncomeCent.toFloat() / maxVal.toFloat()) * canvasHeight * animationProgress.value)
                
                val ePoint = Offset(x, expenseY)
                val iPoint = Offset(x, incomeY)
                
                expensePoints.add(ePoint)
                incomePoints.add(iPoint)
                
                if (index == 0) {
                    expensePath.moveTo(ePoint.x, ePoint.y)
                    incomePath.moveTo(iPoint.x, iPoint.y)
                } else {
                    // Draw smooth curve using cubicTo
                    val prevE = expensePoints[index - 1]
                    val prevI = incomePoints[index - 1]
                    
                    val controlX = prevE.x + (ePoint.x - prevE.x) / 2
                    expensePath.cubicTo(controlX, prevE.y, controlX, ePoint.y, ePoint.x, ePoint.y)
                    incomePath.cubicTo(controlX, prevI.y, controlX, iPoint.y, iPoint.x, iPoint.y)
                }
                
                // Draw Date Labels sparsely
                if (index % maxOf(1, dailySummaries.size / 5) == 0 || index == dailySummaries.size - 1) {
                    val dateParts = summary.date.split("-")
                    val dayStr = if (dateParts.size == 3) dateParts[2].toIntOrNull()?.toString() ?: dateParts[2] else summary.date
                    
                    val textLayout = textMeasurer.measure(
                        text = dayStr,
                        style = TextStyle(fontSize = 10.sp, color = axisLabelColor, textAlign = TextAlign.Center)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(x - textLayout.size.width / 2, canvasHeight + 8.dp.toPx())
                    )
                }
            }

            // Draw Grid & Y-axis labels
            val gridCount = 3
            val stepVal = maxVal.toFloat() / gridCount
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            for (i in 0..gridCount) {
                val value = stepVal * i
                val y = canvasHeight - (value / maxVal.toFloat()) * canvasHeight
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                val label = String.format("%.0f", value / 100.0f)
                val labelLayout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 10.sp, color = axisLabelColor, textAlign = TextAlign.End)
                )
                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(0f, y - labelLayout.size.height / 2)
                )
            }

            // Draw Paths
            drawPath(
                path = expensePath,
                color = Color(0xFFF44336),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            
            drawPath(
                path = incomePath,
                color = Color(0xFF4CAF50),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f))
            )
            
            // Draw Gradient Fills
            val expenseFillPath = Path().apply {
                addPath(expensePath)
                lineTo(width, canvasHeight)
                lineTo(0f, canvasHeight)
                close()
            }
            drawPath(
                path = expenseFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF44336).copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f,
                    endY = canvasHeight
                ),
                style = Fill
            )

            val incomeFillPath = Path().apply {
                addPath(incomePath)
                lineTo(width, canvasHeight)
                lineTo(0f, canvasHeight)
                close()
            }
            drawPath(
                path = incomeFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f,
                    endY = canvasHeight
                ),
                style = Fill
            )
        }
    }
}
