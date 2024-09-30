@file:Suppress("DEPRECATION")

package com.irl.vpnondemand

import android.app.IntentService
import android.content.Intent
import kotlinx.coroutines.runBlocking

class StartupService : IntentService("StartupService") {
  @Suppress("OVERRIDE_DEPRECATION")
  override fun onHandleIntent(intent: Intent?) {
    runBlocking {
      SsidHelper.instance(this@StartupService).onNetworkChange()
    }
  }
}

const val TAG = "VpnOnDemand"
