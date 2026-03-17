package com.example.groupconnect

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: Channel
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
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    when (state) {
                        WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                            statusTextView.text = "WiFi Direct已启用"
                            connectButton.isEnabled = true
                        }
                        else -> {
                            statusTextView.text = "WiFi Direct未启用"
                            connectButton.isEnabled = false
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    // 对等设备列表发生变化
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    wifiP2pManager.requestPeers(channel) { peers ->
                        val peerList = peers?.deviceList ?: emptyList()
                        statusTextView.text = "发现 ${peerList.size} 个设备"
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    // 连接状态发生变化
                    statusTextView.text = "连接状态已改变"
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    // 本设备信息发生变化
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        checkPermissions()
    }
    
    private fun initializeViews() {
        networkNameEditText = findViewById(R.id.networkNameEditText)
        passphraseEditText = findViewById(R.id.passphraseEditText)
        connectButton = findViewById(R.id.connectButton)
        statusTextView = findViewById(R.id.statusTextView)
        
        connectButton.setOnClickListener {
            connectToGroup()
        }
    }
    
    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
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
        
        registerReceiver(receiver, intentFilter)
    }
    
    private fun connectToGroup() {
        val networkName = networkNameEditText.text.toString().trim()
        val passphrase = passphraseEditText.text.toString().trim()
        
        if (networkName.isEmpty() || passphrase.isEmpty()) {
            Toast.makeText(this, "请输入网络名称和密码", Toast.LENGTH_SHORT).show()
            return
        }
        
        statusTextView.text = "正在连接..."
        connectButton.isEnabled = false
        
        // 创建WiFi Direct配置
        val config = WifiP2pConfig().apply {
            deviceAddress = "02:00:00:00:00:00" // 使用默认地址
            wps.setup = WifiP2pConfig.WPS_PBC // 使用PBC方式
            
            // 使用反射设置网络名称和密码（因为API限制）
            try {
                val networkNameField = javaClass.getDeclaredField("networkName")
                networkNameField.isAccessible = true
                networkNameField.set(this, networkName)
                
                val passphraseField = javaClass.getDeclaredField("passphrase")
                passphraseField.isAccessible = true
                passphraseField.set(this, passphrase)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "配置创建失败: ${e.message}", Toast.LENGTH_LONG).show()
                statusTextView.text = "配置失败"
                connectButton.isEnabled = true
                return
            }
        }
        
        // 发起连接
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
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
    
    override fun onResume() {
        super.onResume()
        if (::wifiP2pManager.isInitialized) {
            wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // 发现对等设备成功
                }
                
                override fun onFailure(reason: Int) {
                    // 发现对等设备失败
                }
            })
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::wifiP2pManager.isInitialized) {
            wifiP2pManager.stopPeerDiscovery(channel, null)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // 忽略未注册的异常
        }
    }
}