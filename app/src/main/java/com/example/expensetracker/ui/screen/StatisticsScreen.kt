package com.example.expensetracker.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.model.CategorySummaryUiModel
import com.example.expensetracker.ui.model.DailyExpenseUiModel
import com.example.expensetracker.ui.viewmodel.ExpenseUiState
import kotlin.math.min
import java.util.Locale

val CategoryColors = listOf(
    Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFB74D),
    Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF4DB6AC),
    Color(0xFFFFD54F), Color(0xFFA1887F), Color(0xFF90A4AE)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: ExpenseUiState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计图表") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = uiState.currentMonthLabel + " 支出构成",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.totalExpenseCent > 0 && uiState.categorySummaries.isNotEmpty()) {
                item {
                    DonutChart(
                        categorySummaries = uiState.categorySummaries,
                        totalExpenseCent = uiState.totalExpenseCent
                    )
                }

                itemsIndexed(uiState.categorySummaries) { index, summary ->
                    LegendItem(
                        summary = summary,
                        color = CategoryColors[index % CategoryColors.size],
                        totalExpenseCent = uiState.totalExpenseCent
                    )
                }
                
                if (uiState.dailySummaries.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "每日支出趋势",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }

                    item {
                        // Reverse the list before passing to BarChart 
                        // so that dates are drawn chronologically (left: early, right: late)
                        BarChart(dailySummaries = uiState.dailySummaries.reversed())
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "本月暂无支出数据",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(color, shape = CircleShape)
            )
            Text(
                text = summary.category,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatAmountChart(summary.totalExpenseCent),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            val percent = (summary.totalExpenseCent.toDouble() / totalExpenseCent.toDouble() * 100).toInt()
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DonutChart(
    categorySummaries: List<CategorySummaryUiModel>,
    totalExpenseCent: Long,
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
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp)
        ) {
            val strokeWidth = 40.dp.toPx()
            val canvasSize = min(size.width, size.height)
            val radius = (canvasSize - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f // Start from top

            categorySummaries.forEachIndexed { index, summary ->
                val sweepAngle = (summary.totalExpenseCent.toFloat() / totalExpenseCent.toFloat()) * 360f
                val color = CategoryColors[index % CategoryColors.size]

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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "总支出",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatAmountChart(totalExpenseCent),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatAmountChart(amountCent: Long): String {
    val amount = amountCent / 100.0
    return String.format(Locale.US, "%.2f", amount)
}

@Composable
fun BarChart(
    dailySummaries: List<DailyExpenseUiModel>,
    modifier: Modifier = Modifier
) {
    val maxExpense = dailySummaries.maxOfOrNull { it.totalExpenseCent } ?: 0L
    val animationProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    LaunchedEffect(dailySummaries) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    if (maxExpense == 0L) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp) // Increased height slightly to accommodate labels
            .padding(vertical = 16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val labelPadding = 24.dp.toPx()
            val maxBarHeight = size.height - labelPadding
            
            val barWidth = size.width / (dailySummaries.size * 1.5f).coerceAtLeast(1f)
            val barSpacing = if (dailySummaries.size > 1) {
                (size.width - (barWidth * dailySummaries.size)) / (dailySummaries.size - 1)
            } else {
                0f
            }

            dailySummaries.forEachIndexed { index, summary ->
                val barHeight = if (maxExpense > 0) {
                    (summary.totalExpenseCent.toFloat() / maxExpense.toFloat()) * maxBarHeight * animationProgress.value
                } else {
                    0f
                }

                val xOffset = index * (barWidth + barSpacing)
                val yOffset = maxBarHeight - barHeight

                drawRoundRect(
                    color = Color(0xFF5C6BC0), // Material Indigo 400
                    topLeft = Offset(xOffset, yOffset),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Draw Date Label
                val dateParts = summary.date.split("-")
                val dayStr = if (dateParts.size == 3) {
                    // Try to remove leading zero by parsing to int, fallback to original if it fails
                    dateParts[2].toIntOrNull()?.toString() ?: dateParts[2]
                } else summary.date
                
                val textLayoutResult = textMeasurer.measure(
                    text = dayStr,
                    style = labelStyle
                )
                
                val textXOffset = xOffset + (barWidth / 2) - (textLayoutResult.size.width / 2)
                val textYOffset = maxBarHeight + 8.dp.toPx()
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(textXOffset, textYOffset)
                )
            }
        }
    }
}
