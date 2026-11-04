package tests.tests_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.machiav3lli.backup.ui.activities.NeoActivity
import com.machiav3lli.backup.ui.compose.isAtBottom
import com.machiav3lli.backup.ui.pages.TerminalText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration.Companion.milliseconds

//val queue1 = mutableStateListOf<String>()
val queue1 = mutableListOf<String>()
val queue2 = ConcurrentLinkedQueue<String>()
var counter = 0

val init = run {
    queue1.add("aaa")
    queue2.add("aaa")
    GlobalScope.launch {
        repeat(20) {
            counter += 1
            delay(50.milliseconds)
            queue1.add("some added text $counter")
            queue2.add("some added text $counter")
        }
    }
}


@RunWith(AndroidJUnit4::class)
class Test_UI_FollowNonStateVariable {

    @get:Rule
    val test = createAndroidComposeRule<NeoActivity>()

    @Before
    fun setUp() {
        test.setContent {
            Column {
                TestPreview()
                //TerminalTextPreview()
            }
        }
        //test.onRoot().printToLog("root")
    }

    @Test
    fun test_findList() {

        test.waitForIdle()
        test.waitUntil(10000) {
            test.onAllNodesWithText("some added text 20").fetchSemanticsNodes().count() >= 2
        }
    }
}

@Preview
@Composable
fun TerminalTextPreview() {

    Timber.d("recompose ${queue2.size}")

    val lines = queue2

    var recompose by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        launch {
            while (true) {
                delay(100.milliseconds)
                recompose++
            }
        }
    }

    Box(
        modifier = Modifier
            .height(300.dp)
            .width(500.dp)
            .padding(0.dp)
            .background(color = Color(0.2f, 0.2f, 0.3f))
    ) {
        TerminalText(lines.toList())
    }
}

@Preview
@Composable
fun TestPreview() {

    Timber.d("recompose ${queue1.size} ${queue2.size}")

    val lines1 = queue1
    val lines2 = queue2

    val listState = rememberLazyListState()

    var recompose by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        launch {
            while (true) {
                delay(100.milliseconds)
                recompose++
            }
        }
    }

    if (listState.isAtBottom())
        LaunchedEffect(recompose) {
            launch {
                listState.scrollToItem(Int.MAX_VALUE)
            }
        }

    Box(
        modifier = Modifier
            .height(300.dp)
            .width(500.dp)
            .padding(0.dp)
            .background(color = Color(0.2f, 0.2f, 0.3f))
    ) {
        Row {
            LazyColumn(
                modifier = Modifier.weight(0.5f),
                state = listState
            ) {
                item {
                    Text(
                        "${queue1::class.simpleName}",
                        color = Color.White, fontSize = 10.sp
                    )
                }
                items(lines1) {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(0.5f),
                state = listState
            ) {
                item {
                    Text(
                        "${queue2::class.simpleName}",
                        color = Color.White, fontSize = 10.sp
                    )
                }
                items(lines2.toList()) {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White
                    )
                }
            }
        }
    }
}

