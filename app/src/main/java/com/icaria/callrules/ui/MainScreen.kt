package com.icaria.callrules.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icaria.callrules.R
import com.icaria.callrules.data.Condition
import com.icaria.callrules.data.Rule
import com.icaria.callrules.data.RuleAction
import com.icaria.callrules.data.RulesRepository
import com.icaria.callrules.data.ScreenedCall
import com.icaria.callrules.data.Store
import com.icaria.callrules.engine.PhoneNumbers
import kotlinx.coroutines.launch

enum class MainTab { RULES, HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    store: Store,
    history: List<ScreenedCall>,
    homeRegion: String,
    role: ScreeningRoleState,
    tab: MainTab,
    onTabChange: (MainTab) -> Unit,
    onEditRule: (Rule) -> Unit,
    onNewRule: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }

    val deleteWithUndo: (Rule, Int) -> Unit = { rule, index ->
        RulesRepository.deleteRule(rule.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.rule_deleted),
                actionLabel = context.getString(R.string.undo),
                withDismissAction = true,
            )
            if (result == SnackbarResult.ActionPerformed) RulesRepository.insertRuleAt(rule, index)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(if (tab == MainTab.RULES) R.string.title_rules else R.string.title_history))
                },
                actions = {
                    if (tab == MainTab.HISTORY && history.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.clear_history))
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.RULES,
                    onClick = { onTabChange(MainTab.RULES) },
                    icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_rules)) },
                )
                NavigationBarItem(
                    selected = tab == MainTab.HISTORY,
                    onClick = { onTabChange(MainTab.HISTORY) },
                    icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_history)) },
                )
            }
        },
        floatingActionButton = {
            if (tab == MainTab.RULES) {
                ExtendedFloatingActionButton(
                    onClick = onNewRule,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_rule)) },
                    expanded = scrollBehavior.state.collapsedFraction < 0.5f,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (tab) {
            MainTab.RULES -> RulesContent(store, homeRegion, role, padding, onEditRule, deleteWithUndo)
            MainTab.HISTORY -> HistoryContent(history, homeRegion, padding)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    RulesRepository.clearHistory()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

// --- Reglas ---

@Composable
private fun RulesContent(
    store: Store,
    homeRegion: String,
    role: ScreeningRoleState,
    padding: PaddingValues,
    onEditRule: (Rule) -> Unit,
    onDelete: (Rule, Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "status") {
            StatusCard(role, store.settings.filteringEnabled, homeRegion)
        }
        if (store.rules.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.rules_empty_title),
                    body = stringResource(R.string.rules_empty_body),
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            item(key = "hint") {
                Text(
                    text = stringResource(R.string.rules_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            itemsIndexed(store.rules, key = { _, rule -> rule.id }) { index, rule ->
                RuleCard(
                    rule = rule,
                    index = index,
                    count = store.rules.size,
                    homeRegion = homeRegion,
                    onClick = { onEditRule(rule) },
                    onDelete = { onDelete(rule, index) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(key = "templates") {
            QuickTemplates(store.rules)
        }
    }
}

@Composable
private fun StatusCard(role: ScreeningRoleState, filteringEnabled: Boolean, homeRegion: String) {
    if (!role.held) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.RemoveModerator, contentDescription = null)
                    Text(stringResource(R.string.status_inactive_title), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.status_inactive_body), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = role.request, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.status_activate))
                }
            }
        }
        return
    }

    val on = filteringEnabled
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .toggleable(
                    value = on,
                    role = Role.Switch,
                    onValueChange = { value -> RulesRepository.updateSettings { it.copy(filteringEnabled = value) } },
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = if (on) Icons.Rounded.Shield else Icons.Rounded.RemoveModerator,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (on) R.string.status_on else R.string.status_off),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.status_home, countryWithFlag(homeRegion)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = on, onCheckedChange = null)
        }
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    index: Int,
    count: Int,
    homeRegion: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalContext.current.resources
    val colors = actionColors(rule.action)
    var menuOpen by remember { mutableStateOf(false) }

    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.container)
                    .alpha(if (rule.enabled) 1f else 0.5f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(actionIcon(rule.action), contentDescription = null, tint = colors.content)
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (rule.enabled) 1f else 0.55f),
            ) {
                Text(
                    text = describeCondition(res, rule.condition, homeRegion),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val status = if (rule.enabled) actionLabel(res, rule.action) else
                    "${actionLabel(res, rule.action)} · ${stringResource(R.string.rule_disabled)}"
                Text(
                    text = stringResource(R.string.rule_position, index + 1, status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = { RulesRepository.setRuleEnabled(rule.id, it) },
                modifier = Modifier.padding(start = 8.dp),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_up)) },
                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                        enabled = index > 0,
                        onClick = {
                            menuOpen = false
                            RulesRepository.moveRule(rule.id, -1)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_down)) },
                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                        enabled = index < count - 1,
                        onClick = {
                            menuOpen = false
                            RulesRepository.moveRule(rule.id, 1)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

private data class Template(val label: Int, val condition: Condition, val action: RuleAction)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickTemplates(rules: List<Rule>) {
    val templates = listOf(
        Template(R.string.tpl_national, Condition.Foreign, RuleAction.BLOCK),
        Template(R.string.tpl_hidden, Condition.Hidden, RuleAction.BLOCK),
        Template(R.string.tpl_unknown_silence, Condition.AnyNumber, RuleAction.SILENCE),
    ).filter { template -> rules.none { it.condition == template.condition } }
    if (templates.isEmpty()) return

    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.quick_add),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            templates.forEach { template ->
                AssistChip(
                    onClick = {
                        RulesRepository.upsertRule(Rule(condition = template.condition, action = template.action))
                    },
                    label = { Text(stringResource(template.label)) },
                    leadingIcon = {
                        Icon(
                            conditionIcon(template.condition.type),
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

// --- Registro ---

@Composable
private fun HistoryContent(history: List<ScreenedCall>, homeRegion: String, padding: PaddingValues) {
    if (history.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.History,
            title = stringResource(R.string.history_empty_title),
            body = stringResource(R.string.history_empty_body),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
    ) {
        items(history) { call -> HistoryItem(call, homeRegion) }
    }
}

@Composable
private fun HistoryItem(call: ScreenedCall, homeRegion: String) {
    val res = LocalContext.current.resources
    val number = call.number?.let { PhoneNumbers.formatInternational(it, homeRegion) }
        ?: stringResource(R.string.hidden_number)
    val time = DateUtils.getRelativeTimeSpanString(
        call.time,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
    val where = listOfNotNull(call.region?.let { countryName(it) }, time).joinToString(" · ")
    val why = when {
        call.rule != null -> describeCondition(res, call.rule.condition, homeRegion)
        !call.filteringEnabled -> stringResource(R.string.history_filter_off)
        else -> stringResource(R.string.history_no_rule)
    }

    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (call.region != null) {
                    Text(PhoneNumbers.flag(call.region), fontSize = 22.sp)
                } else {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                }
            }
        },
        headlineContent = { Text(number) },
        supportingContent = {
            Column {
                Text(where)
                Text(why, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        trailingContent = { ActionBadge(call.action) },
    )
}

@Composable
private fun ActionBadge(action: RuleAction) {
    val res = LocalContext.current.resources
    val colors = actionColors(action)
    Surface(color = colors.container, contentColor = colors.content, shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(actionIcon(action), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(actionPastLabel(res, action), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// --- Comunes ---

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
