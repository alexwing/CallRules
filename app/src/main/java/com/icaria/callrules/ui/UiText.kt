package com.icaria.callrules.ui

import android.content.res.Resources
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.icaria.callrules.R
import com.icaria.callrules.data.Condition
import com.icaria.callrules.data.RuleAction
import com.icaria.callrules.engine.PhoneNumbers
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

enum class ConditionType { COUNTRIES, FOREIGN, PREFIX, HIDDEN, ANY }

val Condition.type: ConditionType
    get() = when (this) {
        is Condition.Countries -> ConditionType.COUNTRIES
        Condition.Foreign -> ConditionType.FOREIGN
        is Condition.Prefix -> ConditionType.PREFIX
        Condition.Hidden -> ConditionType.HIDDEN
        Condition.AnyNumber -> ConditionType.ANY
    }

fun countryName(region: String, locale: Locale = Locale.getDefault()): String {
    @Suppress("DEPRECATION")
    val name = Locale("", region).getDisplayCountry(locale)
    return name.ifBlank { region }
}

fun countryWithFlag(region: String): String = "${PhoneNumbers.flag(region)} ${countryName(region)}"

fun describeCondition(res: Resources, condition: Condition, homeRegion: String): String =
    when (condition) {
        is Condition.Countries -> {
            val collator = Collator.getInstance()
            val names = condition.regions.map { countryName(it) }.sortedWith(collator)
            when {
                names.isEmpty() -> res.getString(R.string.cond_countries_empty)
                names.size <= 3 -> res.getString(R.string.cond_countries, names.joinToString(", "))
                else -> res.getString(R.string.cond_countries_more, names.take(2).joinToString(", "), names.size - 2)
            }
        }
        Condition.Foreign -> res.getString(R.string.cond_foreign, countryName(homeRegion))
        is Condition.Prefix -> res.getString(R.string.cond_prefix, condition.prefix.trim())
        Condition.Hidden -> res.getString(R.string.cond_hidden)
        Condition.AnyNumber -> res.getString(R.string.cond_any)
    }

fun actionLabel(res: Resources, action: RuleAction): String = res.getString(
    when (action) {
        RuleAction.BLOCK -> R.string.action_block
        RuleAction.SILENCE -> R.string.action_silence
        RuleAction.ALLOW -> R.string.action_allow
    },
)

fun actionPastLabel(res: Resources, action: RuleAction): String = res.getString(
    when (action) {
        RuleAction.BLOCK -> R.string.action_block_past
        RuleAction.SILENCE -> R.string.action_silence_past
        RuleAction.ALLOW -> R.string.action_allow_past
    },
)

fun actionDescription(res: Resources, action: RuleAction): String = res.getString(
    when (action) {
        RuleAction.BLOCK -> R.string.action_block_desc
        RuleAction.SILENCE -> R.string.action_silence_desc
        RuleAction.ALLOW -> R.string.action_allow_desc
    },
)

fun actionIcon(action: RuleAction): ImageVector = when (action) {
    RuleAction.BLOCK -> Icons.Rounded.Block
    RuleAction.SILENCE -> Icons.Rounded.NotificationsOff
    RuleAction.ALLOW -> Icons.Rounded.CheckCircle
}

fun conditionIcon(type: ConditionType): ImageVector = when (type) {
    ConditionType.COUNTRIES -> Icons.Rounded.Flag
    ConditionType.FOREIGN -> Icons.Rounded.Public
    ConditionType.PREFIX -> Icons.Rounded.Dialpad
    ConditionType.HIDDEN -> Icons.Rounded.VisibilityOff
    ConditionType.ANY -> Icons.Rounded.PersonOff
}

/** Colores de contenedor y contenido para cada acción. */
data class ActionColors(val container: Color, val content: Color)

@Composable
@ReadOnlyComposable
fun actionColors(action: RuleAction): ActionColors {
    val scheme = MaterialTheme.colorScheme
    return when (action) {
        RuleAction.BLOCK -> ActionColors(scheme.errorContainer, scheme.onErrorContainer)
        RuleAction.SILENCE -> ActionColors(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        RuleAction.ALLOW -> ActionColors(scheme.primaryContainer, scheme.onPrimaryContainer)
    }
}

/** Para buscar sin tildes ni mayúsculas: "Japon" encuentra "Japón". */
fun String.searchKey(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
