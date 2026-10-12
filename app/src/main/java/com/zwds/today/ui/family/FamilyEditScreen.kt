package com.zwds.today.ui.family

import android.util.TypedValue
import android.widget.EditText
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.viewinterop.AndroidView
import com.zwds.today.astro.lunar.LunarCalendar
import com.zwds.today.di.ZwdsViewModelFactory
import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.Gender
import com.zwds.today.ui.common.BigOutlinedButton
import com.zwds.today.ui.common.BigPrimaryButton
import com.zwds.today.ui.common.ZwdsScaffold

// ─── 時辰資料 ─────────────────────────────────────────────────────────────────
private data class BranchHour(val branch: EarthlyBranch, val repHour: Int, val range: String)

private val BRANCH_HOURS = listOf(
    BranchHour(EarthlyBranch.ZI,    23, "23 - 01"),
    BranchHour(EarthlyBranch.CHOU,   1, "01 - 03"),
    BranchHour(EarthlyBranch.YIN,    3, "03 - 05"),
    BranchHour(EarthlyBranch.MAO,    5, "05 - 07"),
    BranchHour(EarthlyBranch.CHEN,   7, "07 - 09"),
    BranchHour(EarthlyBranch.SI,     9, "09 - 11"),
    BranchHour(EarthlyBranch.WU,    11, "11 - 13"),
    BranchHour(EarthlyBranch.WEI,   13, "13 - 15"),
    BranchHour(EarthlyBranch.SHEN,  15, "15 - 17"),
    BranchHour(EarthlyBranch.YOU,   17, "17 - 19"),
    BranchHour(EarthlyBranch.XU,    19, "19 - 21"),
    BranchHour(EarthlyBranch.HAI,   21, "21 - 23")
)

private val PickerHeight = 188.dp
private val PickerSelectionHeight = 54.dp

private fun maxDayInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11            -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 30
}

// ─── Main Screen ─────────────────────────────────────────────────────────────

@Composable
fun FamilyEditScreen(
    editingId: Long? = null,
    onDone: () -> Unit
) {
    val vm: FamilyEditViewModel = viewModel(factory = ZwdsViewModelFactory)
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(editingId) {
        if (editingId != null) vm.loadForEdit(editingId)
    }
    LaunchedEffect(state.saved) {
        if (state.saved) { vm.consumeSaved(); onDone() }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    ZwdsScaffold(
        title   = if (state.mode == EditMode.CREATE) "新增家人" else "編輯家人",
        onBack  = onDone,
        actions = {
            if (state.mode == EditMode.EDIT) {
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "刪除")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.zwds.today.ui.common.SectionTitle("一份專屬的每日提醒", "出生資料只保存在此裝置，之後也可以隨時修改。")
            // 稱呼
            FieldLabel("稱呼")
            OutlinedTextField(
                value        = state.name,
                onValueChange = vm::setName,
                placeholder  = { Text("例如：爸爸、老伴、阿嬤") },
                modifier     = Modifier.fillMaxWidth(),
                textStyle    = MaterialTheme.typography.titleLarge,
                singleLine   = true,
                shape = RoundedCornerShape(16.dp)
            )

            // 性別
            FieldLabel("性別")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipButton("男", state.gender == Gender.MALE)   { vm.setGender(Gender.MALE) }
                ChipButton("女", state.gender == Gender.FEMALE) { vm.setGender(Gender.FEMALE) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 農曆 / 國曆切換
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("使用農曆輸入")
                    Text(
                        if (state.isLunarInput) "依農曆生日選擇年月日"
                        else "依國曆生日選擇年月日",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.isLunarInput, onCheckedChange = vm::setIsLunarInput)
            }

            // 日期滾輪
            // key(state.dataVersion)：編輯模式下 loadForEdit 完成後強制重建滾輪，
            // 讓 initialFirstVisibleItemIndex 捲到成員的實際生日位置
            FieldLabel(if (state.isLunarInput) "農曆生日" else "國曆生日")
            key(state.dataVersion) {
                if (state.isLunarInput) {
                    LunarDateWheel(
                        year     = state.lunarYear,
                        month    = state.lunarMonth,
                        day      = state.lunarDay,
                        isLeap   = state.isLeapMonth,
                        onChange = vm::setLunarDate
                    )
                } else {
                    SolarDateWheel(
                        year     = state.solarYear,
                        month    = state.solarMonth,
                        day      = state.solarDay,
                        onChange = vm::setSolarDate
                    )
                }
            }

            // 時辰滾輪
            FieldLabel("出生時辰")
            key(state.dataVersion) {
                HourWheel(hour24 = state.hour24, onChange = vm::setHour)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 主帳號
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("設為預設家人", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "命盤與運勢查詢的預設對象",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(checked = state.isPrimary, onCheckedChange = vm::setPrimary)
            }

            if (state.error != null) {
                Text(
                    state.error ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            BigPrimaryButton(
                text    = if (state.saving) "儲存中..." else "儲存",
                enabled = !state.saving,
                onClick = vm::save
            )
            BigOutlinedButton(text = "取消", onClick = onDone)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text("確認刪除", style = MaterialTheme.typography.headlineSmall) },
            text    = {
                Text(
                    "刪除「${state.name}」後資料將無法復原，確定要刪除嗎？",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; vm.delete(onDone) }) {
                    Text("確定刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ─── 國曆日期選擇器 ───────────────────────────────────────────────────────────

@Composable
private fun SolarDateWheel(
    year: Int, month: Int, day: Int,
    onChange: (Int, Int, Int) -> Unit
) {
    val yearRange = 1920..java.time.LocalDate.now().year.coerceAtMost(2100)
    val maxDay    = remember(year, month) { maxDayInMonth(year, month) }

    // 日期鉗制：切換月份時若 day 超過上限自動修正
    LaunchedEffect(maxDay) {
        if (day > maxDay) onChange(year, month, maxDay)
    }

    val years  = remember { yearRange.map { "${it}年" } }
    val months = remember { (1..12).map { "${it}月" } }

    WheelContainer {
        NativePickerColumn(
            items = years,
            selectedIndex = (year - yearRange.first).coerceIn(0, years.lastIndex),
            onIndexChange = { i ->
                val newYear = yearRange.first + i
                onChange(newYear, month, day.coerceAtMost(maxDayInMonth(newYear, month)))
            },
            modifier = Modifier.weight(1.3f)
        )
        NativePickerColumn(
            items = months,
            selectedIndex = (month - 1).coerceIn(0, 11),
            onIndexChange = { i ->
                val newMonth = i + 1
                onChange(year, newMonth, day.coerceAtMost(maxDayInMonth(year, newMonth)))
            },
            modifier = Modifier.weight(0.85f)
        )
        key(maxDay) {
            val days = (1..maxDay).map { "${it}日" }
            NativePickerColumn(
                items = days,
                selectedIndex = (day - 1).coerceIn(0, days.lastIndex),
                onIndexChange = { i -> onChange(year, month, i + 1) },
                modifier = Modifier.weight(0.85f)
            )
        }
    }
}

// ─── 農曆日期選擇器 ───────────────────────────────────────────────────────────

@Composable
private fun LunarDateWheel(
    year: Int, month: Int, day: Int, isLeap: Boolean,
    onChange: (Int, Int, Int, Boolean) -> Unit
) {
    val yearRange = 1920..java.time.LocalDate.now().year.coerceAtMost(2100)
    val years     = remember { yearRange.map { "${it}年" } }
    val months    = remember { (1..12).map { "${it}月" } }
    val leapMonth = remember(year) { LunarCalendar.leapMonth(year) }
    val leapAllowed = leapMonth == month
    val safeIsLeap = isLeap && leapAllowed
    val maxDay = remember(year, month, safeIsLeap) {
        LunarCalendar.monthDays(year, month, safeIsLeap)
    }

    LaunchedEffect(leapAllowed, safeIsLeap, maxDay) {
        if (isLeap != safeIsLeap || day > maxDay) {
            onChange(year, month, day.coerceAtMost(maxDay), safeIsLeap)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WheelContainer {
            NativePickerColumn(
                items = years,
                selectedIndex = (year - yearRange.first).coerceIn(0, years.lastIndex),
                onIndexChange = { i -> onChange(yearRange.first + i, month, day, safeIsLeap) },
                modifier = Modifier.weight(1.3f)
            )
            NativePickerColumn(
                items = months,
                selectedIndex = (month - 1).coerceIn(0, 11),
                onIndexChange = { i -> onChange(year, i + 1, day, safeIsLeap) },
                modifier = Modifier.weight(0.85f)
            )
            key(maxDay, safeIsLeap) {
                val days = (1..maxDay).map { "${it}日" }
                NativePickerColumn(
                    items = days,
                    selectedIndex = (day - 1).coerceIn(0, days.lastIndex),
                    onIndexChange = { i -> onChange(year, month, i + 1, safeIsLeap) },
                    modifier = Modifier.weight(0.85f)
                )
            }
        }

        // 閏月 toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .clickable(enabled = leapAllowed) { onChange(year, month, day, !safeIsLeap) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Checkbox(
                checked         = safeIsLeap,
                enabled         = leapAllowed,
                onCheckedChange = { onChange(year, month, day, it) }
            )
            Text(
                if (leapAllowed) "是閏月" else "本年此月沒有閏月",
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// ─── 時辰選擇器 ───────────────────────────────────────────────────────────────

@Composable
private fun HourWheel(hour24: Int, onChange: (Int) -> Unit) {
    val currentBranch = EarthlyBranch.fromHour24(hour24)
    val selectedIdx   = BRANCH_HOURS.indexOfFirst { it.branch == currentBranch }.coerceAtLeast(0)
    val items         = remember { BRANCH_HOURS.map { "${it.branch.cnName}時　${it.range}" } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WheelContainer {
            NativePickerColumn(
                items = items,
                selectedIndex = selectedIdx,
                onIndexChange = { i -> onChange(BRANCH_HOURS[i].repHour) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            "不確定時辰可先選「子時」，可在設定頁調整晚子時歸屬",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── WheelContainer ─（圓角卡片包住原生選擇器，統一外觀）───────────────────

@Composable
private fun WheelContainer(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(PickerSelectionHeight)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun NativePickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface.toArgb()
    val textSizePx = with(density) {
        MaterialTheme.typography.titleLarge.fontSize.toPx()
    }
    val selected = selectedIndex.coerceIn(0, items.lastIndex)

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(PickerHeight),
        factory = {
            NumberPicker(context).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = items.size > 1
                setOnLongPressUpdateInterval(150L)
            }
        },
        update = { picker ->
            bindPickerItems(
                picker = picker,
                items = items,
                selectedIndex = selected,
                wrap = items.size > 1,
                textColor = textColor,
                textSizePx = textSizePx,
                onIndexChange = onIndexChange
            )
        }
    )
}

private fun bindPickerItems(
    picker: NumberPicker,
    items: List<String>,
    selectedIndex: Int,
    wrap: Boolean,
    textColor: Int,
    textSizePx: Float,
    onIndexChange: (Int) -> Unit
) {
    val desiredValues = items.toTypedArray()
    val currentValues = picker.displayedValues
    if (currentValues == null || !currentValues.contentEquals(desiredValues)) {
        picker.displayedValues = null
        picker.minValue = 0
        picker.maxValue = desiredValues.lastIndex
        picker.displayedValues = desiredValues
    }

    picker.wrapSelectorWheel = wrap
    if (picker.value != selectedIndex) {
        picker.value = selectedIndex
    }

    styleNumberPicker(
        picker = picker,
        textColor = textColor,
        textSizePx = textSizePx
    )

    picker.setOnValueChangedListener { _, _, newVal ->
        onIndexChange(newVal.coerceIn(0, items.lastIndex))
    }
}

private fun styleNumberPicker(
    picker: NumberPicker,
    textColor: Int,
    textSizePx: Float
) {
    if (android.os.Build.VERSION.SDK_INT >= 29) {
        picker.textColor = textColor
        picker.textSize = textSizePx
        picker.selectionDividerHeight = 0
    }

    for (i in 0 until picker.childCount) {
        val child = picker.getChildAt(i)
        if (child is EditText) {
            child.setTextColor(textColor)
            child.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            child.isEnabled = false
            child.setPadding(0, 0, 0, 0)
        }
    }

    picker.invalidate()
}

// ─── 共用小元件 ───────────────────────────────────────────────────────────────

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.titleMedium,
        color      = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primary else colors.surfaceVariant
    val fg = if (selected) colors.onPrimary else colors.onSurfaceVariant
    Box(
        modifier = Modifier
            .heightIn(min = 56.dp)
            .widthIn(min = 80.dp)
            .background(bg, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = fg)
    }
}
