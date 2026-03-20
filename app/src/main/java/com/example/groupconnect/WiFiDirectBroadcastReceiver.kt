package com.example.groupconnect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.ReceiveChannel
import java.nio.channels.Channel

/**
 * WiFi Direct广播接收器
 */
class WiFiDirectBroadcastReceiver(val wifiP2pManager: WifiP2pManager, val channel: WifiP2pManager.Channel) : BroadcastReceiver() {

  companion object {
    const val TAG = "WiFiDirectBroadcastReceiver"
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
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
        wifiP2pManager.requestConnectionInfo(channel) { info ->
          if (info.groupFormed) {
            Log.d(TAG, "已连接到群组，自己是${if (info.isGroupOwner) "群主" else "客户端"}")
            // 可进一步获取群组名称、密码等（通过 requestGroupInfo）
          } else {
            Log.d(TAG, "未连接到任何群组")
          }
        }
        wifiP2pManager.requestGroupInfo(channel) { groupInfo ->
          if (groupInfo != null) {
            Log.d(TAG, "已连接到群组，群组名称：${groupInfo.networkName}")
            Log.d(TAG, "已连接到群组，群组密码：${groupInfo.passphrase}")
            Toast.makeText(context, "已连接到群组", Toast.LENGTH_SHORT).show()
          }
        }
      }
      WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
        Log.d(TAG, "WiFi Direct本机信息已变化")
      }
    }
  }
}