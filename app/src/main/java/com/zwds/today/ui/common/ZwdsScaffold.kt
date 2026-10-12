package com.zwds.today.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Keep reading comfortable on both phones and wide screens. */
@Composable
fun ZwdsScaffold(title: String, onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null, content: @Composable (PaddingValues) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        CosmicBackdrop(Modifier.matchParentSize())
        Scaffold(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = { ZwdsTopBar(title, onBack, actions) },
            content = content
        )
    }
}
@Composable
fun ZwdsTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 72.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .65f), RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f), RoundedCornerShape(16.dp))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
            }
        } else {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground)
        actions?.invoke()
    }
}
