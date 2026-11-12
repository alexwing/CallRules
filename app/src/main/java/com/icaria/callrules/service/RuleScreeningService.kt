package com.icaria.callrules.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.util.Log
import com.icaria.callrules.data.RuleAction
import com.icaria.callrules.data.RulesRepository
import com.icaria.callrules.data.ScreenedCall
import com.icaria.callrules.engine.Decision
import com.icaria.callrules.engine.HomeRegion
import com.icaria.callrules.engine.PhoneNumbers
import com.icaria.callrules.engine.RuleEngine

/**
 * Android llama a este servicio en cada llamada entrante que no sea de un contacto,
 * siempre que la app tenga el rol de filtrado de llamadas (ROLE_CALL_SCREENING).
 */
class RuleScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, CallResponse.Builder().build())
            return
        }

        RulesRepository.init(this)
        val store = RulesRepository.store.value
        val settings = store.settings
        val homeRegion = HomeRegion.resolve(this, settings)

        val raw = details.handle
            ?.takeIf { it.scheme == PhoneAccount.SCHEME_TEL }
            ?.schemeSpecificPart
        val hidden = details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        val info = PhoneNumbers.analyze(raw, homeRegion, hidden)

        val decision = if (settings.filteringEnabled) {
            RuleEngine.evaluate(info, store.rules, homeRegion)
        } else {
            Decision(RuleAction.ALLOW, null)
        }

        val response = CallResponse.Builder()
        when (decision.action) {
            RuleAction.BLOCK -> response
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipNotification(true)
                .setSkipCallLog(!settings.logBlockedInCallLog)
            RuleAction.SILENCE -> response.setSilenceCall(true)
            RuleAction.ALLOW -> Unit
        }
        respondToCall(details, response.build())

        Log.i(
            TAG,
            "Llamada número=${info.e164 ?: raw} país=${info.region} acción=${decision.action} " +
                "regla=${decision.rule?.id} filtro=${settings.filteringEnabled}",
        )
        RulesRepository.record(
            ScreenedCall(
                time = System.currentTimeMillis(),
                number = if (info.hidden) null else info.e164 ?: raw,
                region = if (info.hidden) null else info.region,
                action = decision.action,
                rule = decision.rule,
                filteringEnabled = settings.filteringEnabled,
            ),
        )
    }

    private companion object {
        const val TAG = "CallRules"
    }
}
