package com.icaria.callrules.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.icaria.callrules.R
import com.icaria.callrules.data.RulesRepository
import com.icaria.callrules.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    homeRegion: String,
    detectedRegion: String?,
    role: ScreeningRoleState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.section_filter))
            SwitchItem(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.set_filtering),
                summary = stringResource(R.string.set_filtering_desc),
                checked = settings.filteringEnabled,
                onCheckedChange = { value -> RulesRepository.updateSettings { it.copy(filteringEnabled = value) } },
            )
            SwitchItem(
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                title = stringResource(R.string.set_calllog),
                summary = stringResource(R.string.set_calllog_desc),
                checked = settings.logBlockedInCallLog,
                onCheckedChange = { value -> RulesRepository.updateSettings { it.copy(logBlockedInCallLog = value) } },
            )
            ListItem(
                leadingContent = { Icon(Icons.Rounded.Public, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.set_home)) },
                supportingContent = {
                    Text(
                        if (settings.homeRegionOverride == null) {
                            stringResource(R.string.set_home_auto, countryWithFlag(homeRegion))
                        } else {
                            stringResource(R.string.set_home_manual, countryWithFlag(homeRegion))
                        },
                    )
                },
                modifier = Modifier.clickable { pickerOpen = true },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.section_system))
            ListItem(
                leadingContent = { Icon(Icons.Rounded.Shield, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.set_role)) },
                supportingContent = {
                    Text(stringResource(if (role.held) R.string.set_role_active else R.string.set_role_inactive))
                },
                modifier = Modifier.clickable {
                    if (!role.held) {
                        role.request()
                    } else {
                        try {
                            context.startActivity(Intent(SystemSettings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                        } catch (e: ActivityNotFoundException) {
                            role.request()
                        }
                    }
                },
            )
            ListItem(
                leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.set_about)) },
                supportingContent = { Text(version) },
            )
        }
    }

    if (pickerOpen) {
        CountryPickerSheet(
            initial = setOfNotNull(settings.homeRegionOverride),
            multiSelect = false,
            autoLabel = detectedRegion?.let { stringResource(R.string.auto_country, countryWithFlag(it)) }
                ?: stringResource(R.string.auto_country_unknown),
            onDismiss = { pickerOpen = false },
            onConfirm = { selection ->
                RulesRepository.updateSettings { it.copy(homeRegionOverride = selection.firstOrNull()) }
                pickerOpen = false
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchItem(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}
