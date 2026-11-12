package com.icaria.callrules.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icaria.callrules.R
import com.icaria.callrules.engine.PhoneNumbers
import java.text.Collator

data class Country(val code: String, val name: String, val dialCode: Int) {
    val flag: String get() = PhoneNumbers.flag(code)
    val searchKey: String = name.searchKey()
}

fun allCountries(): List<Country> {
    val collator = Collator.getInstance()
    return PhoneNumbers.regions()
        .map { Country(it, countryName(it), PhoneNumbers.dialCode(it)) }
        .sortedWith(compareBy(collator) { it.name })
}

/**
 * Hoja inferior con buscador de países.
 * Multiselección: se confirma con «Listo» o al cerrar la hoja.
 * Selección única: se confirma al tocar un país; [autoLabel] añade la opción «Automático»
 * (devuelve un conjunto vacío).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerSheet(
    initial: Set<String>,
    multiSelect: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    autoLabel: String? = null,
) {
    val countries = remember { allCountries() }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initial) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filtered = remember(query, countries) {
        val text = query.trim()
        val digits = text.removePrefix("+")
        when {
            text.isEmpty() -> countries
            digits.isNotEmpty() && digits.all { it.isDigit() } ->
                countries.filter { it.dialCode.toString().startsWith(digits) }
            else -> {
                val key = text.searchKey()
                countries.filter { it.searchKey.contains(key) || it.code.equals(text, ignoreCase = true) }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (multiSelect) onConfirm(selected) else onDismiss() },
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxHeight(0.9f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (multiSelect) R.string.country_picker_title else R.string.country_picker_title_single),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (multiSelect) {
                    FilledTonalButton(onClick = { onConfirm(selected) }) {
                        Text(stringResource(R.string.done_count, selected.size))
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.country_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            LazyColumn(Modifier.fillMaxWidth()) {
                if (autoLabel != null && query.isBlank()) {
                    item(key = "auto") {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            leadingContent = { Icon(Icons.Rounded.MyLocation, contentDescription = null) },
                            headlineContent = { Text(autoLabel) },
                            trailingContent = { RadioButton(selected = selected.isEmpty(), onClick = null) },
                            modifier = Modifier.clickable { onConfirm(emptySet()) },
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    item(key = "none") {
                        Text(
                            stringResource(R.string.no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(filtered, key = { it.code }) { country ->
                    val checked = country.code in selected
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        leadingContent = { Text(country.flag, fontSize = 24.sp) },
                        headlineContent = { Text(country.name) },
                        supportingContent = { Text("+${country.dialCode}") },
                        trailingContent = {
                            if (multiSelect) Checkbox(checked = checked, onCheckedChange = null)
                            else RadioButton(selected = checked, onClick = null)
                        },
                        modifier = Modifier.clickable {
                            if (multiSelect) {
                                selected = if (checked) selected - country.code else selected + country.code
                            } else {
                                onConfirm(setOf(country.code))
                            }
                        },
                    )
                }
            }
        }
    }
}
