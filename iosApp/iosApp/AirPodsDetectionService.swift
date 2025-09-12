import Foundation
import AVFoundation
import CoreMotion
import UIKit

@objc class AirPodsDetectionService: NSObject {
    private var isMonitoring = false
    private var isMotionTracking = false
    private var currentDeviceName: String?
    private var isConnected = false
    private var currentPitchAngle: Double = 0.0
    
    // Only create motion manager on iOS 15.0+
    @available(iOS 15.0, *)
    private var headphoneMotionManager: CMHeadphoneMotionManager?
    
    static let shared = AirPodsDetectionService()
    
    override init() {
        super.init()
        setupDetection()
        startMonitoring()
        
        // Listen for status check requests from Kotlin
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleStatusCheck),
            name: NSNotification.Name("CheckAirPodsStatus"),
            object: nil
        )
        
        // Listen for stop monitoring requests from Kotlin
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleStopMonitoring),
            name: NSNotification.Name("StopAirPodsMonitoring"),
            object: nil
        )
        
        // Listen for start motion tracking requests from Kotlin
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleStartMotionTracking),
            name: NSNotification.Name("StartAirPodsMotionTracking"),
            object: nil
        )
    }
    
    private func setupDetection() {
        // Always start with audio session detection as fallback
        checkConnectionStatusViaAudioSession()
        
        // Try to set up motion detection on iOS 15.0+
        if #available(iOS 15.0, *) {
            setupHeadphoneMotionManager()
        }
    }
    
    @available(iOS 15.0, *)
    private func setupHeadphoneMotionManager() {
        headphoneMotionManager = CMHeadphoneMotionManager()
        
        // Don't start motion updates automatically - only when explicitly requested
        print("AirPodsDetectionService: Motion manager set up, but not started yet")
    }
    
    @available(iOS 15.0, *)
    private func startHeadphoneMotionUpdates() {
        guard let manager = headphoneMotionManager else { return }
        
        // Request motion permission before starting updates
        if manager.isDeviceMotionAvailable {
            print("AirPodsDetectionService: Motion is available, starting updates...")
            
            // Try to start updates - this will fail if motion is not available
            manager.startDeviceMotionUpdates(to: .main) { [weak self] motion, error in
                guard let self = self else { return }
                
                // Check if motion tracking is still active
                if !self.isMotionTracking {
                    print("AirPodsDetectionService: Motion callback received but tracking stopped, ignoring")
                    return
                }
                
                if let error = error {
                    print("AirPodsDetectionService: Error getting motion updates: \(error)")
                    // Motion failed, stop tracking and fall back to audio session
                    self.isMotionTracking = false
                    self.checkConnectionStatusViaAudioSession()
                    return
                }
                
                guard let motion = motion else { 
                    print("AirPodsDetectionService: No motion data received")
                    return 
                }
                
                // Extract pitch angle (head nodding up/down)
                let pitch = motion.attitude.pitch * 180.0 / .pi // Convert to degrees
                
                self.currentPitchAngle = pitch
                self.isConnected = true
                self.currentDeviceName = "AirPods Pro (Motion)"
                
                // Post notification with motion data
                self.postMotionDataNotification(pitch: pitch)
                
                print("AirPodsDetectionService: Pitch angle: \(String(format: "%.1f", pitch))°")
            }
        } else {
            print("AirPodsDetectionService: Motion not available, falling back to audio session")
            checkConnectionStatusViaAudioSession()
        }
    }
    
    @objc func startMotionTracking() {
        print("AirPodsDetectionService: startMotionTracking called - isMotionTracking: \(isMotionTracking)")
        if #available(iOS 15.0, *) {
            // Always recreate motion manager to ensure clean state
            setupHeadphoneMotionManager()
            
            // Check if motion permission is available
            guard let manager = headphoneMotionManager else { 
                print("AirPodsDetectionService: Motion manager not available")
                return 
            }
            
            print("AirPodsDetectionService: Motion manager created, checking availability...")
            print("AirPodsDetectionService: isDeviceMotionAvailable: \(manager.isDeviceMotionAvailable)")
            
            if manager.isDeviceMotionAvailable {
                print("AirPodsDetectionService: Motion permission granted, starting tracking")
                isMotionTracking = true
                print("AirPodsDetectionService: Set isMotionTracking = true")
                startHeadphoneMotionUpdates()
                
                // Add timeout to detect if motion updates don't start
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) { [weak self] in
                    guard let self = self else { return }
                    if self.isMotionTracking && self.currentPitchAngle == 0.0 {
                        print("AirPodsDetectionService: Motion updates timeout - no data received in 2 seconds, falling back to audio session")
                        self.isMotionTracking = false
                        self.checkConnectionStatusViaAudioSession()
                    }
                }
            } else {
                print("AirPodsDetectionService: Motion not available - requesting permission")
                // Show permission request dialog
                showMotionPermissionAlert()
            }
        } else {
            print("AirPodsDetectionService: iOS < 15.0, falling back to audio session")
            checkConnectionStatusViaAudioSession()
        }
    }
    
    @available(iOS 15.0, *)
    private func showMotionPermissionAlert() {
        DispatchQueue.main.async {
            guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                  let rootViewController = windowScene.windows.first?.rootViewController else {
                return
            }
            
            let alert = UIAlertController(
                title: "Motion Permission Required",
                message: "Posturely needs access to your AirPods motion sensors to track your head posture and provide real-time feedback for better posture habits.",
                preferredStyle: .alert
            )
            
            alert.addAction(UIAlertAction(title: "Allow", style: .default) { [weak self] _ in
                // Try to start motion updates after user consent
                self?.isMotionTracking = true
                self?.startHeadphoneMotionUpdates()
            })
            
            alert.addAction(UIAlertAction(title: "Not Now", style: .cancel) { [weak self] _ in
                print("AirPodsDetectionService: User denied motion permission")
                // Fall back to audio session detection only
                self?.checkConnectionStatusViaAudioSession()
            })
            
            rootViewController.present(alert, animated: true)
        }
    }
    
    private func postMotionDataNotification(pitch: Double) {
        let userInfo: [String: Any] = [
            "isConnected": true,
            "deviceName": "AirPods Pro (Motion)",
            "pitchAngle": pitch,
            "hasMotionData": true
        ]
        
        NotificationCenter.default.post(
            name: NSNotification.Name("AirPodsConnectionStatusChanged"),
            object: self,
            userInfo: userInfo
        )
    }
    
    // Fallback method using audio session (for older AirPods or when motion not available)
    private func checkConnectionStatusViaAudioSession() {
        print("AirPodsDetectionService: Starting audio session check...")
        let audioSession = AVAudioSession.sharedInstance()
        
        do {
            try audioSession.setActive(true)
            let currentRoute = audioSession.currentRoute
            
            print("AirPodsDetectionService: Current route has \(currentRoute.outputs.count) outputs")
            
            var foundDevice = false
            var deviceName = ""
            
            for output in currentRoute.outputs {
                let portType = output.portType
                let portName = output.portName
                
                print("AirPodsDetectionService: Checking port: '\(portName)' of type: \(portType)")
                
                // Check for Bluetooth devices (AirPods, Beats)
                if portType == .bluetoothA2DP || 
                   portType == .bluetoothLE || 
                   portType == .bluetoothHFP {
                    
                    print("AirPodsDetectionService: Found Bluetooth port: \(portName)")
                    
                    // Check if the device name contains AirPods or Beats identifiers
                    if portName.lowercased().contains("airpods") ||
                       portName.lowercased().contains("beats") ||
                       portName.lowercased().contains("powerbeats") ||
                       portName.lowercased().contains("studio") ||
                       portName.lowercased().contains("solo") {
                        foundDevice = true
                        deviceName = portName
                        print("AirPodsDetectionService: ✅ Found compatible device: \(portName)")
                        break
                    } else {
                        print("AirPodsDetectionService: ❌ Bluetooth device '\(portName)' not compatible")
                    }
                }
                
                // Also check for wired AirPods (if connected via Lightning)
                if portType == .headphones || portType == .headsetMic {
                    print("AirPodsDetectionService: Found wired port: \(portName)")
                    if portName.lowercased().contains("airpods") {
                        foundDevice = true
                        deviceName = portName
                        print("AirPodsDetectionService: ✅ Found wired AirPods: \(portName)")
                        break
                    }
                }
            }
            
            // Update connection status
            let wasConnected = isConnected
            isConnected = foundDevice
            currentDeviceName = foundDevice ? deviceName : nil
            
            print("AirPodsDetectionService: Connection status - Was: \(wasConnected), Now: \(isConnected), Device: \(currentDeviceName ?? "None")")
            
            // Post notification (include current motion state if any)
            postConnectionStatusNotification()
            
            if !foundDevice {
                print("AirPodsDetectionService: ❌ No compatible devices found")
            }
            
        } catch {
            print("AirPodsDetectionService: ❌ Error accessing audio session: \(error)")
            isConnected = false
            currentDeviceName = nil
            postConnectionStatusNotification()
        }
    }
    
    @objc private func handleStatusCheck() {
        print("AirPodsDetectionService: Received status check request from Kotlin")
        
        // Only check connection status, don't start motion tracking automatically
        checkConnectionStatusViaAudioSession()
        
        // Post the current status immediately after checking (synchronously)
        postConnectionStatusNotification()
    }
    
    @objc private func handleStopMonitoring() {
        print("AirPodsDetectionService: Received stop monitoring request from Kotlin")
        stopMonitoring()
    }
    
    @objc private func handleStartMotionTracking() {
        print("AirPodsDetectionService: Received start motion tracking request from Kotlin")
        startMotionTracking()
    }
    
    private func postConnectionStatusNotification() {
        let userInfo: [String: Any] = [
            "isConnected": isConnected,
            "deviceName": currentDeviceName ?? "Unknown",
            // Preserve motion flag; once motion started, keep sending true so Kotlin doesn't reset
            "hasMotionData": headphoneMotionManager != nil && headphoneMotionManager!.isDeviceMotionActive
        ]
        
        NotificationCenter.default.post(
            name: NSNotification.Name("AirPodsConnectionStatusChanged"),
            object: self,
            userInfo: userInfo
        )
        
        print("AirPodsDetectionService: Posted notification - Connected: \(isConnected), Device: \(currentDeviceName ?? "None"), hasMotion=\(headphoneMotionManager?.isDeviceMotionActive ?? false)")
    }
    
    @objc func getConnectionStatus() -> [String: Any] {
        var hasMotion = false
        if #available(iOS 15.0, *) {
            hasMotion = headphoneMotionManager?.isDeviceMotionActive ?? false
        }
        
        return [
            "isConnected": isConnected,
            "deviceName": currentDeviceName ?? "Unknown",
            "hasMotionData": hasMotion,
            "pitchAngle": currentPitchAngle
        ]
    }
    
    @objc func startMonitoring() {
        if isMonitoring { return }
        
        isMonitoring = true
        
        // Start monitoring for audio route changes (fallback)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleRouteChange),
            name: AVAudioSession.routeChangeNotification,
            object: nil
        )
        
        print("AirPodsDetectionService: Started monitoring")
    }
    
    @objc func stopMonitoring() {
        if !isMonitoring { return }
        
        isMonitoring = false
        // Only remove the audio route change observer; keep command observers alive
        NotificationCenter.default.removeObserver(
            self,
            name: AVAudioSession.routeChangeNotification,
            object: nil
        )
        
        // Stop headphone motion updates
        print("AirPodsDetectionService: Stopping motion tracking - isMotionTracking was: \(isMotionTracking)")
        isMotionTracking = false
        print("AirPodsDetectionService: Set isMotionTracking = false")
        if #available(iOS 15.0, *) {
            headphoneMotionManager?.stopDeviceMotionUpdates()
            // Reset motion manager to ensure clean state
            headphoneMotionManager = nil
            print("AirPodsDetectionService: Reset headphoneMotionManager to nil")
        }
        
        // Reset internal state so next start is fresh
        currentPitchAngle = 0.0
        isConnected = false
        currentDeviceName = nil
        
        // Post a final status so Kotlin bridge clears its state immediately
        postConnectionStatusNotification()
        
        print("AirPodsDetectionService: Stopped monitoring and reset state")
    }
    
    @objc private func handleRouteChange(notification: Notification) {
        print("AirPodsDetectionService: Audio route changed")
        
        // Check connection status again
        checkConnectionStatusViaAudioSession()
    }
    
    deinit {
        stopMonitoring()
    }
}
