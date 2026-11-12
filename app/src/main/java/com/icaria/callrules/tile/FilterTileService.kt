package com.icaria.callrules.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.icaria.callrules.R
import com.icaria.callrules.data.RulesRepository

/** Mosaico de ajustes rápidos para pausar o reanudar el filtrado. */
class FilterTileService : TileService() {

    override fun onStartListening() {
        RulesRepository.init(this)
        render(RulesRepository.store.value.settings.filteringEnabled)
    }

    override fun onClick() {
        RulesRepository.init(this)
        val enabled = !RulesRepository.store.value.settings.filteringEnabled
        RulesRepository.updateSettings { it.copy(filteringEnabled = enabled) }
        render(enabled)
    }

    private fun render(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = getString(if (enabled) R.string.tile_on else R.string.tile_off)
        tile.updateTile()
    }
}
