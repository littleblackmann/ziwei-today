package com.zwds.today.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zwds.today.di.ServiceLocator

fun openWebPage(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Toast.makeText(context, "找不到可開啟連結的瀏覽器", Toast.LENGTH_LONG).show() }
}
@Composable
fun UpdateDialog() {
    val updates = ServiceLocator.updateRepository
    val state by updates.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    state.prompt?.let { release ->
        AlertDialog(
            onDismissRequest = { updates.dismiss(release.tag) },
            title = { Text("新版本 ${release.tag}") },
            text = { Text(release.notes.ifBlank { "新版已可下載。更新會由 Android 安裝畫面向您確認。" },
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = {
                updates.dismiss(release.tag); openWebPage(context, release.pageUrl)
            }) { Text("查看與下載") } },
            dismissButton = { TextButton(onClick = { updates.dismiss(release.tag) }) { Text("稍後再說") } }
        )
    }
}
