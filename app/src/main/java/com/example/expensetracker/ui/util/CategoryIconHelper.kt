package com.example.expensetracker.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.CurrencyYuan
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

const val CATEGORY_ICON_UNIQUE_COUNT = 64

val categoryIconPreviewCategories = listOf(
    "餐饮", "吃饭", "用餐", "饭钱", "午餐", "晚餐", "宵夜", "聚餐",
    "早餐", "咖啡", "奶茶", "饮品",
    "外卖", "零食", "快餐",
    "交通", "打车", "出租车", "网约车",
    "公交", "地铁",
    "火车", "高铁",
    "汽车", "停车", "过路费",
    "油费", "加油",
    "骑行", "电动车", "摩托",
    "购物",
    "超市", "买菜", "生鲜", "水果",
    "服饰", "衣服", "鞋包",
    "数码", "电子", "电脑",
    "日用", "家居",
    "洗护", "清洁",
    "洗衣",
    "娱乐", "游戏",
    "电影",
    "音乐",
    "运动",
    "球类",
    "住房", "房租", "租房",
    "房贷",
    "物业",
    "水费",
    "电费", "水电", "燃气",
    "账单", "发票", "票据",
    "信用卡", "还款",
    "医疗", "医院", "看病",
    "药品", "买药",
    "医保", "保险",
    "体检",
    "教育", "学习", "培训",
    "书籍", "图书",
    "通讯", "话费",
    "手机",
    "网费", "宽带",
    "旅行", "机票", "酒店",
    "宠物",
    "育儿", "孩子",
    "维修", "修理",
    "美容", "美发",
    "护肤", "按摩", "养生",
    "礼物", "人情", "礼金",
    "订阅", "会员",
    "捐赠", "公益",
    "奢侈品", "首饰",
    "薪资", "工资",
    "奖金", "绩效",
    "理财", "投资", "基金", "股票",
    "收益", "分红",
    "利息",
    "收债", "还钱", "收款",
    "副业", "兼职",
    "报销",
    "红包",
    "退款", "返现",
    "补贴", "津贴",
    "现金",
    "钱包", "余额",
    "人民币",
    "手续费", "税费",
    "自定义",
    "其他"
)

@Composable
fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        // 支出分类
        "餐饮", "吃饭", "用餐", "饭钱", "午餐", "晚餐", "宵夜", "聚餐" -> Icons.Default.Restaurant
        "早餐", "咖啡", "奶茶", "饮品" -> Icons.Default.Coffee
        "外卖", "零食", "快餐" -> Icons.Default.Fastfood
        "交通", "打车", "出租车", "网约车" -> Icons.Default.Commute
        "公交", "地铁" -> Icons.Default.DirectionsBus
        "火车", "高铁" -> Icons.Default.Train
        "汽车", "停车", "过路费" -> Icons.Default.DirectionsCar
        "油费", "加油" -> Icons.Default.LocalGasStation
        "骑行", "电动车", "摩托" -> Icons.Default.TwoWheeler
        "购物" -> Icons.Default.ShoppingBag
        "超市", "买菜", "生鲜", "水果" -> Icons.Default.LocalGroceryStore
        "服饰", "衣服", "鞋包" -> Icons.Default.Checkroom
        "数码", "电子", "电脑" -> Icons.Default.LaptopChromebook
        "日用", "家居" -> Icons.Default.Inventory2
        "洗护", "清洁" -> Icons.Default.CleaningServices
        "洗衣" -> Icons.Default.LocalLaundryService
        "娱乐", "游戏" -> Icons.Default.SportsEsports
        "电影" -> Icons.Default.Movie
        "音乐" -> Icons.Default.MusicNote
        "运动" -> Icons.Default.FitnessCenter
        "球类" -> Icons.Default.SportsBasketball
        "住房", "房租", "租房" -> Icons.Default.Home
        "房贷" -> Icons.Default.House
        "物业" -> Icons.Default.HomeWork
        "水费" -> Icons.Default.WaterDrop
        "电费", "水电", "燃气" -> Icons.Default.ElectricalServices
        "账单", "发票", "票据" -> Icons.Default.Receipt
        "信用卡", "还款" -> Icons.Default.CreditCard
        "医疗", "医院", "看病" -> Icons.Default.LocalHospital
        "药品", "买药" -> Icons.Default.Medication
        "医保", "保险" -> Icons.Default.HealthAndSafety
        "体检" -> Icons.Default.MonitorHeart
        "教育", "学习", "培训" -> Icons.Default.School
        "书籍", "图书" -> Icons.AutoMirrored.Filled.MenuBook
        "通讯", "话费" -> Icons.Default.PhoneAndroid
        "手机" -> Icons.Default.Smartphone
        "网费", "宽带" -> Icons.Default.Wifi
        "旅行", "机票", "酒店" -> Icons.Default.Flight
        "宠物" -> Icons.Default.Pets
        "育儿", "孩子" -> Icons.Default.ChildCare
        "维修", "修理" -> Icons.Default.Handyman
        "美容", "美发" -> Icons.Default.ContentCut
        "护肤", "按摩", "养生" -> Icons.Default.Spa
        "礼物", "人情", "礼金" -> Icons.Default.CardGiftcard
        "订阅", "会员" -> Icons.AutoMirrored.Filled.ReceiptLong
        "捐赠", "公益" -> Icons.Default.VolunteerActivism
        "奢侈品", "首饰" -> Icons.Default.Diamond
        // 收入分类
        "薪资", "工资" -> Icons.Default.Work
        "奖金", "绩效" -> Icons.Default.EmojiEvents
        "理财", "投资", "基金", "股票" -> Icons.AutoMirrored.Filled.ShowChart
        "收益", "分红" -> Icons.Default.QueryStats
        "利息" -> Icons.Default.Savings
        "收债", "还钱", "收款" -> Icons.Default.Payments
        "副业", "兼职" -> Icons.Default.Storefront
        "报销" -> Icons.Default.AccountBalance
        "红包" -> Icons.Default.Redeem
        "退款", "返现" -> Icons.Default.CurrencyExchange
        "补贴", "津贴" -> Icons.Default.RequestQuote
        "现金" -> Icons.Default.Paid
        "钱包", "余额" -> Icons.Default.AccountBalanceWallet
        "人民币" -> Icons.Default.CurrencyYuan
        "手续费", "税费" -> Icons.Default.Toll
        // 通用
        "自定义" -> Icons.Default.Category
        "其他" -> Icons.Default.AutoAwesome
        else -> Icons.Default.AutoAwesome
    }
}
