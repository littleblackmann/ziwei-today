package com.zwds.today.ui.family

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.ui.common.*

@Composable
fun FamilyListScreen(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val vm: FamilyListViewModel = viewModel(factory = ZwdsViewModelFactory)
    val members by vm.members.collectAsStateWithLifecycle()
    ZwdsScaffold("我的家人", onBack, actions = {
        IconButton(onAdd) { Icon(Icons.Default.PersonAddAlt1, "新增家人") }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SectionTitle("把在意的人，放在這裡。", "每位家人都有自己的生日、命盤與解讀紀錄。")
            }
            if (members.isEmpty()) item {
                EditorialPanel {
                    Icon(Icons.Default.PeopleOutline, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                    SectionTitle("還沒有家人檔案", "從自己開始，或替家人建立一份專屬的每日提醒。")
                }
            }
            items(members, key = { it.id }) { member ->
                EditorialPanel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f))) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Text(member.name.take(1), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleLarge)
                            Text(if (member.isPrimary) "● 預設家人" else "家人檔案", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton({ onEdit(member.id) }) { Icon(Icons.Default.Edit, "編輯 ${member.name}") }
                    }
                    Text("%d / %02d / %02d · %s · %02d:00".format(member.solarYear, member.solarMonth,
                        member.solarDay, if (member.gender == "FEMALE") "女" else "男", member.hour24),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!member.isPrimary) TextButton({ vm.setPrimary(member.id) }) { Text("設為預設家人") }
                }
            }
            item { BigPrimaryButton("新增家人", onAdd, leadingIcon = Icons.Default.Add) }
        }
    }
}
