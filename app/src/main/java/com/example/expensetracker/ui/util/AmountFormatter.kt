package com.example.expensetracker.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

private val HUNDRED = BigDecimal(100)

fun formatAmountWithSymbol(cent: Long): String {
    val bd = BigDecimal(cent).divide(HUNDRED, 2, RoundingMode.HALF_UP)
    return String.format(Locale.CHINA, "\u00A5%.2f", bd)
}

fun formatAmount(cent: Long): String {
    val bd = BigDecimal(cent).divide(HUNDRED, 2, RoundingMode.HALF_UP)
    return String.format(Locale.CHINA, "%.2f", bd)
}

fun formatAmountInput(cent: Long): String = formatAmount(cent)

fun centToYuanString(cent: Long): String {
    return BigDecimal(cent).divide(HUNDRED, 2, RoundingMode.HALF_UP).toPlainString()
}
