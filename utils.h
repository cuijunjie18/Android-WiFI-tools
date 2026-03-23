#pragma once
//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// This code is licensed under the MIT License (MIT).
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

// SocketReaderWriter - Helper class for reading/writing messages over StreamSocket
// Converted from C++/CX to C++/WinRT

#pragma once

#include "pch.h"

#pragma comment(lib, "ws2_32.lib")

using namespace winrt;
using namespace winrt::Windows::Networking::Sockets;
using namespace winrt::Windows::Storage::Streams;

// Constants
inline const std::array<uint8_t, 3> CustomOui = { 0xAA, 0xBB, 0xCC };
inline const uint8_t CustomOuiType = 0xDD;
inline const std::array<uint8_t, 3> WfaOui = { 0x50, 0x6F, 0x9A };
inline const std::array<uint8_t, 3> MsftOui = { 0x00, 0x50, 0xF2 };
inline const winrt::hstring strServerPort = L"50001";

// Simple message logger (replaces rootPage->NotifyUser)
inline void LogMessage(const std::wstring& message, bool isError = false) {
    if (isError) {
        std::wcerr << L"[ERROR] " << message << std::endl;
    } else {
        std::wcout << L"[INFO]  " << message << std::endl;
    }
}

// Represents a discovered Wi-Fi Direct device
struct DiscoveredDevice {
    winrt::Windows::Devices::Enumeration::DeviceInformation DeviceInfo;
    std::wstring DisplayName;

    DiscoveredDevice(winrt::Windows::Devices::Enumeration::DeviceInformation const& info)
        : DeviceInfo(info), DisplayName(info.Name().c_str())
    {
    }
};

// Represents a connected Wi-Fi Direct device
struct ConnectedDevice {
    std::wstring DisplayName;
    winrt::Windows::Devices::WiFiDirect::WiFiDirectDevice WfdDevice;

    ConnectedDevice(const std::wstring& displayName,
        winrt::Windows::Devices::WiFiDirect::WiFiDirectDevice const& wfdDevice)
        : DisplayName(displayName), WfdDevice(wfdDevice) {
    }
};
