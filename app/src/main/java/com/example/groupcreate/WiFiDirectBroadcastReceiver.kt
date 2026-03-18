package com.example.groupcreate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * WiFi Direct广播接收器
 */
class WiFiDirectBroadcastReceiver() : BroadcastReceiver() {

  companion object {
    const val TAG = "WiFiDirectBroadcastReceiver"
  }

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
        when (state) {
          WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
            Log.d(TAG, "WiFi Direct已启用")
          }
          else -> {
            Log.d(TAG, "WiFi Direct已禁用")
          }
        }
      }
      WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
        Log.d(TAG, "WiFi Direct对端列表已更新")
      }
      WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
        Log.d(TAG, "WiFi Direct连接状态已变化")
      }
      WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
        Log.d(TAG, "WiFi Direct本机信息已变化")
      }
    }
  }
}