import Foundation
import UIKit
import AVFoundation
import RevenueCat
import RevenueCatUI

class NotificationObserver: NSObject {
    private var bridge: PostureTrackingBridge?
    private var backgroundPoseService: PoseLandmarkerService?
    private var backgroundCameraSession: AVCaptureSession?
    
    override init() {
        super.init()
        setupNotifications()
        bridge = PostureTrackingBridge()
    }
    
    private func setupNotifications() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(startPostureTracking),
            name: NSNotification.Name("StartPostureTracking"),
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(stopPostureTracking),
            name: NSNotification.Name("StopPostureTracking"),
            object: nil
        )

        // Present native scan camera on demand (separate from background tracking)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(presentScanCamera),
            name: NSNotification.Name("PresentScanCamera"),
            object: nil
        )

        // Request camera permission on demand from Kotlin
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(requestCameraPermission),
            name: NSNotification.Name("RequestCameraPermission"),
            object: nil
        )

        // Present RevenueCat paywall from KMP side
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(presentRevenueCatPaywall),
            name: NSNotification.Name("PresentRevenueCatPaywall"),
            object: nil
        )

        // Open Screen Time configuration
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(openAppLockConfig),
            name: NSNotification.Name("OpenAppLockConfig"),
            object: nil
        )

        // Start Screen Time app lock schedule
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(startAppLock(_:)),
            name: NSNotification.Name("StartAppLock"),
            object: nil
        )
    }
    
    @objc private func startPostureTracking() {
        DispatchQueue.global(qos: .background).async { [weak self] in
            self?.startBackgroundPoseDetection()
        }
    }
    
    @objc private func stopPostureTracking() {
        DispatchQueue.global(qos: .background).async { [weak self] in
            self?.stopBackgroundPoseDetection()
        }
    }
    
    private func startBackgroundPoseDetection() {
        // Start background camera session
        backgroundCameraSession = AVCaptureSession()
        backgroundCameraSession?.sessionPreset = .high
        
        guard let frontCamera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front) else {
            return
        }
        
        do {
            let input = try AVCaptureDeviceInput(device: frontCamera)
            let output = AVCaptureVideoDataOutput()
            output.setSampleBufferDelegate(self, queue: DispatchQueue.global(qos: .userInteractive))
            
            // Set the pixel format to BGRA which MediaPipe expects
            output.videoSettings = [
                kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA
            ]
            
            backgroundCameraSession?.addInput(input)
            backgroundCameraSession?.addOutput(output)
            
            // Initialize pose detection service
            backgroundPoseService = PoseLandmarkerService(delegate: self)
            
            // Start camera session
            backgroundCameraSession?.startRunning()
        } catch {
            // Handle error silently
        }
    }
    
    private func stopBackgroundPoseDetection() {
        backgroundCameraSession?.stopRunning()
        backgroundPoseService?.stopDetection()
        backgroundCameraSession = nil
        backgroundPoseService = nil
    }

    // MARK: - Presentation of dedicated scan camera with overlay
    @objc private func presentScanCamera() {
        DispatchQueue.main.async {
            guard let root = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .flatMap({ $0.windows })
                .first(where: { $0.isKeyWindow })?.rootViewController else {
                return
            }
            let controller = CameraViewController()
            controller.modalPresentationStyle = .fullScreen
            root.present(controller, animated: true)
        }
    }

    // MARK: - RevenueCat Paywall Presentation
    @objc private func presentRevenueCatPaywall() {
        DispatchQueue.main.async {
            guard let root = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .flatMap({ $0.windows })
                .first(where: { $0.isKeyWindow })?.rootViewController else {
                return
            }
            // First, check if user already has an active entitlement
            Purchases.shared.getCustomerInfo { info, _ in
                if let entitlements = info?.entitlements.active, entitlements["premium"] != nil {
                    // User is subscribed; do not show paywall
                    return
                }
                // Not subscribed → present paywall
                Purchases.shared.getOfferings { offerings, _ in
                    if let current = offerings?.current {
                        let controller = RevenueCatUI.PaywallViewController(offering: current)
                        controller.modalPresentationStyle = .fullScreen
                        controller.isModalInPresentation = true // prevent swipe-to-dismiss
                        root.present(controller, animated: true)
                    } else {
                        let fallback = RevenueCatUI.PaywallViewController()
                        fallback.modalPresentationStyle = .fullScreen
                        fallback.isModalInPresentation = true
                        root.present(fallback, animated: true)
                    }
                }
            }
        }
    }

    // MARK: - App Lock (Screen Time) Stubs
    @objc private func openAppLockConfig() {
        AppLockManager.requestAuthorizationIfNeeded { granted in
            DispatchQueue.main.async {
                let vc = UIApplication.shared.connectedScenes
                    .compactMap({ $0 as? UIWindowScene })
                    .flatMap({ $0.windows })
                    .first(where: { $0.isKeyWindow })?.rootViewController
                AppLockManager.presentPicker(from: vc)
            }
        }
    }

    @objc private func startAppLock(_ notification: Notification) {
        let from = (notification.userInfo?["from"] as? String) ?? ""
        let till = (notification.userInfo?["till"] as? String) ?? ""
        AppLockManager.startLock(from: from, till: till)
    }

    // MARK: - Camera permission request handler
    @objc private func requestCameraPermission() {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        switch status {
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { _ in }
        default:
            break
        }
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: - AVCaptureVideoDataOutputSampleBufferDelegate
extension NotificationObserver: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        let presentationTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
        let timestamp = Int(CMTimeGetSeconds(presentationTime) * 1000)
        backgroundPoseService?.detectPose(in: sampleBuffer, timestamp: timestamp)
    }
}

// MARK: - PoseDetectionDelegate
extension NotificationObserver: PoseDetectionDelegate {
    func didDetectPose(_ landmarks: [PoseLandmark], worldLandmarks: [PoseLandmark]) {
        // Update shared data for Compose
        PoseDataManager.shared.updatePoseData(landmarks)
        
        // Post notification with landmark data for Compose
        let landmarkData = landmarks.map { landmark in
            [
                "x": landmark.x,
                "y": landmark.y,
                "z": landmark.z,
                "visibility": landmark.visibility,
                "presence": landmark.presence
            ]
        }
        
        NotificationCenter.default.post(
            name: NSNotification.Name("PoseLandmarksUpdated"),
            object: nil,
            userInfo: ["landmarks": landmarkData]
        )
    }
    
    func didEncounterError(_ error: Error) {
        // Handle error silently
    }
} 