package com.ZKQWatcher.android.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Int,                 // k.txt 的编号
    val name: String,            // UI 显示
    val group: Group,            // DAY / NIGHT
    val hoursPerDay: Float       // T_i（单位 h，0.1 步长即可）
) {
    enum class Group { DAY, NIGHT ,ALL_DAY }
}

// model/Settings.kt
@Serializable
data class Settings(
    val accounts: List<Account> = emptyList(),
    val segmentsPerAccount: Int = 3               // 全局 N
)
