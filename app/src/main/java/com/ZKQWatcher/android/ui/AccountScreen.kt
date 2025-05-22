package com.ZKQWatcher.android.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ZKQWatcher.android.BackgroundService
import com.ZKQWatcher.android.data.SettingsRepository
import com.ZKQWatcher.android.model.Account
import com.ZKQWatcher.android.model.Account.Group
import com.ZKQWatcher.android.model.Settings
import kotlinx.coroutines.launch
import com.ZKQWatcher.android.RootPermissionUtil

@Composable
fun AccountScreen(repo: SettingsRepository) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    /* 从 DataStore 读取设置 */
    var settings by remember { mutableStateOf<Settings?>(null) }
    LaunchedEffect(Unit) { settings = repo.load() }

    /* 新建 / 编辑对话框控制 */
    var showDialog by remember { mutableStateOf(false) }
    var editing: Account? by remember { mutableStateOf(null) }

    /* ------- UI ------- */
    settings?.let { st ->
        // 分段数输入状态，初始化为当前值
        var segmentsInput by remember { mutableStateOf(st.segmentsPerAccount.toString()) }

        Column(Modifier.padding(16.dp)) {

            /** 全局 N 可编辑 */
            OutlinedTextField(
                value = segmentsInput,
                onValueChange = { value ->
                    segmentsInput = value
                    val n = value.toIntOrNull() ?: 0f
                    scope.launch {
                        // 保存新的 segmentsPerAccount
                        repo.save(st.copy(segmentsPerAccount = n.toInt()))
                        settings = repo.load()
                    }
                },
                label = { Text("全局 N (段数)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Text("全天组", style = MaterialTheme.typography.titleMedium)
            AccountList(
                accounts = st.accounts.filter { it.group == Group.ALL_DAY },
                onDelete = { acc ->
                    scope.launch {
                        repo.save(st.copy(accounts = st.accounts - acc))
                        settings = repo.load()
                    }
                },
                onEdit = { editing = it; showDialog = true }
            )
            Text("白天组", style = MaterialTheme.typography.titleMedium)
            AccountList(
                accounts = st.accounts.filter { it.group == Group.DAY },
                onDelete = { acc ->
                    scope.launch {
                        repo.save(st.copy(accounts = st.accounts - acc))
                        settings = repo.load()
                    }
                },
                onEdit = { editing = it; showDialog = true }
            )

            Spacer(Modifier.height(12.dp))
            Text("黑夜组", style = MaterialTheme.typography.titleMedium)
            AccountList(
                accounts = st.accounts.filter { it.group == Group.NIGHT },
                onDelete = { acc ->
                    scope.launch {
                        repo.save(st.copy(accounts = st.accounts - acc))
                        settings = repo.load()
                    }
                },
                onEdit   = { editing = it; showDialog = true }
            )

            Spacer(Modifier.height(24.dp))
            Button(onClick = { editing = null; showDialog = true }) {
                Text("➕ 添加账号")
            }

            /* ------------ 后台服务控制区 ------------ */
            Spacer(Modifier.height(24.dp))
            Text("后台运行中: ${BackgroundService.running}")

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (!BackgroundService.running) {
                            val intent = Intent(ctx, BackgroundService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                ctx.startForegroundService(intent)
                            else
                                ctx.startService(intent)
                            Toast.makeText(ctx, "后台已启动", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "服务已在运行", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("运行后台程序") }

                Button(
                    onClick = {
                        if (BackgroundService.running) {
                            ctx.stopService(Intent(ctx, BackgroundService::class.java))
                            Toast.makeText(ctx, "后台已停止", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "服务未在运行", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("终止后台程序") }
            }
        }

        /* ------- 新建 / 编辑弹窗 ------- */
        if (showDialog) {
            EditAccountDialog(
                initial   = editing,
                onDismiss = { showDialog = false },
                onConfirm = { acc ->
                    scope.launch {
                        val list = st.accounts.filter { it.id != acc.id } + acc
                        repo.save(st.copy(accounts = list.sortedBy { it.id }))
                        settings = repo.load()
                    }
                    showDialog = false
                }
            )
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        CircularProgressIndicator()
    }
}

/* ===== 子组件 ===== */

@Composable
private fun AccountList(
    accounts: List<Account>,
    onDelete: (Account) -> Unit,
    onEdit: (Account) -> Unit
) {
    accounts.forEach { acc ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onEdit(acc) },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${acc.id}. ${acc.name}" + if (acc.group != Group.ALL_DAY) " (${acc.hoursPerDay}h)" else "")
            IconButton(onClick = { onDelete(acc) }) {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        }
    }
}

/* 编辑/新增弹窗 */
@Composable
private fun EditAccountDialog(
    initial: Account?,
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    var idInput by remember { mutableStateOf(initial?.id?.toString() ?: "") }
    var name  by remember { mutableStateOf(initial?.name ?: "") }
    var hours by remember { mutableStateOf(initial?.hoursPerDay?.toString() ?: "1") }
    var group by remember { mutableStateOf(initial?.group ?: Group.DAY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val h = if (group == Group.ALL_DAY) 0f else hours.toFloatOrNull() ?: 0f
                onConfirm(
                    initial?.copy(name = name, hoursPerDay = h, group = group)
                        ?: Account(
                            id          = idInput.toIntOrNull() ?: 0,
                            name        = name,
                            group       = group,
                            hoursPerDay = h
                        )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("编辑账号") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账号昵称") }
                )
                OutlinedTextField(
                    value = idInput,
                    onValueChange = { idInput = it },
                    label = { Text("账号编号") },
                    singleLine = true
                )
                if (group != Group.ALL_DAY) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("运行时间 (小时/天)") }
                    )
                }
                Row {
                    RadioButton(selected = group == Group.DAY, onClick = { group = Group.DAY })
                    Text("白天组")
                    Spacer(Modifier.width(12.dp))

                    RadioButton(selected = group == Group.NIGHT, onClick = { group = Group.NIGHT })
                    Text("黑夜组")
                    Spacer(Modifier.width(12.dp))

                    RadioButton(selected = group == Group.ALL_DAY, onClick = { group = Group.ALL_DAY })
                    Text("全天组")
                }
            }
        }
    )
}
