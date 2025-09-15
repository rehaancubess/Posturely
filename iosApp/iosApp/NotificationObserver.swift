import Foundation
import UIKit
import AVFoundation

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