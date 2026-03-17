# 参考代码

```kotlin
    override suspend fun connectToGroup(networkName: String, passphrase: String): ConnectToGroupResult {
        if (!hasPermission(wifiDirectPermission)) {
            Log.e(TAG, "No permission to connect to group")
            return ConnectToGroupResult(false, errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_PERMISSION_DENIED, errorMsg = "connectToGroup: No permission to connect to group")
        }

        // 1. 初始清理
        removeGroup()

        // 内部函数：挂起等待连接成功，利用超时来判断失败，而不是依赖 false 广播
        suspend fun awaitConnectionSuccess(timeout: Long): Boolean {
            return withTimeoutOrNull(timeout) {
                suspendCancellableCoroutine { cont ->
                    connectToGroupCallback = { isSuccess ->
                        // 只有成功时才恢复协程，失败或断开不处理，交给超时
                        if (isSuccess && cont.isActive) {
                            connectToGroupCallback = null
                            cont.resume(true)
                        }
                    }
                    cont.invokeOnCancellation { connectToGroupCallback = null }
                }
            } == true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ================= 第一次尝试：乐观盲连 =================
            var result = connectToGroupImpl(networkName, passphrase)

            // 如果是失败，直接返回，超时才第二次尝试
            if (!result.success && result.errorCode != NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_CONNECT_TO_GROUP_TIMEOUT) {
                return result
            }

            // 如果指令下发成功，等待 5 秒（给足状态机从 Disconnect -> Connect 的切换时间）
            // 如果 5 秒后还没回调 true，说明是真的连不上（或小米15闪退了）
            if (awaitConnectionSuccess(5000L)) {
                return ConnectToGroupResult(true)
            }

            // ================= 进入自愈补偿流程 =================
            Log.w(
                TAG,
                "Optimistic connect failed (timeout or silent fail), starting recovery: Discover -> Delay -> Retry"
            )

            // 0. 取消之前的连接，防止 BUSY
            suspendCancellableCoroutine { cont ->
                wifiP2pManager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.i(TAG, "cancelConnect success")
                        cont.resume(true)
                    }

                    override fun onFailure(r: Int) {
                        Log.e(TAG, "cancelConnect failed: $r")
                        cont.resume(false)
                    }
                })
            }

            // 1. 停止之前的任务，防止 BUSY
            suspendCancellableCoroutine { cont ->
                wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.i(TAG, "stopPeerDiscovery success")
                        cont.resume(true)
                    }

                    override fun onFailure(r: Int) {
                        Log.e(TAG, "stopPeerDiscovery failed: $r")
                        cont.resume(false)
                    }
                })
            }

            // 2. 强制触发一次发现，目的是激活底层驱动并填充扫描缓存 (Scan Cache)
            suspendCancellableCoroutine { cont ->
                wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.i(TAG, "discoverPeers success")
                        cont.resume(true)
                    }

                    override fun onFailure(r: Int) {
                        Log.e(TAG, "discoverPeers failed: $r")
                        cont.resume(false)
                    }
                })
            }

            // 3. 给底层驱动 600ms 时间去完成扫描并将 SSID 写入内核缓存
            if (awaitConnectionSuccess(600)) {
                return ConnectToGroupResult(true)
            }

            // 4. 第二次尝试：重连 (此时底层 Scan Cache 应该已有数据)
            result = connectToGroupImpl(networkName, passphrase)
            if (!result.success && result.errorCode != NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_OTHER_ERROR) {
                return result
            }

            // 5. 最后一次连接
            if (awaitConnectionSuccess(5000)) {
                return ConnectToGroupResult(true)
            }

            Log.w(TAG, "Retry connectToGroupImpl timeout")
            return ConnectToGroupResult(
                false,
                errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_CONNECT_TO_GROUP_TIMEOUT,
                errorMsg = "connectToGroup: Retry timeout"
            )
        } else {
            return connectLegacyWifi(networkName, passphrase)
        }
    }

    suspend fun connectToGroupImpl(networkName: String, passphrase: String): ConnectToGroupResult {
        Log.i(TAG, "connecting to group: $networkName ($passphrase)")
        connecting.value = true

        return try {
            val config: WifiP2pConfig = WifiP2pConfig().apply {
                deviceAddress = "02:00:00:00:00:00"
                wps.setup = WpsInfo.KEYPAD

                javaClass.getDeclaredField("passphrase").run {
                    isAccessible = true
                    set(this@apply, passphrase)
                }
                javaClass.getDeclaredField("networkName").run {
                    isAccessible = true
                    set(this@apply, networkName)
                }
            }
            withTimeoutOrNull(3 * 1000L) {
                suspendCancellableCoroutine { cont ->
                    channel?.let { ch ->
                        wifiP2pManager.connect(ch, config, object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                Log.i(TAG, "connection request sent to ${networkName}，waiting for response...")
                                cont.resume(ConnectToGroupResult(true))
                            }

                            override fun onFailure(reason: Int) {
                                Log.e(TAG, "connection fail: $reason")
                                connecting.value = false
                                var errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_CONNECT_TO_GROUP_ERROR
                                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                                    errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_DEVICE_NOT_SUPPORT
                                } else if (!isWiFiEnable()) {
                                    errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_WIFI_NOT_ENABLED
                                } else if (isWiFiAPEnable()) {
                                    errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_WIFI_AP_ENABLED
                                }
                                cont.resume(ConnectToGroupResult(false, errorCode = errorCode, errorMsg = "connectToGroup: connection fail: $reason"))
                            }
                        })
                    } ?: cont.resume(ConnectToGroupResult(false, errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_CONNECT_TO_GROUP_ERROR, errorMsg = "connectToGroup: Channel is null"))
                }
            } ?: ConnectToGroupResult(false, errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_CONNECT_TO_GROUP_TIMEOUT, errorMsg = "connectToGroup: connectToGroupImpl timeout")
        } catch (e: Exception) {
            Log.printErrStackTrace(TAG, e, "connectToGroupImpl")
            ConnectToGroupResult(
                false,
                errorCode = NearConnectionErrorCode.NEAR_CONNECTION_ERROR_CODE_CONNECT_TO_GROUP_ERROR,
                errorMsg = "connectToGroup: ${e.message}:${
                    android.util.Log.getStackTraceString(e)
                }"
            )
        }
    }
```