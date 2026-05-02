package com.zwds.today.ui.common

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 滾輪選擇器（Wheel Picker）
 *
 * 設計原則：
 *   - 元件本身「無狀態」——selectedIndex 由外部控制
 *   - 只有「使用者停止滾動」時才呼叫 onIndexChange，不在任何其他時機呼叫
 *   - 外部要強制重置滾動位置時，用 key() 重建整個元件（不在內部做 animateScroll）
 *
 * @param items           顯示的字串清單
 * @param selectedIndex   目前選中的 index（0-based）
 * @param onIndexChange   使用者停止滾動後回呼新 index
 * @param visibleItemCount 同時顯示幾個（奇數，預設 5）
 */
@Composable
fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemCount: Int = 5
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val baseTextStyle = MaterialTheme.typography.titleLarge
    val wheelTextStyle = baseTextStyle.copy(
        lineHeight = baseTextStyle.fontSize * 1.15f
    )
    val minItemHeight = with(density) { wheelTextStyle.lineHeight.toDp() + 12.dp }
    val itemHeightDp = if (minItemHeight > 44.dp) minItemHeight else 44.dp
    val halfCount    = visibleItemCount / 2
    val safeIndex    = selectedIndex.coerceIn(0, items.lastIndex)
    val latestSelectedIndex by rememberUpdatedState(safeIndex)
    val latestOnIndexChange by rememberUpdatedState(onIndexChange)

    // initialFirstVisibleItemIndex 決定初始捲動位置；
    // 此後不再從外部主動呼叫 animateScrollToItem，避免與捲動偵測形成回饋迴圈。
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)
    val fling     = rememberSnapFlingBehavior(lazyListState = listState)
    val centeredIndex by remember(listState, items.size) {
        derivedStateOf {
            val info   = listState.layoutInfo
            val center = info.viewportSize.height / 2
            info.visibleItemsInfo.minByOrNull { item ->
                abs(center - (item.offset + item.size / 2))
            }?.index?.coerceIn(0, items.lastIndex) ?: latestSelectedIndex
        }
    }

    // 只在使用者滾動（true → false 轉換）時偵測選中 index。
    // 刻意不對初始 false 狀態做任何處理，避免元件剛建立時誤觸發。
    LaunchedEffect(listState, items.size) {
        var wasScrolling = false
        snapshotFlow { listState.isScrollInProgress }.collect { isScrolling ->
            if (wasScrolling && !isScrolling) {
                val newIdx = centeredIndex
                if (newIdx != latestSelectedIndex) {
                    latestOnIndexChange(newIdx)
                }
            }
            wasScrolling = isScrolling
        }
    }

    LaunchedEffect(safeIndex, items.size) {
        if (!listState.isScrollInProgress && centeredIndex != safeIndex) {
            listState.scrollToItem(index = safeIndex)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeightDp * visibleItemCount)
            .clip(RoundedCornerShape(16.dp))
    ) {
        LazyColumn(
            state               = listState,
            flingBehavior       = fling,
            contentPadding      = PaddingValues(vertical = itemHeightDp * halfCount),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, text ->

                // 離視窗中心的距離（以 item 高度為單位）
                val dist by remember(index) {
                    derivedStateOf {
                        val info  = listState.layoutInfo
                        val vc    = info.viewportSize.height / 2f
                        val iInfo = info.visibleItemsInfo.firstOrNull { it.index == index }
                        if (iInfo != null)
                            (abs(vc - (iInfo.offset + iInfo.size / 2f)) / iInfo.size.toFloat())
                                .coerceIn(0f, 2f)
                        else 1.5f
                    }
                }

                Box(
                    modifier        = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    val isCentered = index == centeredIndex
                    Text(
                        text       = text,
                        style      = wheelTextStyle,
                        fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                        color      = MaterialTheme.colorScheme.onSurface,
                        modifier   = Modifier.graphicsLayer {
                            alpha  = (1f - dist * 0.45f).coerceAtLeast(0.08f)
                            val sc = (1f - dist * 0.12f).coerceAtLeast(0.75f)
                            scaleX = sc; scaleY = sc
                        }
                    )
                }
            }
        }

        // 選中框：兩條細線夾住中心格
        HorizontalDivider(
            modifier  = Modifier
                .align(Alignment.TopCenter)
                .padding(top = itemHeightDp * halfCount)
                .fillMaxWidth(0.92f),
            thickness = 1.dp,
            color     = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
        HorizontalDivider(
            modifier  = Modifier
                .align(Alignment.TopCenter)
                .padding(top = itemHeightDp * (halfCount + 1))
                .fillMaxWidth(0.92f),
            thickness = 1.dp,
            color     = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
    }
}
