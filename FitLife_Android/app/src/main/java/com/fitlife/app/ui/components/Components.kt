package com.fitlife.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

// ── Animated circular progress ring ──────────────────────────────────────────
@Composable
fun RingProgress(
    progress: Float,           // 0f..1f
    size: Dp = 180.dp,
    strokeWidth: Dp = 18.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "ring"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val radius = (this.size.minDimension - stroke) / 2f
            val topLeft = Offset((this.size.width - radius * 2) / 2f, (this.size.height - radius * 2) / 2f)
            val arcSize = Size(radius * 2, radius * 2)
            // Track
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // Progress
            drawArc(color = progressColor, startAngle = -90f, sweepAngle = 360f * animatedProgress,
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

// ── Animated progress bar ─────────────────────────────────────────────────────
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "bar"
    )
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(trackColor)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).background(color, RoundedCornerShape(50)))
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────
@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String = "",
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color)
            if (unit.isNotEmpty())
                Text(unit, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Section card with header ──────────────────────────────────────────────────
@Composable
fun SectionCard(
    title: String,
    subtitle: String = "",
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (subtitle.isNotEmpty())
                        Text(subtitle, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ── Macro progress row ────────────────────────────────────────────────────────
@Composable
fun MacroRow(label: String, current: Float, target: Float, color: Color) {
    val pct = if (target > 0) (current / target).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${current.toInt()}g / ${target.toInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        AnimatedProgressBar(pct, color = color, modifier = Modifier.fillMaxWidth())
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────
@Composable
fun BarChart(
    values: List<Pair<String, Int>>, // label to value
    maxValue: Int,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    goalValue: Int = 0,
    modifier: Modifier = Modifier.height(120.dp)
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val goalColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEach { (label, value) ->
            val fraction = if (maxValue > 0) value.toFloat() / maxValue else 0f
            val animFrac by animateFloatAsState(fraction, tween(600), label = "bar_$label")
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Canvas(Modifier.fillMaxSize()) {
                        val barW = this.size.width * 0.7f
                        val x = (this.size.width - barW) / 2f
                        // Track bar
                        drawRoundRect(trackColor, topLeft = Offset(x, 0f), size = Size(barW, this.size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                        // Value bar
                        val barH = this.size.height * animFrac
                        if (barH > 0)
                            drawRoundRect(highlightColor, topLeft = Offset(x, this.size.height - barH), size = Size(barW, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                        // Goal line
                        if (goalValue > 0 && maxValue > 0) {
                            val goalY = this.size.height * (1f - goalValue.toFloat() / maxValue)
                            drawLine(goalColor, Offset(0f, goalY), Offset(this.size.width, goalY), 1.5f)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ── Streak badge ──────────────────────────────────────────────────────────────
@Composable
fun StreakBadge(days: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text("$days day streak", style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

// ── Mood selector ─────────────────────────────────────────────────────────────
@Composable
fun MoodSelector(selected: Int, onSelect: (Int) -> Unit) {
    val moods = listOf("😞", "😕", "😐", "😊", "😁")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        moods.forEachIndexed { i, emoji ->
            val v = i + 1
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected == v) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(52.dp).clickable { onSelect(v) }
            ) {
                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 24.sp) }
            }
        }
    }
}
