package com.example.webreader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.webreader.data.NovelSeries
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryGridView(
    seriesList: List<NovelSeries>,
    isBatchMode: Boolean,
    selectedSeriesIds: Set<String>,
    onSeriesClick: (NovelSeries) -> Unit,
    onSeriesLongClick: (NovelSeries) -> Unit,
    onTranslateSeries: (NovelSeries) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(seriesList, key = { it.seriesId }) { series ->
            val isSelected = selectedSeriesIds.contains(series.seriesId)
            NovelCoverCard(
                series = series,
                isSelected = isSelected,
                isBatchMode = isBatchMode,
                onClick = { onSeriesClick(series) },
                onLongClick = { onSeriesLongClick(series) },
                onTranslateSeries = { onTranslateSeries(series) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovelCoverCard(
    series: NovelSeries,
    isSelected: Boolean,
    isBatchMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTranslateSeries: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Generate deterministic gradient background color based on title hash
    val gradientColors = remember(series.title) {
        val hash = abs(series.title.hashCode())
        val hue1 = (hash % 360).toFloat()
        val hue2 = ((hash + 60) % 360).toFloat()
        val c1 = Color.hsl(hue1, 0.65f, 0.35f)
        val c2 = Color.hsl(hue2, 0.75f, 0.20f)
        listOf(c1, c2)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Book Cover Header with Gradient & Styled Title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(Brush.linearGradient(gradientColors))
                    .padding(12.dp)
            ) {
                // Pin / Favorite Icon Badge & 3-dot Menu
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (series.isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = Color.Yellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (series.isFavorite) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    var showCardMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showCardMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showCardMenu,
                            onDismissRequest = { showCardMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dịch chương") },
                                onClick = {
                                    showCardMenu = false
                                    onTranslateSeries()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chi tiết bộ truyện") },
                                onClick = {
                                    showCardMenu = false
                                    onClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.Book, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                // Batch Selection Checkbox Icon
                if (isBatchMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = "Select",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    )
                }

                // Styled Typography Title
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = series.hostDomain,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Card Footer: Chapter count & Progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Book,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${series.chapterCount} chương",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val progressPercent = (series.readProgressPercent * 100).toInt()
                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Last read chapter indicator
                val lastReadTitle = series.lastReadChapterTitle
                if (!lastReadTitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📖 Đang đọc: $lastReadTitle",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { series.readProgressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
