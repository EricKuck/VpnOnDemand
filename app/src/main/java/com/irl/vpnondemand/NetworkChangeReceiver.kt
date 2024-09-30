package com.irl.vpnondemand

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkChangeReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    context.startService(Intent(context, StartupService::class.java))
  }
}
