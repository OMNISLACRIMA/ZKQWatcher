package com.ZKQWatcher.android

/**
 * Keeps all accounts in memory.
 * Call [addAccount] before starting the BackgroundService.
 */
object AccountManager {

    private const val DAY_GROUP = 0
    private const val NIGHT_GROUP = 1

    data class Account(
        val id: Int,             // 0‑based index used in file path
        val name: String,
        var group: Int = DAY_GROUP
    )

    private val accounts = mutableListOf<Account>()

    /** Add account and auto‑assign group to keep both groups balanced. */
    fun addAccount(name: String) {
        val id = accounts.size
        val dayCnt  = accounts.count { it.group == DAY_GROUP }
        val nightCnt = accounts.size - dayCnt
        val group = if (dayCnt <= nightCnt) DAY_GROUP else NIGHT_GROUP
        accounts += Account(id, name, group)
    }

    fun getAll(): List<Account> = accounts.toList()
    fun dayGroup()  = accounts.filter { it.group == DAY_GROUP }
    fun nightGroup() = accounts.filter { it.group == NIGHT_GROUP }
}
