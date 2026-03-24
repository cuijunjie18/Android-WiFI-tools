# Wi-Fi Direct 广播端 Demo（Windows）

一个 Windows 控制台应用程序，作为 **Wi-Fi Direct Group Owner (GO)** 运行，支持 Android Wi-Fi Direct (P2P) 设备发现、连接并通过 TCP 通信。

## 概述

本项目演示了一个运行在 Windows 上的 Wi-Fi Direct 广播端，基于 **C++/WinRT** 和 Windows WiFiDirect API 构建。应用程序功能：

1. **广播发现**：以自主组拥有者（Autonomous Group Owner）模式广播，使附近的 Wi-Fi Direct 设备能够发现本机。
2. **自动接受连接**：自动接受来自 Android（或其他 Wi-Fi Direct）客户端的连接请求。
3. **建立 TCP 通信**：与已连接设备建立 TCP 通信 — 在端口 `50001` 上运行一个简单的 TCP Echo 服务器。

## 架构

```
┌─────────────────────────────────────────────────┐
│              Windows（本应用）                    │
│                                                  │
│  ┌──────────────────────┐  ┌──────────────────┐  │
│  │  WiFiDirect 广播端    │  │  TCP Echo 服务器  │  │
│  │  (Advertiser / GO)   │  │  (端口 50001)     │  │
│  └──────────┬───────────┘  └────────▲─────────┘  │
│             │                       │             │
│             │  收到连接请求           │  TCP 连接   │
│             │  ──────────────────►  │             │
│             │  (自动接受 +            │             │
│             │   新线程处理)           │             │
└─────────────┼───────────────────────┼─────────────┘
              │  Wi-Fi Direct P2P     │  TCP/IP
              ▼                       │
┌─────────────────────────────────────┐
│         Android 设备                 │
│   (Wi-Fi Direct 客户端 / Peer)       │
└─────────────────────────────────────┘
```

## 项目结构

```
Android_P2P_tools/
├── main.cpp                  # 入口点 & Wi-Fi Direct 广播端逻辑
├── socket.h                  # TCP Echo 服务器实现（Winsock2）
├── utils.h                   # 辅助类型、常量和日志工具
├── pch.h                     # 预编译头文件（WinRT 和标准库头文件）
├── wifiDirectDemo.slnx       # Visual Studio 解决方案文件
├── wifiDirectDemo.vcxproj    # Visual Studio 项目文件（MSVC v145, C++20）
└── vibe_docs/                # 开发文档和笔记
```

## 核心组件

### 1. Wi-Fi Direct 广播端（`main.cpp`）

- 创建 `WiFiDirectAdvertisementPublisher`，启用 **自主组拥有者** 模式。
- 设置 `ListenStateDiscoverability` 为 `Normal`，使设备可被发现。
- 在 `WiFiDirectConnectionListener` 上注册 `ConnectionRequested` 回调。
- 收到连接请求时：
  - 从回调中提取设备信息（名称、ID）。
  - 创建一个 **独立线程** 来处理阻塞的连接协商（避免阻塞事件派发线程导致死锁）。
  - 调用 `WiFiDirectDevice::FromIdAsync()`，设置 **60 秒超时** 和 `GroupOwnerIntent = 15`（最大值，确保本设备成为 GO）。
  - 实现 **重试机制**（最多 3 次尝试，每次间隔 2 秒），提高连接成功率。
- 连接成功后，自动启动 TCP Echo 服务器。
- 主线程提供简单的交互循环（输入 `stop` / `quit` / `exit` 退出）。

### 2. TCP Echo 服务器（`socket.h`）

- 使用 **Winsock2** 将 TCP 套接字绑定到 `0.0.0.0:50001`。
- 等待单个客户端连接。
- 接收已连接客户端的数据并 **原样回显**（经典 Echo 服务器模式）。
- 连接时打印客户端 IP 和端口。

### 3. 工具类（`utils.h`）

- **`LogMessage()`**：集中式日志函数，支持错误/信息级别，输出到 `stdout`/`stderr`。
- **`ConnectedDevice`**：保存已连接 Wi-Fi Direct 设备的显示名称和 WinRT 设备对象的结构体。
- **`DiscoveredDevice`**：已发现设备信息的结构体。
- **常量**：自定义 OUI 字节、WFA/MSFT OUI，以及服务器端口字符串（`"50001"`）。

### 4. 预编译头文件（`pch.h`）

包含所有必要的 WinRT 头文件和标准 C++ 库：
- `winrt/Windows.Devices.WiFiDirect.h`
- `winrt/Windows.Devices.Enumeration.h`
- `winrt/Windows.Networking.Sockets.h`
- 标准线程、I/O 和容器头文件

## 构建要求

| 要求 | 版本 |
|---|---|
| **操作系统** | Windows 10/11 |
| **IDE** | Visual Studio 2022+ |
| **工具集** | MSVC v145 |
| **C++ 标准** | C++20 |
| **Windows SDK** | 10.0.26100.0 |
| **目标平台** | x64 / x86 |

### 构建步骤

1. 使用 Visual Studio 2022 打开 `wifiDirectDemo.slnx`。
2. 选择所需的配置（Debug/Release）和平台（推荐 x64）。
3. 生成解决方案（`Ctrl+Shift+B`）。
4. 运行生成的可执行文件。

## 使用方法

1. **启动应用程序** — 将开始以 Wi-Fi Direct Group Owner 身份进行广播。
2. **在 Android 设备上**，扫描 Wi-Fi Direct 对等设备并连接到广播的设备。
3. Windows 应用将 **自动接受** 连接并启动 TCP Echo 服务器。
4. Android 应用随后可以通过 TCP 连接到 GO 的 IP 地址的 `50001` 端口进行数据交换。
5. 在控制台输入 `stop`、`quit` 或 `exit` 关闭广播端。

### 控制台输出示例

```
=== Wi-Fi Direct Advertiser Mode ===
[INFO]  Advertisement Status: 1 Error: 0
[INFO]  Advertisement started, waiting for connections...
[INFO]  Connection request received from Android_Device
[INFO]  Device ID: ...
[INFO]  Auto-accepting connection...
[INFO]  Connection attempt 1/3 for Android_Device
[INFO]  Successfully connected to Android_Device at 192.168.49.x
[INFO]  Device added to connected list. Total: 1
Begin tcp Test...
Waiting for client to connect...
Client connected from 192.168.49.x:xxxxx
```

## 设计说明

- **线程安全**：连接处理被分离到独立线程中执行，避免阻塞 WinRT 事件派发线程（此前这会导致连接超时和死锁）。
- **重试机制**：Wi-Fi Direct 连接本身不太稳定；3 次重试、2 秒间隔的策略显著提高了连接成功率。
- **超时控制**：使用 `wait_for(60s)` 替代 `.get()`，防止 `FromIdAsync` 无限期阻塞。
- **GO Intent = 15**：确保本 Windows 设备在 P2P 协商中始终成为 Group Owner。

## 已知限制

- TCP 服务器目前一次只能处理 **一个客户端**（单次 `accept` 调用）。
- Echo 服务器仅作基础演示；可根据需要替换为实际应用逻辑。
- 未实现持久化配对或断线重连逻辑。
- 应用程序需要 **管理员权限**，且 Wi-Fi 适配器必须支持 Wi-Fi Direct。

## 参考

[1] wifi direct demo：https://github.com/nimeia/wifi-direct-demo  