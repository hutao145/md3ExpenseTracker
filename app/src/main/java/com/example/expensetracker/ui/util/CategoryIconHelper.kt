package com.example.expensetracker.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        // 支出分类
        "餐饮" -> Icons.Default.Restaurant
        "交通" -> Icons.Default.Commute
        "购物" -> Icons.Default.ShoppingBag
        "日用" -> Icons.Default.Inventory2
        "娱乐" -> Icons.Default.SportsEsports
        "住房" -> Icons.Default.Home
        "账单" -> Icons.Default.Receipt
        "医疗" -> Icons.Default.LocalHospital
        "教育" -> Icons.Default.School
        "通讯" -> Icons.Default.PhoneAndroid
        "运动" -> Icons.Default.FitnessCenter
        "旅行" -> Icons.Default.Flight
        "宠物" -> Icons.Default.Pets
        "育儿" -> Icons.Default.ChildCare
        "维修" -> Icons.Default.Handyman
        "人情" -> Icons.Default.CardGiftcard
        // 收入分类
        "薪资" -> Icons.Default.Work
        "奖金" -> Icons.Default.EmojiEvents
        "理财" -> Icons.Default.TrendingUp
        "收债" -> Icons.Default.Payments
        "副业" -> Icons.Default.Savings
        "报销" -> Icons.Default.AccountBalance
        "红包" -> Icons.Default.VolunteerActivism
        // 通用
        "自定义" -> Icons.Default.Category
        "其他" -> Icons.Default.AutoAwesome
        else -> Icons.Default.AutoAwesome
    }
}
