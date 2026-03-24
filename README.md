
# GroupCreate

一个基于 **Wi-Fi Direct (Wi-Fi P2P)** 的 Android 客户端应用，核心功能是创建 P2P Group 并支持设备连接。

## 功能概述

- **创建 Wi-Fi Direct 群组**：一键创建固定名称和密码的 P2P Group，作为 Group Owner (GO) 等待其他设备加入
- **关闭群组**：支持手动关闭当前已创建的群组
- **自动发现与连接**：通过广播接收器自动发现附近的 Wi-Fi Direct 设备，并发起连接请求
- **连接状态监听**：实时监听设备的连接/断开事件，并在 UI 上展示当前状态

## 技术栈

| 项目 | 说明 |
|------|------|
| 语言 | Kotlin |
| 最低 SDK | API 24 (Android 7.0) |
| 目标 SDK | API 36 |
| 核心 API | Android Wi-Fi P2P (`WifiP2pManager`) |
| 架构 | 单 Activity + BroadcastReceiver |

## 项目结构

```
app/src/main/java/com/example/groupcreate/
├── MainActivity.kt                  # 主界面，负责群组创建/关闭、权限管理、连接回调处理
├── WiFiDirectBroadcastReceiver.kt   # Wi-Fi Direct 广播接收器，处理 P2P 状态变化和连接事件
└── Utils.kt                         # 工具类，提供随机字符串生成、设备名获取、字符串截断等功能
```

### 核心文件说明

#### `MainActivity.kt`
- 实现 `ConnectionListener` 接口，处理设备连接/断开/发现的回调
- 通过 `WifiP2pConfig.Builder` 创建固定网络名 `DIRECT-123`、密码 `00000000` 的群组
- 使用 `ActivityResultContracts` 进行运行时权限申请
- 在 `onDestroy` 中自动注销广播接收器并移除群组

#### `WiFiDirectBroadcastReceiver.kt`
- 监听四类 Wi-Fi P2P 广播事件：
  - `WIFI_P2P_STATE_CHANGED_ACTION` — Wi-Fi Direct 启用/禁用状态
  - `WIFI_P2P_PEERS_CHANGED_ACTION` — 发现的对端设备列表变化
  - `WIFI_P2P_CONNECTION_CHANGED_ACTION` — 连接状态变化（连接/断开）
  - `WIFI_P2P_THIS_DEVICE_CHANGED_ACTION` — 本机设备信息变化
- 定义 `ConnectionListener` 回调接口，将事件传递给 Activity

#### `Utils.kt`
- `generateRandomString()` — 生成指定长度的随机字符串
- `getCurrentDeviceName()` — 获取当前设备名称
- `truncateByBytes()` — 按 UTF-8 字节数安全截断字符串

## 所需权限

```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
```

## 使用方式

1. 使用 Android Studio 打开项目
2. 连接 Android 设备（需要真实设备，模拟器不支持 Wi-Fi Direct）
3. 编译并运行应用
4. 点击 **「创建 group」** 按钮创建 P2P 群组
5. 其他设备可通过 Wi-Fi Direct 搜索并连接到该群组（网络名：`DIRECT-123`，密码：`00000000`）
6. 连接成功后，界面会显示已连接设备的信息
7. 点击 **「关闭 group」** 按钮可移除当前群组

## 工作流程

```
┌─────────────────────────────────────────────────┐
│                  应用启动                        │
│                    │                             │
│          检查并申请运行时权限                      │
│                    │                             │
│         初始化 WifiP2pManager & Channel          │
│                    │                             │
│         注册 WiFiDirectBroadcastReceiver         │
│                    │                             │
│    ┌───────────────┴───────────────┐             │
│    ▼                               ▼             │
│  点击「创建 group」            点击「关闭 group」  │
│    │                               │             │
│  移除旧群组(如有)             移除当前群组         │
│    │                                             │
│  创建新的 P2P Group                              │
│    │                                             │
│  等待设备连接...                                  │
│    │                                             │
│  BroadcastReceiver 收到                          │
│  PEERS_CHANGED 广播                              │
│    │                                             │
│  自动发起连接请求                                 │
│    │                                             │
│  连接成功，UI 更新状态                            │
└─────────────────────────────────────────────────┘
```

## 注意事项

- **必须使用真实设备**进行测试，Android 模拟器不支持 Wi-Fi Direct
- 创建群组的设备将作为 **Group Owner (GO)**
- 当前群组配置为固定网络名和密码，适合开发调试使用
- 应用退出时会自动清理群组资源
