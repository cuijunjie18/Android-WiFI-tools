package com.example.groupcreate

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
  companion object {
    const val TAG = "CJJ_debug"
  }
  private lateinit var wifiP2pManager: WifiP2pManager

  private lateinit var channel: Channel
  private var broadcastReceiver: WiFiDirectBroadcastReceiver? = null

  private lateinit var button: Button

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
    initBroadcastReceiver()
    checkPermissions()
  }

  private fun initWifiP2p() {
    wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    channel = wifiP2pManager.initialize(this, mainLooper, null)
    initBroadcastReceiver()
    Log.d(TAG, "init WifiP2p done")
  }

  private fun initBroadcastReceiver() {
      val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
//        addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
//        addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
      }
      broadcastReceiver = WiFiDirectBroadcastReceiver()
      registerReceiver(broadcastReceiver, intentFilter)
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  private fun initController() {
    button = findViewById(R.id.button)
    button.setOnClickListener {
      createGroup()
    }
    Log.d(TAG, "init Controller done")
  }

  private fun checkPermissions() {
    val requiredPermissions = arrayOf(
      Manifest.permission.ACCESS_WIFI_STATE,
      Manifest.permission.CHANGE_WIFI_STATE,
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
      }

      override fun onFailure(reason: Int) {
        Log.e(TAG, "create Group fail")
        Toast.makeText(this@MainActivity, "创建群组失败", Toast.LENGTH_SHORT).show()
      }
    }

    val config = WifiP2pConfig.Builder()
      .setNetworkName("DIRECT-CJJ")
      .setPassphrase("1234567890")
      .build()

    wifiP2pManager.createGroup(channel, config, listener)
    Log.d(TAG, "create Group done")
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(broadcastReceiver)
  }

//  override fun onRequestPermissionsResult(requestCode: Int,
//                                          permissions: Array<String>, grantResults: IntArray) {
//    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    when (requestCode) {
//      1 -> {
//        if (grantResults.isNotEmpty() &&
//          grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//        } else {
//          Toast.makeText(this, "You denied the permission",
//            Toast.LENGTH_SHORT).show()
//        }
//      }
//    }
//  }
}