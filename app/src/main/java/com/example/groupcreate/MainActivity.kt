package com.example.groupcreate

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), WiFiDirectBroadcastReceiver.ConnectionListener {
  companion object {
    const val TAG = "CJJ_debug"
  }
  private lateinit var wifiP2pManager: WifiP2pManager

  private lateinit var channel: Channel
  private var broadcastReceiver: WiFiDirectBroadcastReceiver? = null
  private lateinit var utils: Utils

  private lateinit var createButton: Button
  private lateinit var closeButton: Button
  private lateinit var statusTextView: TextView
  private var haveConnection = false

  private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    if (permissions.all { it.value }) {
      initWifiP2p()
    } else {
      Log.e(TAG, "没有权限")
      for (permission in permissions) {
        Log.e(TAG, "没有权限：${permission.key}")
      }
      Toast.makeText(this, "需要权限才能使用WiFi Direct功能", Toast.LENGTH_LONG).show()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    initController()
    checkPermissions()
  }

  private fun initWifiP2p() {
    wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    channel = wifiP2pManager.initialize(this, mainLooper, null)
    utils = Utils(this)
    initBroadcastReceiver()
    Log.d(TAG, "init WifiP2p done")
  }

  private fun initBroadcastReceiver() {
      val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
      }
      // 传入this作为ConnectionListener
      broadcastReceiver = WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this)
      registerReceiver(broadcastReceiver, intentFilter)
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun initController() {
    createButton = findViewById(R.id.createButton)
    closeButton = findViewById(R.id.closeButton)
    statusTextView = findViewById(R.id.statusTextView)
    createButton.setOnClickListener {
      createGroup()
    }
    closeButton.setOnClickListener {
      Toast.makeText(this, "正在关闭群组", Toast.LENGTH_SHORT).show()
      removeGroup()
    }
    Log.d(TAG, "init Controller done")
  }

  private fun checkPermissions() {
    val requiredPermissions = arrayOf(
      Manifest.permission.ACCESS_WIFI_STATE,
      Manifest.permission.CHANGE_WIFI_STATE,
      Manifest.permission.CHANGE_NETWORK_STATE,
      Manifest.permission.INTERNET,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION,
      Manifest.permission.NEARBY_WIFI_DEVICES
    )

    val missingPermissions = requiredPermissions.filter {
      ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    if (missingPermissions.isNotEmpty()) {
      permissionLauncher.launch(missingPermissions.toTypedArray())
    } else {
      initWifiP2p()
    }
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
  @RequiresApi(Build.VERSION_CODES.Q)
  private fun createGroup() {
    Log.d(TAG, "create Group start")
    val listener = object : WifiP2pManager.ActionListener {
      override fun onSuccess() {
        Log.d(TAG, "create Group success")
        Toast.makeText(this@MainActivity, "创建群组成功", Toast.LENGTH_SHORT).show()
        updateStatus("群组已创建，等待设备连接...")
      }

      override fun onFailure(reason: Int) {
        Log.e(TAG, "create Group fail: error code: $reason")
        Toast.makeText(this@MainActivity, "创建群组失败", Toast.LENGTH_SHORT).show()
        updateStatus("创建群组失败")
      }
    }

    val config = WifiP2pConfig.Builder()
      .setNetworkName("DIRECT-123")
      .setPassphrase("00000000")
      .build()

    // 移除旧群组
    removeGroup()
    wifiP2pManager.createGroup(channel, config, listener)
    Log.d(TAG, "create Group done")
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
  private fun removeGroup() {
    wifiP2pManager.requestGroupInfo(channel) { group ->
      if (group != null) {
        Log.d(TAG, "已有群组存在: ${group.networkName}, 正在移除...")
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
          override fun onSuccess() {
            Log.d(TAG, "移除旧群组成功")
            Toast.makeText(this@MainActivity, "移除旧群组成功", Toast.LENGTH_SHORT).show()
            updateStatus("群组已关闭")
          }
          override fun onFailure(reason: Int) { Log.e(TAG, "移除旧群组失败") }
        })
      } else {
        Log.d(TAG, "没有群组，不需要移除")
      }
    }
  }

  /**
   * 更新状态显示文本
   */
  private fun updateStatus(text: String) {
    runOnUiThread {
      statusTextView.text = text
    }
  }

  // ==================== ConnectionListener 回调 ====================

  override fun onDeviceConnected(deviceName: String, deviceAddress: String) {
    Log.d(TAG, "设备已连接回调: $deviceName ($deviceAddress)")
    val displayName = if (deviceName.isNotEmpty()) deviceName else deviceAddress
    updateStatus("设备已连接: $displayName")
    runOnUiThread {
      Toast.makeText(this, "设备已连接: $displayName", Toast.LENGTH_SHORT).show()
    }
    haveConnection = true
  }

  override fun onDeviceDisconnected() {
    Log.d(TAG, "设备断开连接回调")
    updateStatus("设备已断开连接")
    runOnUiThread {
      Toast.makeText(this, "设备已断开连接", Toast.LENGTH_SHORT).show()
    }
    haveConnection = false
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
  override fun onDeviceFound(device: WifiP2pDevice) {
    if (haveConnection) {
      return
    }
    Log.d(TAG, "设备已找到回调: ${device.deviceName} (${device.deviceAddress})")
    val config = WifiP2pConfig().apply {
      deviceAddress = device.deviceAddress
      wps.setup = WpsInfo.KEYPAD
    }

    wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
      override fun onSuccess() { Log.d(TAG, "连接请求已发送") }
      override fun onFailure(reason: Int) { Log.e(TAG, "连接请求失败：$reason") }
    })
  }

  @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(broadcastReceiver)
    removeGroup()
  }
}