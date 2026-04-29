package com.machiav3lli.backup.ui.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.machiav3lli.backup.R
import com.machiav3lli.backup.ui.compose.component.CarouselIndicators
import com.machiav3lli.backup.ui.compose.component.TopBar
import com.machiav3lli.backup.utils.allPermissionsGranted
import kotlinx.coroutines.launch

@Composable
fun OnboardingPage(onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state = rememberPagerState { 3 }

    fun animateToPage(page: Int) {
        persist_pageOnboarded.value = page
        scope.launch { state.animateScrollToPage(page) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopBar(
                title = stringResource(
                    id = R.string.setup_FORMAT,
                    when (state.currentPage) {
                        2 -> stringResource(id = R.string.prefs_title)
                        1 -> stringResource(id = R.string.permissions)
                        else -> stringResource(id = R.string.app_info)
                    }
                )
            ) {
                CarouselIndicators(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    size = state.pageCount,
                    state = state,
                    enableScrolling = false,
                )
            }
        }
    ) { paddingValues ->
        if (context.allPermissionsGranted && persist_pageOnboarded.value > 2)
            onComplete()
        else Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                modifier = Modifier.weight(1f),
                state = state,
                userScrollEnabled = false,
            ) { page ->
                when (page) {
                    0 -> WelcomePage {
                        animateToPage(1)
                    }

                    1 -> PermissionsPage {
                        if (persist_pageOnboarded.value > 1) onComplete()
                        else animateToPage(2)
                    }

                    2 -> OnboardingPrefsPage {
                        animateToPage(3)
                        onComplete()
                    }
                }
            }
        }
    }
}