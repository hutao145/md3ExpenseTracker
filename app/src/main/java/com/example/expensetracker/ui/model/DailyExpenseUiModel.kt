package com.example.expensetracker.ui.model

data class ExpenseItemUiModel(
    val id: Long,
    val amountCent: Long,
    val type: Int,
    val category: String,
    val note: String,
    val assetId: Long? = null,
    val assetName: String? = null
)

data class DailyExpenseUiModel(
    val date: String,
    val totalExpenseCent: Long,
    val totalIncomeCent: Long,
    val items: List<ExpenseItemUiModel>
)

data class CategorySummaryUiModel(
    val category: String,
    val totalExpenseCent: Long,
    val count: Int
)
