package com.machiav3lli.backup.ui.compose.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.machiav3lli.backup.NeoApp
import com.machiav3lli.backup.ui.pages.pref_busyFadeTime
import com.machiav3lli.backup.ui.pages.pref_busyTurnTime
import com.machiav3lli.backup.ui.pages.pref_versionOpacity
import com.machiav3lli.backup.utils.SystemUtils
import com.machiav3lli.backup.utils.SystemUtils.applicationIssuer
import com.machiav3lli.backup.utils.SystemUtils.versionName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun Modifier.angledGradientBackground(colors: List<Color>, degrees: Float, factor: Float = 1f) =
    this.then(
        drawBehind {

            val (w, h) = size
            val dim = max(w, h) * factor

            val degreesNormalised = (degrees % 360).let { if (it < 0) it + 360 else it }

            val alpha = (degreesNormalised * PI / 180).toFloat()

            val centerOffsetX = cos(alpha) * dim / 2
            val centerOffsetY = sin(alpha) * dim / 2

            drawRect(
                brush = Brush.linearGradient(
                    colors = colors,
                    // negative here so that 0 degrees is left -> right
                    // and 90 degrees is top -> bottom
                    start = Offset(center.x - centerOffsetX, center.y - centerOffsetY),
                    end = Offset(center.x + centerOffsetX, center.y + centerOffsetY)
                ),
                size = size
            )
        }
    )

fun Modifier.busyBackground(
    angle: Float,
    color0: Color,
    color1: Color,
    color2: Color,
): Modifier {
    val factor = 0.2f
    return this
        .angledGradientBackground(
            listOf(
                color0,
                color0,
                color1,
                color1,
                color0,
                color0,
                color0,
                color0,
                color0,
            ), angle, factor
        )
        .angledGradientBackground(
            listOf(
                color0,
                color0,
                color2,
                color2,
                color0,
                color0,
                color0,
                color0,
                color0,
            ), angle * 1.5f, factor
        )
}

@Composable
fun BusyBackgroundAnimated(
    busy: Boolean,
    content: @Composable () -> Unit,
) {
    val rounds = 12
    val color0 = Color.Transparent
    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    val color2 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)

    val inTime = pref_busyFadeTime.value
    val outTime = pref_busyFadeTime.value
    val turnTime = pref_busyTurnTime.value

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = busy,
            enter = fadeIn(tween(inTime)),
            exit = fadeOut(tween(outTime)),
            modifier = Modifier
                .fillMaxSize()
        ) {
            fun currentAngle(): Float =
                SystemUtils.msSinceBoot % turnTime * 360f / turnTime
            //var angle by rememberSaveable { mutableStateOf(70f) }
            //var angle by rememberSaveable { mutableFloatStateOf(calcAngle()) }
            var angle by retain { mutableStateOf(currentAngle()) }
            LaunchedEffect(true) {
                withContext(Dispatchers.IO) {
                    animate(
                        initialValue = angle,
                        targetValue = angle + 360f * rounds,
                        animationSpec = infiniteRepeatable(
                            animation = tween(turnTime * rounds, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    ) { value, _ -> angle = value }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .busyBackground(angle, color0, color1, color2)
            ) {
            }
        }

        content()
    }
}

@Composable
fun BusyBackgroundColor(
    busy: Boolean,
    content: @Composable () -> Unit,
) {
    val inTime = pref_busyFadeTime.value
    val outTime = pref_busyFadeTime.value

    AnimatedVisibility(
        visible = busy,
        enter = fadeIn(tween(inTime)),
        exit = fadeOut(tween(outTime)),
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Gray.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
        }
    }

    content()
}

@Composable
fun BusyBackground(
    modifier: Modifier = Modifier,
    busy: State<Boolean>? = null,
    content: @Composable () -> Unit,
) {
    val isBusy by remember { busy ?: NeoApp.busy }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
//        if (false)
//            BusyBackgroundAnimated(busy = isBusy, content = content)
//        else
        BusyBackgroundColor(busy = isBusy, content = content)
    }
}

@Composable
fun FullScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
//        if (false)
//            BusyBackground(modifier = modifier, content = content)
//        else
        content()

        if (pref_versionOpacity.value > 0)
            Text(
                text = "$versionName ${context.applicationIssuer}",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = pref_versionOpacity.value / 100f),
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopCenter)
            )
    }
}

@Composable
fun InnerBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BusyBackground(modifier = modifier, content = content)
}

@Preview
@Composable
fun BusyBackgroundPreview() {
    val busy by remember { NeoApp.busy }
    val count by remember { NeoApp.busyCountDown }
    val level by remember { NeoApp.busyLevel }

    Column {
        Row {
            RefreshButton {
                NeoApp.hitBusy(5000)
            }
            ActionChip(text = "begin", positive = level > 0) {
                NeoApp.beginBusy()
            }
            ActionChip(text = "end", positive = level > 0) {
                NeoApp.endBusy()
            }
        }
        BusyBackground(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                """
                    busy:   ${busy}
                    count:  ${count}
                    level:  ${level}
    
                    we are
                    very busy
                    today
                """.trimIndent(),
                fontSize = 24.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(align = Alignment.Center)
            )
        }
    }
}
