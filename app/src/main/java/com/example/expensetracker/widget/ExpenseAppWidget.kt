package com.example.expensetracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.data.local.ExpenseDatabase
import com.example.expensetracker.ui.util.formatAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class ExpenseAppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data in IO dispatcher
        val totalExpenseCent = withContext(Dispatchers.IO) {
            val db = ExpenseDatabase.getInstance(context)
            val allExpenses = db.expenseDao().getAllExpensesSnapshot()
            
            val today = LocalDate.now(ZoneId.systemDefault())
            
            // Filter this month's expenses
            val thisMonthExpenses = allExpenses.filter { entity ->
                val date = java.time.Instant.ofEpochMilli(entity.createdAtEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                date.year == today.year && date.month == today.month && entity.type == 0
            }
            
            thisMonthExpenses.sumOf { it.amountCent }
        }

        provideContent {
            ExpenseWidgetContent(totalExpenseCent)
        }
    }

    @Composable
    private fun ExpenseWidgetContent(totalExpenseCent: Long) {
        val formattedAmount = formatAmount(totalExpenseCent)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月支出",
                    style = TextStyle(
                        fontStyle = androidx.glance.text.FontStyle.Normal,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                // Add button
                val intent = Intent(
                    androidx.glance.LocalContext.current, 
                    MainActivity::class.java
                ).apply {
                    action = "com.example.expensetracker.ACTION_ADD_EXPENSE"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = "记账",
                    modifier = GlanceModifier
                        .size(32.dp)
                        .clickable(actionStartActivity(intent))
                )
            }
            
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            Text(
                text = "¥ $formattedAmount",
                style = TextStyle(
                    fontStyle = androidx.glance.text.FontStyle.Normal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
            
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}
