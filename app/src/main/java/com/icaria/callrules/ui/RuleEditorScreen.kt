package com.icaria.callrules.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.icaria.callrules.R
import com.icaria.callrules.data.Condition
import com.icaria.callrules.data.Rule
import com.icaria.callrules.data.RuleAction
import com.icaria.callrules.engine.PhoneNumbers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    initial: Rule?,
    homeRegion: String,
    onClose: () -> Unit,
    onSave: (Rule) -> Unit,
    onDelete: (Rule) -> Unit,
) {
    val res = LocalContext.current.resources
    var type by rememberSaveable { mutableStateOf(initial?.condition?.type ?: ConditionType.COUNTRIES) }
    var countries by rememberSaveable {
        mutableStateOf(ArrayList((initial?.condition as? Condition.Countries)?.regions.orEmpty().sorted()))
    }
    var prefix by rememberSaveable { mutableStateOf((initial?.condition as? Condition.Prefix)?.prefix.orEmpty()) }
    var action by rememberSaveable { mutableStateOf(initial?.action ?: RuleAction.BLOCK) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val condition: Condition? = when (type) {
        ConditionType.COUNTRIES -> countries.takeIf { it.isNotEmpty() }?.let { Condition.Countries(it.toSet()) }
        ConditionType.FOREIGN -> Condition.Foreign
        ConditionType.PREFIX -> prefix.trim()
            .takeIf { text -> PhoneNumbers.clean(text).any { it.isDigit() } }
            ?.let { Condition.Prefix(it) }
        ConditionType.HIDDEN -> Condition.Hidden
        ConditionType.ANY -> Condition.AnyNumber
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (initial == null) R.string.editor_new else R.string.editor_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    TextButton(
                        enabled = condition != null,
                        onClick = {
                            val newCondition = condition ?: return@TextButton
                            val rule = initial?.copy(condition = newCondition, action = action)
                                ?: Rule(condition = newCondition, action = action)
                            onSave(rule)
                        },
                    ) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionTitle(stringResource(R.string.editor_which_calls))
            Column(Modifier.selectableGroup()) {
                ConditionType.entries.forEach { option ->
                    ConditionOption(
                        type = option,
                        selected = type == option,
                        homeRegion = homeRegion,
                        onSelect = { type = option },
                    )
                }
            }

            AnimatedVisibility(visible = type == ConditionType.COUNTRIES) {
                CountriesField(
                    countries = countries,
                    onRemove = { code -> countries = ArrayList(countries - code) },
                    onAdd = { pickerOpen = true },
                )
            }
            AnimatedVisibility(visible = type == ConditionType.PREFIX) {
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text(stringResource(R.string.prefix_label)) },
                    supportingText = { Text(stringResource(R.string.prefix_help)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.editor_what_to_do))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RuleAction.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = action == option,
                        onClick = { action = option },
                        shape = SegmentedButtonDefaults.itemShape(index, RuleAction.entries.size),
                        icon = {
                            SegmentedButtonDefaults.Icon(
                                active = action == option,
                                inactiveContent = {
                                    Icon(
                                        actionIcon(option),
                                        contentDescription = null,
                                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                    )
                                },
                            )
                        },
                    ) { Text(actionLabel(res, option)) }
                }
            }
            Text(
                text = actionDescription(res, action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }

    if (pickerOpen) {
        CountryPickerSheet(
            initial = countries.toSet(),
            multiSelect = true,
            onDismiss = { pickerOpen = false },
            onConfirm = { selected ->
                countries = ArrayList(selected.sorted())
                pickerOpen = false
            },
        )
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_rule_title)) },
            text = { Text(describeCondition(res, initial.condition, homeRegion)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(initial)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun ConditionOption(type: ConditionType, selected: Boolean, homeRegion: String, onSelect: () -> Unit) {
    val (title, description) = when (type) {
        ConditionType.COUNTRIES -> stringResource(R.string.type_countries) to stringResource(R.string.type_countries_desc)
        ConditionType.FOREIGN -> stringResource(R.string.type_foreign) to
            stringResource(R.string.type_foreign_desc, countryName(homeRegion))
        ConditionType.PREFIX -> stringResource(R.string.type_prefix) to stringResource(R.string.type_prefix_desc)
        ConditionType.HIDDEN -> stringResource(R.string.type_hidden) to stringResource(R.string.type_hidden_desc)
        ConditionType.ANY -> stringResource(R.string.type_any) to stringResource(R.string.type_any_desc)
    }
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            conditionIcon(type),
            contentDescription = null,
            tint = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
            )
            Text(description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountriesField(countries: List<String>, onRemove: (String) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            countries.forEach { code ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(code) },
                    label = { Text(countryWithFlag(code)) },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.remove),
                            modifier = Modifier.size(InputChipDefaults.IconSize),
                        )
                    },
                )
            }
            AssistChip(
                onClick = onAdd,
                label = { Text(stringResource(R.string.countries_add)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                },
            )
        }
        if (countries.isEmpty()) {
            Text(
                stringResource(R.string.countries_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
