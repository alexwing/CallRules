package com.icaria.callrules.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException

/**
 * Reglas, ajustes e historial, guardados como JSON en el almacenamiento interno.
 *
 * El estado vive en memoria (StateFlow) y se escribe a disco en segundo plano; la
 * interfaz, el servicio de filtrado y el mosaico de ajustes rápidos comparten proceso
 * y por tanto la misma instancia.
 */
object RulesRepository {
    private const val TAG = "CallRules"
    private const val STORE_FILE = "rules.json"
    private const val HISTORY_FILE = "history.json"
    private const val MAX_HISTORY = 300

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storeMutex = Mutex()
    private val historyMutex = Mutex()
    private lateinit var dir: File

    @Volatile
    private var initialized = false

    private val _store = MutableStateFlow(Store.DEFAULT)
    val store: StateFlow<Store> = _store.asStateFlow()

    private val _history = MutableStateFlow<List<ScreenedCall>>(emptyList())
    val history: StateFlow<List<ScreenedCall>> = _history.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            dir = context.applicationContext.filesDir
            _store.value = readFile(STORE_FILE)
                ?.let { runCatching { StoreJson.json.decodeFromString<Store>(it) }.getOrNull() }
                ?: Store.DEFAULT
            _history.value = readFile(HISTORY_FILE)
                ?.let { runCatching { StoreJson.json.decodeFromString<List<ScreenedCall>>(it) }.getOrNull() }
                ?: emptyList()
            initialized = true
        }
    }

    // --- Reglas ---

    fun upsertRule(rule: Rule) = updateStore { store ->
        val index = store.rules.indexOfFirst { it.id == rule.id }
        val rules = if (index >= 0) {
            store.rules.toMutableList().also { it[index] = rule }
        } else {
            store.rules + rule
        }
        store.copy(rules = rules)
    }

    fun insertRuleAt(rule: Rule, index: Int) = updateStore { store ->
        if (store.rules.any { it.id == rule.id }) return@updateStore store
        val rules = store.rules.toMutableList()
        rules.add(index.coerceIn(0, rules.size), rule)
        store.copy(rules = rules)
    }

    fun deleteRule(id: String) = updateStore { store ->
        store.copy(rules = store.rules.filterNot { it.id == id })
    }

    fun setRuleEnabled(id: String, enabled: Boolean) = updateStore { store ->
        store.copy(rules = store.rules.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun moveRule(id: String, delta: Int) = updateStore { store ->
        val rules = store.rules.toMutableList()
        val from = rules.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in rules.indices) return@updateStore store
        rules.add(to, rules.removeAt(from))
        store.copy(rules = rules)
    }

    fun updateSettings(transform: (Settings) -> Settings) = updateStore { store ->
        store.copy(settings = transform(store.settings))
    }

    // --- Historial ---

    fun record(call: ScreenedCall) {
        _history.update { (listOf(call) + it).take(MAX_HISTORY) }
        persist(HISTORY_FILE, historyMutex) { StoreJson.json.encodeToString(_history.value) }
    }

    fun clearHistory() {
        _history.value = emptyList()
        persist(HISTORY_FILE, historyMutex) { StoreJson.json.encodeToString(_history.value) }
    }

    // --- Persistencia ---

    private fun updateStore(transform: (Store) -> Store) {
        _store.update(transform)
        persist(STORE_FILE, storeMutex) { StoreJson.json.encodeToString(_store.value) }
    }

    /** Escribe siempre el valor más reciente; el mutex mantiene el orden de escrituras. */
    private fun persist(name: String, mutex: Mutex, encode: () -> String) {
        if (!initialized) return
        scope.launch {
            mutex.withLock { writeFile(name, encode()) }
        }
    }

    private fun readFile(name: String): String? = runCatching {
        val file = File(dir, name)
        if (!file.exists() && !File(dir, "$name.bak").exists()) return null
        AtomicFile(file).readFully().decodeToString()
    }.getOrNull()

    private fun writeFile(name: String, text: String) {
        val file = AtomicFile(File(dir, name))
        val out = try {
            file.startWrite()
        } catch (e: IOException) {
            Log.w(TAG, "No se pudo abrir $name para escribir", e)
            return
        }
        try {
            out.write(text.encodeToByteArray())
            file.finishWrite(out)
        } catch (e: IOException) {
            file.failWrite(out)
            Log.w(TAG, "No se pudo guardar $name", e)
        }
    }
}
