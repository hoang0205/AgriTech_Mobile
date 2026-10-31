package com.example.agritech_mobile.ui.bargain

internal fun bargainErrorMessage(code: Int?, detail: String?): String {
    val fallback = "Chưa thực hiện được yêu cầu. Vui lòng thử lại sau."
    if (code != null && code >= 500) return fallback
    val message = detail?.trim()?.takeIf { it.isNotEmpty() } ?: return "Không thực hiện được. Vui lòng kiểm tra kết nối và tải lại."
    val technical = listOf("could not execute", "sql", "jdbc", "hibernate", "constraint", "exception", "cannot be null")
    return if (message.length > 300 || technical.any { message.contains(it, ignoreCase = true) }) fallback else message
}
