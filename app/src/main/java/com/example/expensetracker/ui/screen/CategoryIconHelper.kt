package com.example.expensetracker.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "餐饮" -> Icons.Default.Restaurant
        "交通" -> Icons.Default.Commute
        "购物" -> Icons.Default.ShoppingBag
        "娱乐" -> Icons.Default.SportsEsports
        "账单" -> Icons.Default.Receipt
        "医疗" -> Icons.Default.LocalHospital
        "教育" -> Icons.Default.School
        else -> Icons.Default.AutoAwesome
    }
}
