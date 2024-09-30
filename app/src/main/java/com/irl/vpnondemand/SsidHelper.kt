package com.irl.vpnondemand

import android.annotation.SuppressLint
import android.app.IntentService.WIFI_SERVICE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class SsidHelper private constructor(private val context: Context) {

  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

  private val _currentSsid = MutableStateFlow<String?>(null)
  val currentSsid: Flow<String?> = _currentSsid

  val enabled: Flow<Boolean> = context.dataStore.data.map { it[EnabledProperty] != false }

  val homeSsids: Flow<List<String>> = context.dataStore.data.map {
    it[HomeSsidsProperty]
      ?.split(",")
      ?.distinct()
      ?.sorted()
      ?.filter { it.isNotEmpty() }
      ?: emptyList()
  }

  suspend fun setEnabled(enabled: Boolean) {
    context.dataStore.edit { preferences ->
      preferences[EnabledProperty] = enabled
    }

    onNetworkChange()
  }

  suspend fun setHomeSsids(ssids: List<String>) {
    context.dataStore.edit { preferences ->
      preferences[HomeSsidsProperty] = ssids.distinct().joinToString(",")
    }

    onNetworkChange()
  }

  suspend fun addHomeSsid(ssid: String) {
    setHomeSsids(homeSsids.first().plus(ssid))
  }

  suspend fun removeHomeSsid(ssid: String) {
    setHomeSsids(homeSsids.first().minus(ssid))
  }

  suspend fun onNetworkChange() {
    if (!enabled.first()) return

    val wifiManager = context.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

    val ssid = wifiManager.connectionInfo.ssid
      ?.removePrefix("\"")
      ?.removeSuffix("\"")

    _currentSsid.value = ssid

    val tailscaleBroadcast = Intent()
    tailscaleBroadcast.component = ComponentName("com.tailscale.ipn", "com.tailscale.ipn.IPNReceiver")

    if (ssid in homeSsids.first()) {
      Log.d(TAG, "Connected to ssid: $ssid. Disabling Tailscale.")
      tailscaleBroadcast.action = "com.tailscale.ipn.DISCONNECT_VPN"
    } else {
      if (ssid == null || ssid == "<unknown ssid>") {
        Log.d(TAG, "Not on wifi. Enabling Tailscale.")
      } else {
        Log.d(TAG, "Connected to ssid: $ssid. Enabling Tailscale.")
      }
      tailscaleBroadcast.action = "com.tailscale.ipn.CONNECT_VPN"
    }

    context.sendBroadcast(tailscaleBroadcast)
  }

  companion object {
    @SuppressLint("StaticFieldLeak")
    private var instance: SsidHelper? = null

    fun instance(context: Context): SsidHelper {
      return instance ?: SsidHelper(context.applicationContext).also {
        runBlocking { it.onNetworkChange() }
        instance = it
      }
    }
  }
}

val EnabledProperty = booleanPreferencesKey("enabled")
val HomeSsidsProperty = stringPreferencesKey("homeSsids")