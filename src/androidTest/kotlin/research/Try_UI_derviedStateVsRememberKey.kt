package tests.research.Try_UI_derivedStateVsRememberKey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

const val countTime = 100
const val resetTime = 200

var externalState = mutableIntStateOf(0)
var externalByState by mutableIntStateOf(0)
val externalStateValue get() = externalState.intValue
val externalByStateValue get() = externalByState
var externalValue = 0

@Composable
fun ActionButton(name: String, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Stable
fun Modifier.recomposeHighlighter(): Modifier = this.then(recomposeModifier)

// Use a single instance + @Stable to ensure that recompositions can enable skipping optimizations
// Modifier.composed will still remember unique data per call site.
private val recomposeModifier =
    Modifier.composed(inspectorInfo = debugInspectorInfo {
        name = "recomposeHighlighter"
    }) {
        // The total number of compositions that have occurred. We're not using a State<> here be
        // able to read/write the value without invalidating (which would cause infinite
        // recomposition).
        val totalCompositions = remember { arrayOf(0L) }
        totalCompositions[0]++
        // The value of totalCompositions at the last timeout.
        val totalCompositionsAtLastTimeout = remember { mutableLongStateOf(0L) }
        // Start the timeout, and reset everytime there's a recomposition. (Using totalCompositions
        // as the key is really just to cause the timer to restart every composition).
        LaunchedEffect(totalCompositions[0]) {
            delay(resetTime.milliseconds)
            totalCompositionsAtLastTimeout.longValue = totalCompositions[0]
        }
        Modifier.drawWithCache {
            onDrawWithContent {
                // Draw actual content.
                drawContent()
                // Below is to draw the highlight, if necessary. A lot of the logic is copied from
                // Modifier.border
                val numCompositionsSinceTimeout =
                    totalCompositions[0] - totalCompositionsAtLastTimeout.longValue
                val hasValidBorderParams = size.minDimension > 0f
                if (!hasValidBorderParams || numCompositionsSinceTimeout <= 0) {
                    return@onDrawWithContent
                }
                val (color, strokeWidthPx) =
                    when (numCompositionsSinceTimeout) {
                        // We need at least one composition to draw, so draw the smallest border
                        // color in blue.
                        1L   -> Color.Cyan.copy(alpha = 0.5f) to 1.dp.toPx()
                        // 2 compositions is _probably_ okay.
                        2L   -> Color.Blue.copy(alpha = 0.5f) to 1.dp.toPx()
                        // 3 or more compositions before timeout may indicate an issue. lerp the
                        // color from yellow to red, and continually increase the border size.
                        else -> {
                            lerp(
                                Color.Green.copy(alpha = 0.3f),
                                Color.Red.copy(alpha = 0.5f),
                                min(1f, (numCompositionsSinceTimeout - 1).toFloat() / 100f)
                            ) to 2.dp.toPx() //numCompositionsSinceTimeout.toInt().dp.toPx()
                        }
                    }
                val halfStroke = strokeWidthPx / 2
                val topLeft = Offset(halfStroke, halfStroke)
                val borderSize =
                    Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                val fillArea = (strokeWidthPx * 2) > size.minDimension
                val rectTopLeft = if (fillArea) Offset.Zero else topLeft
                val size = if (fillArea) size else borderSize
                val style = if (fillArea) Fill else Stroke(strokeWidthPx)
                drawRect(
                    brush = SolidColor(color),
                    topLeft = rectTopLeft,
                    size = size,
                    style = style
                )
            }
        }
    }

//functions-begin
@Composable
fun DerivedState_from_Value() {
    val value by remember { derivedStateOf { externalValue } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_from_Value")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_from_Param(param: Int) {
    val value by remember { derivedStateOf { param } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_from_Param")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Value_stateofValue() {
    val value by remember(externalValue) { mutableStateOf(externalValue) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Value_stateofValue")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Value_stateofValue10() {
    val value by remember(externalValue) { mutableStateOf(externalValue / 10) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Value_stateofValue10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Value10_stateofValue10() {
    val value by remember(externalValue / 10) { mutableStateOf(externalValue / 10) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Value10_stateofValue10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Value_Value() {
    val value = remember(externalValue) { externalValue }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Value_Value")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Value_Value10() {
    val value = remember(externalValue) { externalValue / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Value_Value10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Value10_Value10() {
    val value = remember(externalValue / 10) { externalValue / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Value10_Value10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun Remember__stateofParam(param: Int) {
    val value by remember { mutableStateOf(param) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" Remember__stateofParam")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun Remember__stateofParam10(param: Int) {
    val value by remember { mutableStateOf(param / 10) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" Remember__stateofParam10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun Remember__Param(param: Int) {
    val value = remember { param }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" Remember__Param")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun Remember__Param10(param: Int) {
    val value = remember { param / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" Remember__Param10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_State() {
    val value by remember { derivedStateOf { externalState.intValue } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_State")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_State10() {
    val value by remember { derivedStateOf { externalState.intValue / 10 } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_State10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_StateValue() {
    val value by remember { derivedStateOf { externalStateValue } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_StateValue")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_StateValue10() {
    val value by remember { derivedStateOf { externalStateValue / 10 } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_StateValue10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_byState() {
    val value by remember { derivedStateOf { externalByState } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_byState")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_byState10() {
    val value by remember { derivedStateOf { externalByState / 10 } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_byState10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_byStateValue() {
    val value by remember { derivedStateOf { externalByStateValue } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_byStateValue")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DerivedState_byStateValue10() {
    val value by remember { derivedStateOf { externalByStateValue / 10 } }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" DerivedState_byStateValue10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_State_stateofState() {
    val value by remember(externalState.intValue) { mutableStateOf(externalState.intValue) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_State_stateofState")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_State_stateofState10() {
    val value by remember(externalState.intValue) { mutableStateOf(externalState.intValue / 10) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_State_stateofState10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_State10_stateofState10() {
    val value by remember(externalState.intValue / 10) { mutableStateOf(externalState.intValue / 10) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_State10_stateofState10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_State_State() {
    val value = remember(externalState.intValue) { externalState.intValue }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_State_State")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_State_State10() {
    val value = remember(externalState.intValue) { externalState.intValue / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_State_State10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_State10_State10() {
    val value = remember(externalState.intValue / 10) { externalState.intValue / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_State10_State10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_byState_byState() {
    val value = remember(externalByState) { externalByState }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_byState_byState")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_byState_byState10() {
    val value = remember(externalByState) { externalByState / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_byState_byState10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_byState10_byState10() {
    val value = remember(externalByState / 10) { externalByState / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_byState10_byState10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_byStateValue_byStateValue() {
    val value = remember(externalByStateValue) { externalByStateValue }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_byStateValue_byStateValue")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_byStateValue_byStateValue10() {
    val value = remember(externalByStateValue) { externalByStateValue / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_byStateValue_byStateValue10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_byStateValue10_byStateValue10() {
    val value = remember(externalByStateValue / 10) { externalByStateValue / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_byStateValue10_byStateValue10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Param_stateofParam(param: Int) {
    val value by remember(param) { mutableStateOf(param) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Param_stateofParam")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Param_stateofParam10(param: Int) {
    val value by remember(param) { mutableStateOf(param / 10) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Param_stateofParam10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Param10_stateofParam10(param: Int) {
    val value by remember(param / 10) { mutableStateOf(param / 10) }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Param10_stateofParam10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Param_Param(param: Int) {
    val value = remember(param) { param }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Param_Param")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Param_Param10(param: Int) {
    val value = remember(param) { param / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Param_Param10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RememberKey_Param10_Param10(param: Int) {
    val value = remember(param / 10) { param / 10 }
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" RememberKey_Param10_Param10")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun Param(param: Int) {
    Row {
        Text(
            text = "   $param   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" Param")
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun Param10(param: Int) {
    val value = param / 10
    Row {
        Text(
            text = "   $value   ",
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(70.dp)
                .recomposeHighlighter()
        )
        Text(" Param10")
        Spacer(modifier = Modifier.weight(1f))
    }
}
//functions-end

@Composable
fun TheComposable() {

    Column(
        modifier = Modifier
            .recomposeHighlighter()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {

        Text("externalValue: $externalValue")
        Text("externalState: ${externalState.intValue}")
        Text("externalByState: $externalByState")
        Text("externalStateValue: $externalStateValue")
        Text("externalByStateValue: $externalByStateValue")

        //invocations-begin
        Text("")

        DerivedState_from_Value()
        DerivedState_from_Param(externalValue)
        RememberKey_Value_stateofValue()
        RememberKey_Value_stateofValue10()
        RememberKey_Value10_stateofValue10()
        RememberKey_Value_Value()
        RememberKey_Value_Value10()
        RememberKey_Value10_Value10()
        Remember__stateofParam(externalValue)
        Remember__stateofParam10(externalValue)
        Remember__Param(externalValue)
        Remember__Param10(externalValue)
        DerivedState_State()
        DerivedState_State10()
        DerivedState_StateValue()
        DerivedState_StateValue10()
        DerivedState_byState()
        DerivedState_byState10()
        DerivedState_byStateValue()
        DerivedState_byStateValue10()
        RememberKey_State_stateofState()
        RememberKey_State_stateofState10()
        RememberKey_State10_stateofState10()
        RememberKey_State_State()
        RememberKey_State_State10()
        RememberKey_State10_State10()
        RememberKey_byState_byState()
        RememberKey_byState_byState10()
        RememberKey_byState10_byState10()
        RememberKey_byStateValue_byStateValue()
        RememberKey_byStateValue_byStateValue10()
        RememberKey_byStateValue10_byStateValue10()
        RememberKey_Param_stateofParam(externalValue)
        RememberKey_Param_stateofParam10(externalValue)
        RememberKey_Param10_stateofParam10(externalValue)
        RememberKey_Param_Param(externalValue)
        RememberKey_Param_Param10(externalValue)
        RememberKey_Param10_Param10(externalValue)
        Param(externalValue)
        Param10(externalValue)

        Text("")
        Text("generated: 2024-09-16_17-22-08")
        //invocations-end
    }
}

fun startCounter() {
    externalState.intValue = 0
    externalByState = 0
    externalValue = 0
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            externalValue++
            externalState.intValue = externalValue
            externalByState = externalValue
            delay(countTime.milliseconds)
        }
    }
}

@RunWith(AndroidJUnit4::class)
class Try_UI_derivedStateVsRememberKey {

    @get:Rule
    val test = createComposeRule()

    @Before
    fun setUp() {
        test.setContent {
            TheComposable()
        }
        test.onRoot().printToLog("root")
    }

    @Test
    fun test_composables() {

        startCounter()

        test.waitUntil(60000) {
            //externalValue >= 50000/countTime
            externalValue >= 200
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Try_UI_derivedStateVsRememberKey_Preview() {

    LaunchedEffect(true) {
        startCounter()
    }

    TheComposable()
}
