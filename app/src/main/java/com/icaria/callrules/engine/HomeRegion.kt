package com.icaria.callrules.engine

import android.content.Context
import android.telephony.TelephonyManager
import com.icaria.callrules.data.Settings
import java.util.Locale

/** País de referencia para "llamadas nacionales": ajuste manual, SIM, red o idioma. */
object HomeRegion {

    fun detect(context: Context): String? {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        return listOf(telephony.simCountryIso, telephony.networkCountryIso)
            .firstOrNull { !it.isNullOrBlank() && it.length == 2 }
            ?.uppercase()
    }

    fun resolve(context: Context, settings: Settings): String =
        settings.homeRegionOverride
            ?: detect(context)
            ?: Locale.getDefault().country.takeIf { it.length == 2 }
            ?: "ES"
}
