package be.kdr.agvalarm.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.kdr.agvalarm.R
import be.kdr.agvalarm.ui.theme.AccentOrange
import be.kdr.agvalarm.ui.theme.CardWhite
import be.kdr.agvalarm.ui.theme.Coral
import be.kdr.agvalarm.ui.theme.Ink
import be.kdr.agvalarm.ui.theme.LabelGrey
import be.kdr.agvalarm.ui.theme.SageCanvas
import be.kdr.agvalarm.ui.theme.SageDeep
import be.kdr.agvalarm.ui.theme.SageOlive
import be.kdr.agvalarm.ui.theme.TrackGrey

enum class DashTab(val label: String) {
    Home("Home"),
    Alarms("Storingen"),
    Settings("Instellingen"),
}

@Composable
fun DashboardScaffold(
    current: DashTab,
    onSelect: (DashTab) -> Unit,
    content: @Composable () -> Unit,
) {
    val heroStrength = when (current) {
        DashTab.Settings -> 0.16f
        else -> 1f
    }
    Box(Modifier.fillMaxSize()) {
        FactoryWashBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(26.dp), ambientColor = Color(0x33000000))
                    .clip(RoundedCornerShape(26.dp)),
            ) {
                AgvHeroLayer(strength = heroStrength)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.00f to Color(0x38F4F2EC),
                                0.36f to Color(0x22F4F2EC),
                                1.00f to Color(0xC2F4F2EC),
                            ),
                        ),
                )
                Column(Modifier.fillMaxSize()) {
                    BrandHeader(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp))
                    Spacer(Modifier.height(12.dp))
                    PillNav(
                        current = current,
                        onSelect = onSelect,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

private val HeroSageMatrix: ColorMatrix = ColorMatrix().apply {
    setToSaturation(0.28f)
    timesAssign(
        ColorMatrix(
            floatArrayOf(
                0.62f, 0.12f, 0.08f, 0f, 28f,
                0.10f, 0.66f, 0.10f, 0f, 30f,
                0.08f, 0.12f, 0.55f, 0f, 24f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}

@Composable
fun FactoryWashBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(SageCanvas, Color(0xFF959A8B), SageDeep),
            ),
        )
    }
}

@Composable
private fun AgvHeroLayer(strength: Float, modifier: Modifier = Modifier) {
    val alpha = strength.coerceIn(0f, 1f)
    Box(modifier.fillMaxSize().background(SageCanvas)) {
        if (alpha > 0.01f) {
            Image(
                painter = painterResource(id = R.drawable.agv_hero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.84f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer { this.alpha = 0.94f * alpha },
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0.72f, 0.78f),
                colorFilter = ColorFilter.colorMatrix(HeroSageMatrix),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(SageCanvas.copy(alpha = 0.40f + (1f - alpha) * 0.42f)),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to SageCanvas.copy(alpha = 0.22f * alpha),
                            0.26f to Color.Transparent,
                            0.58f to SageCanvas.copy(alpha = 0.22f * alpha),
                            1.00f to SageCanvas.copy(alpha = 0.90f),
                        ),
                    ),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.00f to SageCanvas.copy(alpha = 0.20f * alpha),
                            0.38f to Color.Transparent,
                            1.00f to Color.Transparent,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun BrandHeader(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        AgvMark()
        Spacer(Modifier.width(10.dp))
        Text(
            text = "AGV Alarm",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
    }
}

@Composable
fun AgvMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.72f)
            lineTo(w * 0.72f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.38f)
            lineTo(w * 0.28f, h * 0.72f)
            close()
        }
        drawPath(path, AccentOrange)
        drawCircle(AccentOrange, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.82f))
    }
}

@Composable
private fun PillNav(
    current: DashTab,
    onSelect: (DashTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0x332F332C))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DashTab.entries.forEach { tab ->
            val selected = tab == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Ink else Color(0xFFEDEDE6),
                )
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier.fillMaxWidth(),
    sage: Boolean = false,
    onExpand: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (sage) SageOlive else CardWhite,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = if (sage) {
            null
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
        },
    ) {
        Box {
            Column(
                Modifier
                    .padding(16.dp)
                    .then(if (onExpand != null) Modifier.padding(end = 28.dp) else Modifier),
                content = content,
            )
            if (onExpand != null) {
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Meer",
                        tint = if (sage) Color.White.copy(alpha = 0.8f) else LabelGrey,
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedBar(filled: Int, total: Int = 12, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (index < filled) AccentOrange else TrackGrey),
            )
        }
    }
}

@Composable
fun RowScope.KpiCell(value: String, unit: String = "", label: String) {
    Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                fontSize = 22.sp,
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = LabelGrey,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = LabelGrey)
    }
}

@Composable
fun CoralPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Coral)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun WarningTriangle(modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(size.width / 2f, size.height * 0.08f)
            lineTo(size.width * 0.94f, size.height * 0.90f)
            lineTo(size.width * 0.06f, size.height * 0.90f)
            close()
        }
        drawPath(path, Coral)
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.46f, size.height * 0.36f),
            size = Size(size.width * 0.08f, size.height * 0.28f),
        )
        drawCircle(
            color = Color.White,
            radius = size.width * 0.045f,
            center = Offset(size.width / 2f, size.height * 0.76f),
        )
    }
}

@Composable
fun LimeDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(be.kdr.agvalarm.ui.theme.Lime),
    )
}

@Composable
fun MiniGauge(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.size(72.dp)) {
        val stroke = 8.dp.toPx()
        drawArc(
            color = TrackGrey,
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            size = Size(size.width, size.height),
        )
        drawArc(
            color = AccentOrange,
            startAngle = 140f,
            sweepAngle = 260f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round),
            size = Size(size.width, size.height),
        )
    }
}
