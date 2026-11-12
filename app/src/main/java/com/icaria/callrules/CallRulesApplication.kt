package com.icaria.callrules

import android.app.Application
import com.icaria.callrules.data.RulesRepository

class CallRulesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RulesRepository.init(this)
    }
}
