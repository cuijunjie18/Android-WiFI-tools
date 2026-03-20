package com.example.groupconnect

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.MacAddress
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "CJJ_debug"
    }

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: Channel

    private var broadcastReceiver: WiFiDirectBroadcastReceiver? = null
    private lateinit var networkNameEditText: EditText
    private lateinit var passphraseEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var statusTextView: TextView
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            initializeWifiP2p()
        } else {
            Toast.makeText(this, "需要权限才能使用WiFi Direct功能", Toast.LENGTH_LONG).show()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        checkPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    private fun initializeViews() {
        networkNameEditText = findViewById(R.id.networkNameEditText)
        passphraseEditText = findViewById(R.id.passphraseEditText)
        connectButton = findViewById(R.id.connectButton)
        statusTextView = findViewById(R.id.statusTextView)
        
        connectButton.setOnClickListener {
            connectToGroup()
        }
        connectButton.isEnabled = true
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
            initializeWifiP2p()
        }
    }
    
    private fun initializeWifiP2p() {
        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)
        
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        broadcastReceiver = WiFiDirectBroadcastReceiver(wifiP2pManager, channel)
        registerReceiver(broadcastReceiver, intentFilter)
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    @SuppressLint("BlockedPrivateApi")
    private fun connectToGroup() {
        val networkName = networkNameEditText.text.toString().trim()
        val passphrase = passphraseEditText.text.toString().trim()
        
        if (networkName.isEmpty() || passphrase.isEmpty()) {
            Toast.makeText(this, "请输入网络名称和密码", Toast.LENGTH_SHORT).show()
            return
        }
        
        statusTextView.text = "正在连接..."
        connectButton.isEnabled = false

        val device_address = MacAddress.fromString("02:00:00:00:00:00")
        val config = WifiP2pConfig.Builder()
            .setDeviceAddress(device_address)
            .setNetworkName(networkName)
            .setPassphrase(passphrase)
            .build()

        config.wps.setup = WpsInfo.KEYPAD

        // 发起连接
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "连接请求已发送")
                runOnUiThread {
                    statusTextView.text = "连接请求已发送，等待响应..."
                    Toast.makeText(this@MainActivity, "连接请求发送成功", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(reason: Int) {
                runOnUiThread {
                    statusTextView.text = "连接失败: $reason"
                    connectButton.isEnabled = true
                    Toast.makeText(this@MainActivity, "连接失败: $reason", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }
}