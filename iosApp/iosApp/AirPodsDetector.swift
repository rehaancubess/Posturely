import Foundation
import AVFoundation
import CoreBluetooth

@objc class AirPodsDetector: NSObject {
    
    @objc func isAirPodsConnected() -> Bool {
        let audioSession = AVAudioSession.sharedInstance()
        
        do {
            try audioSession.setActive(true)
            let currentRoute = audioSession.currentRoute
            
            for output in currentRoute.outputs {
                let portType = output.portType
                let portName = output.portName
                
                // Check for Bluetooth devices (AirPods, Beats)
                if portType == .bluetoothA2DP || 
                   portType == .bluetoothLE || 
                   portType == .bluetoothHFP {
                    
                    // Check if the device name contains AirPods or Beats identifiers
                    if portName.lowercased().contains("airpods") ||
                       portName.lowercased().contains("beats") ||
                       portName.lowercased().contains("powerbeats") ||
                       portName.lowercased().contains("studio") ||
                       portName.lowercased().contains("solo") {
                        return true
                    }
                }
                
                // Also check for wired AirPods (if connected via Lightning)
                if portType == .headphones || portType == .headsetMic {
                    if portName.lowercased().contains("airpods") {
                        return true
                    }
                }
            }
            
            return false
            
        } catch {
            print("Error accessing audio session: \(error)")
            return false
        }
    }
    
    @objc func getConnectedDeviceName() -> String? {
        let audioSession = AVAudioSession.sharedInstance()
        
        do {
            try audioSession.setActive(true)
            let currentRoute = audioSession.currentRoute
            
            for output in currentRoute.outputs {
                let portType = output.portType
                let portName = output.portName
                
                // Check for Bluetooth devices (AirPods, Beats)
                if portType == .bluetoothA2DP || 
                   portType == .bluetoothLE || 
                   portType == .bluetoothHFP {
                    
                    // Check if the device name contains AirPods or Beats identifiers
                    if portName.lowercased().contains("airpods") ||
                       portName.lowercased().contains("beats") ||
                       portName.lowercased().contains("powerbeats") ||
                       portName.lowercased().contains("studio") ||
                       portName.lowercased().contains("solo") {
                        return portName
                    }
                }
                
                // Also check for wired AirPods (if connected via Lightning)
                if portType == .headphones || portType == .headsetMic {
                    if portName.lowercased().contains("airpods") {
                        return portName
                    }
                }
            }
            
            return nil
            
        } catch {
            print("Error accessing audio session: \(error)")
            return nil
        }
    }
    
    @objc func startMonitoring() {
        // Start monitoring for audio route changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleRouteChange),
            name: AVAudioSession.routeChangeNotification,
            object: nil
        )
    }
    
    @objc func stopMonitoring() {
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc private func handleRouteChange(notification: Notification) {
        // Handle audio route changes (device connect/disconnect)
        print("Audio route changed")
    }
}
