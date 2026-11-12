package com.icaria.callrules

import com.icaria.callrules.data.Condition
import com.icaria.callrules.data.Rule
import com.icaria.callrules.data.RuleAction
import com.icaria.callrules.data.Store
import com.icaria.callrules.data.StoreJson
import com.icaria.callrules.engine.PhoneNumbers
import com.icaria.callrules.engine.RuleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val home = "ES"

    private fun decide(number: String?, vararg rules: Rule, hidden: Boolean = false) =
        RuleEngine.evaluate(PhoneNumbers.analyze(number, home, hidden), rules.toList(), home)

    private val blockItaly = Rule(id = "it", condition = Condition.Countries(setOf("IT")), action = RuleAction.BLOCK)
    private val blockForeign = Rule(id = "foreign", condition = Condition.Foreign, action = RuleAction.BLOCK)

    @Test
    fun `numero movil italiano se bloquea por pais`() {
        assertEquals(RuleAction.BLOCK, decide("+393441932623", blockItaly).action)
        assertEquals(RuleAction.BLOCK, decide("0039 344 193 2623", blockItaly).action)
    }

    @Test
    fun `fijo italiano con cero inicial se reconoce como Italia`() {
        val info = PhoneNumbers.analyze("+39 06 1234 5678", home)
        assertEquals("IT", info.region)
        assertEquals("0612345678", info.national)
        assertEquals(RuleAction.BLOCK, decide("+390612345678", blockItaly).action)
    }

    @Test
    fun `numero espanol no coincide con regla de Italia`() {
        val decision = decide("+34612345678", blockItaly)
        assertEquals(RuleAction.ALLOW, decision.action)
        assertNull(decision.rule)
    }

    @Test
    fun `solo nacionales deja pasar Espana y bloquea el resto`() {
        assertEquals(RuleAction.ALLOW, decide("+34612345678", blockForeign).action)
        assertEquals(RuleAction.ALLOW, decide("612345678", blockForeign).action)
        assertEquals(RuleAction.ALLOW, decide("0034 912 345 678", blockForeign).action)
        assertEquals(RuleAction.BLOCK, decide("+447911123456", blockForeign).action)
        assertEquals(RuleAction.BLOCK, decide("+12025550123", blockForeign).action)
    }

    @Test
    fun `numero no valido con prefijo conocido usa el pais del prefijo`() {
        // +39 con un número nacional imposible: libphonenumber no da región exacta.
        val info = PhoneNumbers.analyze("+39 1", home)
        assertNull(info.region)
        assertEquals(39, info.countryCode)
        assertEquals(RuleAction.BLOCK, decide("+39 1", blockItaly).action)
        assertEquals(RuleAction.BLOCK, decide("+39 1", blockForeign).action)
    }

    @Test
    fun `numeros ocultos solo coinciden con la regla de ocultos`() {
        val hiddenRule = Rule(condition = Condition.Hidden, action = RuleAction.BLOCK)
        assertEquals(RuleAction.BLOCK, decide(null, hiddenRule).action)
        assertEquals(RuleAction.BLOCK, decide("", hiddenRule).action)
        assertEquals(RuleAction.BLOCK, decide("+393441932623", hiddenRule, hidden = true).action)
        assertEquals(RuleAction.ALLOW, decide(null, blockItaly, blockForeign).action)
        assertEquals(RuleAction.ALLOW, decide("+393441932623", blockItaly, hidden = true).action)
    }

    @Test
    fun `prefijo internacional y nacional`() {
        val uk = Rule(condition = Condition.Prefix("+44 7"), action = RuleAction.BLOCK)
        assertEquals(RuleAction.BLOCK, decide("+447911123456", uk).action)
        assertEquals(RuleAction.ALLOW, decide("+442071234567", uk).action)

        val premium = Rule(condition = Condition.Prefix("900"), action = RuleAction.SILENCE)
        assertEquals(RuleAction.SILENCE, decide("+34900123456", premium).action)
        assertEquals(RuleAction.SILENCE, decide("900123456", premium).action)
        // Un 900 de otro país no es el 900 nacional.
        assertEquals(RuleAction.ALLOW, decide("+44900123456", premium).action)
    }

    @Test
    fun `prefijo vacio o sin digitos no coincide nunca`() {
        val empty = Rule(condition = Condition.Prefix(" + "), action = RuleAction.BLOCK)
        assertEquals(RuleAction.ALLOW, decide("+393441932623", empty).action)
    }

    @Test
    fun `gana la primera regla y las desactivadas se ignoran`() {
        val allowRome = Rule(id = "rome", condition = Condition.Prefix("+39 06"), action = RuleAction.ALLOW)
        assertEquals(RuleAction.ALLOW, decide("+390612345678", allowRome, blockItaly).action)
        assertEquals("rome", decide("+390612345678", allowRome, blockItaly).rule?.id)
        assertEquals(RuleAction.BLOCK, decide("+393441932623", allowRome, blockItaly).action)

        val disabled = blockItaly.copy(enabled = false)
        assertEquals(RuleAction.ALLOW, decide("+393441932623", disabled).action)
    }

    @Test
    fun `cualquier numero coincide con todo`() {
        val any = Rule(condition = Condition.AnyNumber, action = RuleAction.SILENCE)
        assertEquals(RuleAction.SILENCE, decide("+34612345678", any).action)
        assertEquals(RuleAction.SILENCE, decide(null, any).action)
    }

    @Test
    fun `limpieza de numeros`() {
        assertEquals("+393441932623", PhoneNumbers.clean("0039 (344) 193-2623"))
        assertEquals("+44", PhoneNumbers.clean("+4+4"))
        assertEquals("", PhoneNumbers.clean("abc"))
    }

    @Test
    fun `banderas`() {
        assertEquals("🇮🇹", PhoneNumbers.flag("IT"))
        assertEquals("🇪🇸", PhoneNumbers.flag("es"))
    }

    @Test
    fun `el almacen se guarda y se recupera en JSON`() {
        val store = Store.DEFAULT.copy(
            rules = Store.DEFAULT.rules + listOf(
                Rule(id = "a", condition = Condition.Foreign, action = RuleAction.BLOCK, enabled = false),
                Rule(id = "b", condition = Condition.Prefix("+44"), action = RuleAction.SILENCE),
                Rule(id = "c", condition = Condition.Hidden, action = RuleAction.BLOCK),
                Rule(id = "d", condition = Condition.AnyNumber, action = RuleAction.ALLOW),
            ),
        )
        val text = StoreJson.json.encodeToString(Store.serializer(), store)
        assertTrue(text.contains("\"type\":\"countries\""))
        assertEquals(store, StoreJson.json.decodeFromString(Store.serializer(), text))
    }

    @Test
    fun `JSON con campos desconocidos no rompe la carga`() {
        val text = """{"rules":[{"id":"x","condition":{"type":"hidden"},"action":"BLOCK","nuevo":1}],"settings":{},"otro":true}"""
        val store = StoreJson.json.decodeFromString(Store.serializer(), text)
        assertEquals(1, store.rules.size)
        assertFalse(store.rules.isEmpty())
        assertTrue(store.settings.filteringEnabled)
    }
}
