package com.icaria.callrules.ui

import android.app.role.RoleManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.icaria.callrules.data.Rule
import com.icaria.callrules.data.RulesRepository
import com.icaria.callrules.engine.HomeRegion

private sealed interface Screen {
    data object Main : Screen
    data object Settings : Screen
    data class Editor(val rule: Rule?) : Screen
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val store by RulesRepository.store.collectAsStateWithLifecycle()
    val history by RulesRepository.history.collectAsStateWithLifecycle()
    val detectedRegion = remember { HomeRegion.detect(context) }
    val homeRegion = remember(store.settings.homeRegionOverride) { HomeRegion.resolve(context, store.settings) }
    val role = rememberScreeningRole()

    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var tab by rememberSaveable { mutableStateOf(MainTab.RULES) }

    BackHandler(enabled = screen != Screen.Main) { screen = Screen.Main }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val forward = targetState != Screen.Main
            val direction = if (forward) 1 else -1
            (fadeIn() + slideInHorizontally { direction * it / 5 }) togetherWith
                (fadeOut() + slideOutHorizontally { -direction * it / 5 })
        },
        label = "screen",
    ) { current ->
        when (current) {
            Screen.Main -> MainScreen(
                store = store,
                history = history,
                homeRegion = homeRegion,
                role = role,
                tab = tab,
                onTabChange = { tab = it },
                onEditRule = { screen = Screen.Editor(it) },
                onNewRule = { screen = Screen.Editor(null) },
                onOpenSettings = { screen = Screen.Settings },
            )
            Screen.Settings -> SettingsScreen(
                settings = store.settings,
                homeRegion = homeRegion,
                detectedRegion = detectedRegion,
                role = role,
                onBack = { screen = Screen.Main },
            )
            is Screen.Editor -> RuleEditorScreen(
                initial = current.rule,
                homeRegion = homeRegion,
                onClose = { screen = Screen.Main },
                onSave = {
                    RulesRepository.upsertRule(it)
                    screen = Screen.Main
                },
                onDelete = {
                    RulesRepository.deleteRule(it.id)
                    screen = Screen.Main
                },
            )
        }
    }
}

/** Si la app tiene el rol de filtrado de llamadas y cómo pedirlo. */
class ScreeningRoleState(val held: Boolean, val request: () -> Unit)

@Composable
fun rememberScreeningRole(): ScreeningRoleState {
    val context = LocalContext.current
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    fun isHeld() = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    var held by remember { mutableStateOf(isHeld()) }
    LifecycleResumeEffect(Unit) {
        held = isHeld()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        held = isHeld()
    }
    return ScreeningRoleState(held) {
        roleManager?.let { launcher.launch(it.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)) }
    }
}
