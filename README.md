# P2PConnect

基于 **Wi-Fi Direct (P2P)** 技术的 Android 客户端应用，支持扫描附近的 Wi-Fi Direct 设备并建立点对点连接，连接成功后可进行 TCP 通信测试。

## ✨ 功能特性

- **设备扫描**：自动发现附近支持 Wi-Fi Direct 的设备，并展示在列表中
- **设备连接**：选择目标设备，发起 Wi-Fi Direct P2P 连接，支持连接失败自动重试（最多 3 次）
- **TCP 通信测试**：连接成功后，可通过 TCP Socket 向 Group Owner 发送消息并接收响应
- **动态权限管理**：适配 Android 10~13+ 不同版本的权限要求，自动请求必要权限

## 🏗️ 技术架构

| 技术/框架 | 说明 |
|---|---|
| **语言** | Kotlin |
| **最低 SDK** | API 24 (Android 7.0) |
| **目标 SDK** | API 36 |
| **构建工具** | Gradle (Kotlin DSL) |
| **UI 绑定** | ViewBinding |
| **异步处理** | Kotlin Coroutines (lifecycleScope) |
| **核心 API** | WifiP2pManager、BroadcastReceiver |

## 📋 权限说明

应用根据不同 Android 版本动态请求以下权限：

| 权限 | 用途 | 版本要求 |
|---|---|---|
| `INTERNET` | 网络通信 (TCP 测试) | 全版本 |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | Wi-Fi 状态管理 | 全版本 |
| `ACCESS_FINE_LOCATION` | 设备发现定位 | Android 12 及以下 |
| `NEARBY_WIFI_DEVICES` | 附近设备发现 | Android 13+ |
| `ACCESS_NETWORK_STATE` / `CHANGE_NETWORK_STATE` | 网络状态管理 | 全版本 |

## 📁 项目结构

```
P2PConnect/
├── app/src/main/
│   ├── AndroidManifest.xml              # 应用清单 & 权限声明
│   ├── java/com/example/p2pconnect/
│   │   ├── MainActivity.kt             # 主界面：设备列表、连接操作、TCP 测试
│   │   └── WifiDirectManager.kt        # Wi-Fi Direct 核心管理类：扫描、连接、广播监听
│   └── res/layout/
│       └── activity_main.xml            # 主界面布局
├── app/build.gradle.kts                 # 模块级构建配置
├── build.gradle.kts                     # 项目级构建配置
└── gradle/libs.versions.toml            # 依赖版本管理
```

### 核心模块说明

- **`MainActivity.kt`** — 应用主入口，负责 UI 交互逻辑：
  - 初始化设备列表 (ListView + ArrayAdapter)
  - 处理扫描、连接、TCP 测试按钮事件
  - 实现 `WifiDirectManager.ConnectionListener` 回调接口
  - 使用 Kotlin 协程在 IO 线程执行 TCP Socket 通信

- **`WifiDirectManager.kt`** — Wi-Fi Direct 核心管理器：
  - 封装 `WifiP2pManager` 的初始化与通道管理
  - 注册 `BroadcastReceiver` 监听设备发现和连接状态变化
  - 提供 `discoverPeers()` 和 `connectToDevice()` 方法
  - 通过 `ConnectionListener` 接口向上层回调事件

## 🚀 使用方法

1. **克隆项目**
   ```bash
   git clone <repository-url>
   ```

2. **使用 Android Studio 打开项目**，等待 Gradle 同步完成

3. **连接 Android 真机**（Wi-Fi Direct 不支持模拟器）

4. **运行应用**，授予权限后：
   - 点击 **「扫描设备」** 发现附近的 Wi-Fi Direct 设备
   - 在列表中选择目标设备
   - 点击 **「连接选中设备」** 发起 P2P 连接
   - 连接成功后，点击 **「进行 TCP 测试」**（需要对端设备在 50001 端口监听）

## ⚠️ 注意事项

- **必须使用真机调试**，Android 模拟器不支持 Wi-Fi Direct 功能
- TCP 测试功能要求对端设备在 **50001 端口** 上运行 TCP 服务端
- 连接请求发送后，对端设备可能需要确认授权
- Android 13 及以上版本需授予「附近设备」权限；Android 12 及以下需授予「精确位置」权限
