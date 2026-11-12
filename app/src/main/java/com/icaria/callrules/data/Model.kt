package com.icaria.callrules.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class RuleAction { BLOCK, SILENCE, ALLOW }

/** Qué llamadas cubre una regla. */
@Serializable
sealed interface Condition {

    /** Llamadas cuyo número pertenece a alguno de estos países (códigos ISO, p. ej. "IT"). */
    @Serializable
    @SerialName("countries")
    data class Countries(val regions: Set<String>) : Condition

    /** Llamadas de cualquier país distinto del país de referencia. */
    @Serializable
    @SerialName("foreign")
    data object Foreign : Condition

    /** Números que empiezan por un prefijo ("+39", "+44 7", "900"...). */
    @Serializable
    @SerialName("prefix")
    data class Prefix(val prefix: String) : Condition

    /** Llamadas con el número oculto o sin número. */
    @Serializable
    @SerialName("hidden")
    data object Hidden : Condition

    /** Cualquier llamada que llegue al filtro (Android no le pasa las de contactos). */
    @Serializable
    @SerialName("any")
    data object AnyNumber : Condition
}

@Serializable
data class Rule(
    val id: String = UUID.randomUUID().toString(),
    val condition: Condition,
    val action: RuleAction,
    val enabled: Boolean = true,
)

@Serializable
data class Settings(
    val filteringEnabled: Boolean = true,
    val logBlockedInCallLog: Boolean = true,
    /** País de referencia elegido a mano; null = detectarlo por la SIM. */
    val homeRegionOverride: String? = null,
)

@Serializable
data class Store(
    val rules: List<Rule> = emptyList(),
    val settings: Settings = Settings(),
) {
    companion object {
        /** Primera ejecución: mantiene el comportamiento de Block +39. */
        val DEFAULT = Store(
            rules = listOf(
                Rule(
                    id = "default-italy",
                    condition = Condition.Countries(setOf("IT")),
                    action = RuleAction.BLOCK,
                ),
            ),
        )
    }
}

/** Una llamada que ha pasado por el filtro, con la regla que se le aplicó. */
@Serializable
data class ScreenedCall(
    val time: Long,
    val number: String?,
    val region: String?,
    val action: RuleAction,
    val rule: Rule? = null,
    val filteringEnabled: Boolean = true,
)

object StoreJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
}
