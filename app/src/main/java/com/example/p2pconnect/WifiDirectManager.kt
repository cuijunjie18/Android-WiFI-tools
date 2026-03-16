package com.example.p2pconnect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.annotation.RequiresPermission

class WifiDirectManager(
  private val context: Context,
  private val listener: ConnectionListener
) : WifiP2pManager.ChannelListener {

  companion object {
    private const val TAG = "WifiDirectManager"
  }

  private val manager: WifiP2pManager =
    context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
  private val channel: WifiP2pManager.Channel =
    manager.initialize(context, context.mainLooper, this)

  private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
          manager.requestPeers(channel) { peerList ->
            peerList.deviceList.forEach { device ->
              Log.d(TAG, "发现设备：${device.deviceName} (${device.deviceAddress})")
              listener.onDeviceFound(device)
            }
          }
        }
        WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
          val info = intent.getParcelableExtra<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
          if (info != null && info.groupFormed) {
            if (info.isGroupOwner) {
              Log.d(TAG, "连接成功：我是 GO")
            } else {
              Log.d(TAG, "连接成功：我是 Client, GO IP = ${info.groupOwnerAddress?.hostAddress}")
              listener.onConnectionSuccess(info)
            }
          } else {
            listener.onConnectionFailed()
          }
        }
      }
    }
  }

  interface ConnectionListener {
    fun onDeviceFound(device: WifiP2pDevice)
    fun onConnectionSuccess(info: WifiP2pInfo)
    fun onConnectionFailed()
  }

  init {
    val intentFilter = IntentFilter().apply {
      addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
      addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
      addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
      addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    context.registerReceiver(receiver, intentFilter)
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
  fun discoverPeers() {
    manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
      override fun onSuccess() { Log.d(TAG, "扫描启动成功") }
      override fun onFailure(reason: Int) { Log.e(TAG, "扫描失败：$reason") }
    })
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
  fun connectToDevice(device: WifiP2pDevice) {
    val config = WifiP2pConfig().apply {
      deviceAddress = device.deviceAddress
      wps.setup = WpsInfo.PBC // 关键：使用 PBC 方式，对应 Linux 的 wpa_cli pbc
      // 如果使用 PIN 码：
      // wps.setup = WpsInfo.KEYPAD
      // wps.pin = "12345678"
    }

    manager.connect(channel, config, object : WifiP2pManager.ActionListener {
      override fun onSuccess() { Log.d(TAG, "连接请求已发送") }
      override fun onFailure(reason: Int) { Log.e(TAG, "连接请求失败：$reason") }
    })
  }

  fun onDestroy() {
    try {
      context.unregisterReceiver(receiver)
    } catch (e: Exception) {
      Log.e(TAG, "Receiver 注销失败", e)
    }
  }

  override fun onChannelDisconnected() {
    Log.e(TAG, "WiFi Direct Channel 断开")
  }
}