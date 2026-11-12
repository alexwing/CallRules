package com.icaria.callrules.engine

import com.icaria.callrules.data.Condition
import com.icaria.callrules.data.Rule
import com.icaria.callrules.data.RuleAction

data class Decision(val action: RuleAction, val rule: Rule?)

/**
 * Evalúa las reglas en orden: gana la primera activa que coincide.
 * Si ninguna coincide, la llamada pasa.
 */
object RuleEngine {

    fun evaluate(info: NumberInfo, rules: List<Rule>, homeRegion: String): Decision {
        val match = rules.firstOrNull { it.enabled && matches(it.condition, info, homeRegion) }
        return Decision(match?.action ?: RuleAction.ALLOW, match)
    }

    fun matches(condition: Condition, info: NumberInfo, homeRegion: String): Boolean = when (condition) {
        is Condition.Countries -> !info.hidden && originRegion(info) in condition.regions
        Condition.Foreign -> !info.hidden && isForeign(info, homeRegion)
        is Condition.Prefix -> !info.hidden && matchesPrefix(condition.prefix, info, homeRegion)
        Condition.Hidden -> info.hidden
        Condition.AnyNumber -> true
    }

    /** País exacto si se conoce; si no, el país principal del prefijo internacional. */
    private fun originRegion(info: NumberInfo): String? =
        info.region ?: info.countryCode?.let(PhoneNumbers::mainRegionFor)

    private fun isForeign(info: NumberInfo, homeRegion: String): Boolean {
        info.region?.let { return it != homeRegion }
        val countryCode = info.countryCode ?: return false
        return countryCode != PhoneNumbers.dialCode(homeRegion)
    }

    private fun matchesPrefix(prefix: String, info: NumberInfo, homeRegion: String): Boolean {
        val wanted = PhoneNumbers.clean(prefix)
        if (wanted.none { it.isDigit() }) return false
        if (wanted.startsWith("+")) {
            return info.e164?.startsWith(wanted) == true || info.digits?.startsWith(wanted) == true
        }
        // Sin "+": se compara con el número tal cual llega y con el número nacional.
        if (info.digits?.startsWith(wanted) == true) return true
        return info.countryCode == PhoneNumbers.dialCode(homeRegion) &&
            info.national?.startsWith(wanted) == true
    }
}
