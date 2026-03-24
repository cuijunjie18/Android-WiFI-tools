package com.example.groupcreate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

/**
 * WiFi Direct广播接收器
 * 处理P2P状态变化、对端发现、连接变化等事件
 */
class WiFiDirectBroadcastReceiver(
  val wifiP2pManager: WifiP2pManager,
  val channel: WifiP2pManager.Channel,
  private val connectionListener: ConnectionListener? = null
) : BroadcastReceiver() {

  companion object {
    const val TAG = "WiFiDirectBroadcastReceiver"
  }

  /**
   * 连接状态回调接口
   */
  interface ConnectionListener {
    fun onDeviceConnected(deviceName: String, deviceAddress: String)
    fun onDeviceDisconnected()
    fun onDeviceFound(device: WifiP2pDevice)
  }

  @RequiresApi(Build.VERSION_CODES.Q)
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
        wifiP2pManager.requestPeers(channel) { peerList ->
          peerList.deviceList.forEach { device ->
            Log.d(TAG, "发现设备：${device.deviceName} (${device.deviceAddress}) 状态: ${getDeviceStatus(device.status)}")
            connectionListener?.onDeviceFound(device)
          }
        }
      }
      WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
        Log.d(TAG, "WiFi Direct连接状态已变化")

        // 获取NetworkInfo判断连接状态
        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
        Log.d(TAG, "NetworkInfo: isConnected=${networkInfo?.isConnected}, state=${networkInfo?.state}")

        if (networkInfo != null && networkInfo.isConnected) {
          // 设备已连接，请求连接信息
          wifiP2pManager.requestConnectionInfo(channel) { info ->
            Log.d(TAG, "连接信息 - groupFormed: ${info.groupFormed}, isGroupOwner: ${info.isGroupOwner}")
            Log.d(TAG, "连接信息 - groupOwnerAddress: ${info.groupOwnerAddress?.hostAddress}")
          }

          // 请求群组信息，获取已连接的客户端列表
          wifiP2pManager.requestGroupInfo(channel) { group ->
            if (group != null) {
              Log.d(TAG, "群组名称: ${group.networkName}")
              Log.d(TAG, "密码: ${group.passphrase}")
              Log.d(TAG, "接口名称: ${group.`interface`}")
              Log.d(TAG, "GO设备地址: ${group.owner.deviceAddress}")
              Log.d(TAG, "已连接客户端数量: ${group.clientList.size}")

              // 遍历已连接的客户端
              group.clientList.forEach { client ->
                Log.d(TAG, "已连接客户端: ${client.deviceName} (${client.deviceAddress})")
                connectionListener?.onDeviceConnected(client.deviceName, client.deviceAddress)
              }
            }
          }
        } else {
          Log.d(TAG, "设备已断开连接")
          connectionListener?.onDeviceDisconnected()

          // 断开后仍然查看群组状态
          wifiP2pManager.requestGroupInfo(channel) { group ->
            if (group != null) {
              Log.d(TAG, "群组仍然存在: ${group.networkName}, 剩余客户端: ${group.clientList.size}")
            } else {
              Log.d(TAG, "群组已不存在")
            }
          }
        }
      }
      WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
        Log.d(TAG, "WiFi Direct本机信息已变化")
        wifiP2pManager.requestGroupInfo(channel) { group ->
          if (group != null) {
            Log.d(TAG, "已有群组存在: ${group.networkName}")
            Log.d(TAG, "密码: ${group.passphrase}")
            Log.d(TAG, "本机当前接口名称: ${group.`interface`}")
            Log.d(TAG, "本机当前接口Mac地址: ${group.owner.deviceAddress}")
          }
        }
      }
    }
  }

  /**
   * 将设备状态码转换为可读字符串
   */
  private fun getDeviceStatus(status: Int): String {
    return when (status) {
      WifiP2pDevice.CONNECTED -> "已连接"
      WifiP2pDevice.INVITED -> "已邀请"
      WifiP2pDevice.FAILED -> "失败"
      WifiP2pDevice.AVAILABLE -> "可用"
      WifiP2pDevice.UNAVAILABLE -> "不可用"
      else -> "未知($status)"
    }
  }
}