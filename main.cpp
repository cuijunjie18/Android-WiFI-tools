//*********************************************************
//
// WiFi Direct Legacy AP - Automated Demo
//
// This demo automatically starts a WiFi Direct Legacy AP
// with pre-configured SSID and passphrase, auto-accepts
// all incoming connections, and runs until the user
// presses Enter to stop.
//
//*********************************************************

#include "stdafx.h"
#include "WlanHostedNetworkWinRT.h"

using namespace ABI::Windows::Foundation;
using namespace Microsoft::WRL;
using namespace Microsoft::WRL::Wrappers;

// ============================================================
// Configuration: Set your desired SSID and passphrase here
// ============================================================
static const wchar_t* AP_SSID       = L"MyWiFiDirectAP";
static const wchar_t* AP_PASSPHRASE = L"12345678";
// ============================================================

/// Simple listener that prints events to the console
class DemoListener : public IWlanHostedNetworkListener
{
public:
    DemoListener()
        : _startedEvent(CreateEventEx(nullptr, nullptr, 0, WRITE_OWNER | EVENT_ALL_ACCESS)),
          _stoppedEvent(CreateEventEx(nullptr, nullptr, 0, WRITE_OWNER | EVENT_ALL_ACCESS))
    {
    }

    // Wait for the AP to fully start
    void WaitForStart()
    {
        WaitForSingleObjectEx(_startedEvent.Get(), INFINITE, FALSE);
    }

    // Wait for the AP to fully stop
    void WaitForStop()
    {
        WaitForSingleObjectEx(_stoppedEvent.Get(), INFINITE, FALSE);
    }

    // --- IWlanHostedNetworkListener ---

    void OnDeviceConnected(std::wstring remoteHostName) override
    {
        std::wcout << L"[+] Peer connected: " << remoteHostName << std::endl;
    }

    void OnDeviceDisconnected(std::wstring deviceId) override
    {
        std::wcout << L"[-] Peer disconnected: " << deviceId << std::endl;
    }

    void OnAdvertisementStarted() override
    {
        std::wcout << L"[*] Soft AP started successfully!" << std::endl;
        SetEvent(_startedEvent.Get());
    }

    void OnAdvertisementStopped(std::wstring message) override
    {
        std::wcout << L"[*] Soft AP stopped: " << message << std::endl;
        SetEvent(_stoppedEvent.Get());
    }

    void OnAdvertisementAborted(std::wstring message) override
    {
        std::wcout << L"[!] Soft AP aborted: " << message << std::endl;
        SetEvent(_startedEvent.Get());  // Unblock WaitForStart on failure
        SetEvent(_stoppedEvent.Get());
    }

    void OnAsyncException(std::wstring message) override
    {
        std::wcout << L"[!] Async exception: " << message << std::endl;
    }

    void LogMessage(std::wstring message) override
    {
        std::wcout << L"[i] " << message << std::endl;
    }

private:
    Event _startedEvent;
    Event _stoppedEvent;
};


int _tmain(int argc, _TCHAR* argv[])
{
    // Initialize the Windows Runtime
    RoInitializeWrapper initialize(RO_INIT_MULTITHREADED);
    if (FAILED(initialize))
    {
        std::cout << "Failed to initialize Windows Runtime" << std::endl;
        return static_cast<HRESULT>(initialize);
    }

    std::wcout << L"=========================================" << std::endl;
    std::wcout << L" WiFi Direct Legacy AP - Automated Demo" << std::endl;
    std::wcout << L"=========================================" << std::endl;
    std::wcout << L"  SSID       : " << AP_SSID << std::endl;
    std::wcout << L"  Passphrase : " << AP_PASSPHRASE << std::endl;
    std::wcout << L"  AutoAccept : ON" << std::endl;
    std::wcout << L"=========================================" << std::endl;
    std::wcout << std::endl;

    DemoListener listener;
    WlanHostedNetworkHelper hostedNetwork;

    // Configure the AP
    hostedNetwork.SetSSID(AP_SSID);
    hostedNetwork.SetPassphrase(AP_PASSPHRASE);
    hostedNetwork.SetAutoAccept(true);
    hostedNetwork.RegisterListener(&listener);

    // Start the AP
    try
    {
        std::wcout << L"Starting Soft AP..." << std::endl;
        hostedNetwork.Start();
        listener.WaitForStart();
    }
    catch (WlanHostedNetworkException& e)
    {
        std::wcout << L"Failed to start: " << e.what() << std::endl;
        return 1;
    }

    // Print connection info
    std::wcout << std::endl;
    std::wcout << L"AP is running. Connect to:" << std::endl;
    std::wcout << L"  SSID       : " << hostedNetwork.GetSSID() << std::endl;
    std::wcout << L"  Passphrase : " << hostedNetwork.GetPassphrase() << std::endl;
    std::wcout << std::endl;
    std::wcout << L"Press Enter to stop the AP and exit..." << std::endl;

    // Block until user presses Enter
    std::wstring dummy;
    std::getline(std::wcin, dummy);

    // Stop the AP
    try
    {
        std::wcout << L"Stopping Soft AP..." << std::endl;
        hostedNetwork.Stop();
        listener.WaitForStop();
    }
    catch (WlanHostedNetworkException& e)
    {
        std::wcout << L"Error stopping AP: " << e.what() << std::endl;
    }

    std::wcout << L"Done." << std::endl;
    return 0;
}
