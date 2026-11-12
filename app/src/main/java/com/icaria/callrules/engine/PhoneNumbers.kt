package com.icaria.callrules.engine

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat

/** Lo que sabemos del número de una llamada entrante. */
data class NumberInfo(
    val raw: String?,
    /** Dígitos con un "+" inicial opcional ("00" ya convertido en "+"). */
    val digits: String?,
    val e164: String?,
    /** Número nacional significativo, sin prefijo de país. */
    val national: String?,
    val countryCode: Int?,
    /** País exacto del número (ISO 3166), si libphonenumber lo reconoce. */
    val region: String?,
    val hidden: Boolean,
)

object PhoneNumbers {
    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    /** Deja solo dígitos y un "+" inicial; "00" internacional pasa a "+". */
    fun clean(input: String): String {
        val kept = input.filter { it.isDigit() || it == '+' }
        val singlePlus = kept.take(1) + kept.drop(1).replace("+", "")
        return if (singlePlus.startsWith("00")) "+" + singlePlus.drop(2) else singlePlus
    }

    fun analyze(raw: String?, homeRegion: String, presentationHidden: Boolean = false): NumberInfo {
        val digits = raw?.let(::clean)?.takeIf { s -> s.any { it.isDigit() } }
            ?: return NumberInfo(raw, null, null, null, null, null, hidden = true)
        return try {
            val parsed = util.parse(digits, homeRegion)
            NumberInfo(
                raw = raw,
                digits = digits,
                e164 = util.format(parsed, PhoneNumberFormat.E164),
                national = util.getNationalSignificantNumber(parsed),
                countryCode = parsed.countryCode,
                region = util.getRegionCodeForNumber(parsed)?.takeIf { it != UNKNOWN_REGION },
                hidden = presentationHidden,
            )
        } catch (e: NumberParseException) {
            NumberInfo(raw, digits, null, null, knownCountryCode(digits), null, presentationHidden)
        }
    }

    /** Prefijo internacional conocido aunque el número no sea válido ("+39 1" → 39). */
    private fun knownCountryCode(digits: String): Int? {
        if (!digits.startsWith("+")) return null
        val rest = digits.drop(1)
        return (1..minOf(3, rest.length))
            .map { rest.take(it).toInt() }
            .firstOrNull { util.getRegionCodeForCountryCode(it) != UNKNOWN_REGION }
    }

    /** País principal de un prefijo internacional (39 → IT, 1 → US). */
    fun mainRegionFor(countryCode: Int): String? =
        util.getRegionCodeForCountryCode(countryCode).takeIf { it != UNKNOWN_REGION }

    fun dialCode(region: String): Int = util.getCountryCodeForRegion(region)

    fun regions(): Set<String> = util.supportedRegions

    fun formatInternational(number: String, homeRegion: String): String = try {
        util.format(util.parse(number, homeRegion), PhoneNumberFormat.INTERNATIONAL)
    } catch (e: NumberParseException) {
        number
    }

    /** Bandera emoji a partir del código ISO de dos letras. */
    fun flag(region: String): String {
        val code = region.uppercase()
        if (code.length != 2 || !code.all { it in 'A'..'Z' }) return "🌐"
        return code.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    }

    private const val UNKNOWN_REGION = "ZZ"
}
