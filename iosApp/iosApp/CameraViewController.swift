import UIKit
import AVFoundation
import MediaPipeTasksVision

// Shared data structure for pose data
class PoseDataManager {
    static let shared = PoseDataManager()
    private init() {}
    
    var currentPoseData: String = "No pose data available"
    var currentLandmarks: [PoseLandmark] = []
    var lastUpdateTime: Date = Date()
    
    // Static properties for easy access from Kotlin
    static var landmarkCount: Int = 0
    static var landmarkX: [Float] = Array(repeating: 0.5, count: 33)
    static var landmarkY: [Float] = Array(repeating: 0.5, count: 33)
    
    func updatePoseData(_ landmarks: [PoseLandmark]) {
        currentLandmarks = landmarks
        
        // Calculate distances and ratios (in normalized coordinates for ratio)
        let noseToShoulderCenter = sqrt(
            pow(landmarks[0].x - (landmarks[11].x + landmarks[12].x) / 2, 2) +
            pow(landmarks[0].y - (landmarks[11].y + landmarks[12].y) / 2, 2)
        )
        
        // Calculate shoulder width for reference
        let shoulderWidth = sqrt(
            pow(landmarks[11].x - landmarks[12].x, 2) +
            pow(landmarks[11].y - landmarks[12].y, 2)
        )
        
        // Calculate ratio (normalized by shoulder width)
        let ratioCenter = noseToShoulderCenter / shoulderWidth
        
        let poseInfo = """
        Pose Detected!
        Landmarks: \(landmarks.count)
        Head: x: \(String(format: "%.3f", landmarks[0].x)), y: \(String(format: "%.3f", landmarks[0].y))
        Shoulders: L(\(String(format: "%.3f", landmarks[11].x)), R(\(String(format: "%.3f", landmarks[12].x)))
        
        Distance Ratio (normalized):
        Nose to Shoulder Center: \(String(format: "%.2f", ratioCenter))
        """
        currentPoseData = poseInfo
        lastUpdateTime = Date()
        
        // Update global variables for Kotlin access
        PoseDataBridge.updateGlobalLandmarks(landmarks)
    }
    
    func getLandmarks() -> [PoseLandmark] {
        return currentLandmarks
    }
}

class CameraViewController: UIViewController {
    private var captureSession: AVCaptureSession?
    private var videoPreviewLayer: AVCaptureVideoPreviewLayer?
    private var poseLandmarkerService: PoseLandmarkerService?
    private var skeletonLayer: CAShapeLayer?
    
    // UI Elements
    private var cameraView: UIView!
    private var statusLabel: UILabel!
    private var poseDataLabel: UILabel!
    private var closeButton: UIButton!
    
    // Pose landmarks for visualization
    private var currentLandmarks: [PoseLandmark] = []
    
    // Performance optimization: frame rate limiting
    private var lastProcessTime: TimeInterval = 0
    private let minFrameInterval: TimeInterval = 0.1 // 10 FPS like Android
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupCamera()
        setupPoseDetection()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        startCamera()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        stopCamera()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        // Update video preview layer frame when view layout changes
        videoPreviewLayer?.frame = cameraView.bounds
    }
    
    private func setupUI() {
        view.backgroundColor = .black
        
        // Camera view
        cameraView = UIView()
        cameraView.translatesAutoresizingMaskIntoConstraints = false
        cameraView.backgroundColor = .black
        view.addSubview(cameraView)
        
        // Close button
        closeButton = UIButton(type: .system)
        closeButton.setTitle("✕", for: .normal)
        closeButton.setTitleColor(.white, for: .normal)
        closeButton.titleLabel?.font = UIFont.systemFont(ofSize: 24, weight: .bold)
        closeButton.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        closeButton.layer.cornerRadius = 20
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.addTarget(self, action: #selector(closeButtonTapped), for: .touchUpInside)
        view.addSubview(closeButton)
        
        // Status label
        statusLabel = UILabel()
        statusLabel.text = "Initializing camera..."
        statusLabel.textColor = .white
        statusLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        statusLabel.textAlignment = .center
        statusLabel.layer.cornerRadius = 8
        statusLabel.layer.masksToBounds = true
        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(statusLabel)
        
        // Pose data label
        poseDataLabel = UILabel()
        poseDataLabel.text = "Camera ready - MediaPipe integration coming soon"
        poseDataLabel.textColor = .white
        poseDataLabel.backgroundColor = UIColor.black.withAlphaComponent(0.7)
        poseDataLabel.textAlignment = .left
        poseDataLabel.numberOfLines = 0
        poseDataLabel.layer.cornerRadius = 8
        poseDataLabel.layer.masksToBounds = true
        poseDataLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(poseDataLabel)
        
        // Setup constraints
        NSLayoutConstraint.activate([
            cameraView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            cameraView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            cameraView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            cameraView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            closeButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            closeButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            closeButton.widthAnchor.constraint(equalToConstant: 40),
            closeButton.heightAnchor.constraint(equalToConstant: 40),
            
            statusLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            statusLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            statusLabel.trailingAnchor.constraint(equalTo: closeButton.leadingAnchor, constant: -20),
            statusLabel.heightAnchor.constraint(equalToConstant: 40),
            
            poseDataLabel.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            poseDataLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            poseDataLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            poseDataLabel.heightAnchor.constraint(greaterThanOrEqualToConstant: 60)
        ])
    }
    
    @objc private func closeButtonTapped() {
        dismiss(animated: true, completion: nil)
    }
    
    private func setupCamera() {
        captureSession = AVCaptureSession()
        captureSession?.sessionPreset = .high
        
        // Check camera authorization first
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureCamera()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    if granted {
                        self?.configureCamera()
                    } else {
                        self?.updateStatus("Camera access denied")
                    }
                }
            }
        case .denied, .restricted:
            updateStatus("Camera access denied - check Settings")
        @unknown default:
            updateStatus("Unknown camera authorization status")
        }
    }
    
    private func configureCamera() {
        guard let frontCamera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front) else {
            updateStatus("Failed to access front camera")
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
            
            captureSession?.addInput(input)
            captureSession?.addOutput(output)
            
            setupVideoPreviewLayer()
            updateStatus("Front camera configured successfully")
        } catch {
            updateStatus("Failed to setup front camera: \(error.localizedDescription)")
        }
    }
    
    private func setupVideoPreviewLayer() {
        guard let captureSession = captureSession else { return }
        
        videoPreviewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        videoPreviewLayer?.videoGravity = .resizeAspectFill
        videoPreviewLayer?.frame = cameraView.bounds
        
        if let videoPreviewLayer = videoPreviewLayer {
            cameraView.layer.addSublayer(videoPreviewLayer)
        }
    }
    
    private func setupPoseDetection() {
        poseLandmarkerService = PoseLandmarkerService(delegate: self)
    }
    
    private func startCamera() {
        DispatchQueue.global(qos: .background).async { [weak self] in
            self?.captureSession?.startRunning()
            DispatchQueue.main.async {
                self?.updateStatus("Front camera started - Pose detection active")
            }
        }
    }
    
    private func stopCamera() {
        DispatchQueue.global(qos: .background).async { [weak self] in
            self?.captureSession?.stopRunning()
            self?.poseLandmarkerService?.stopDetection()
        }
    }
    
    private func updateStatus(_ message: String) {
        DispatchQueue.main.async { [weak self] in
            self?.statusLabel.text = message
        }
    }
    
    private func updatePoseData(_ landmarks: [PoseLandmark]) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            self.currentLandmarks = landmarks
            
            // Update pose data display
            self.poseDataLabel.text = PoseDataManager.shared.currentPoseData
            
            // Draw skeleton
            self.drawSkeleton()
            
            // Update shared data for Compose
            PoseDataManager.shared.updatePoseData(landmarks)
        }
    }
    
    private func drawSkeleton() {
        // Remove previous skeleton
        skeletonLayer?.removeFromSuperlayer()
        
        guard !currentLandmarks.isEmpty else { return }
        
        let skeletonLayer = CAShapeLayer()
        skeletonLayer.strokeColor = UIColor.green.cgColor
        skeletonLayer.lineWidth = 3.0
        skeletonLayer.fillColor = UIColor.clear.cgColor
        
        let path = UIBezierPath()
        
        // Convert normalized coordinates to view coordinates
        let viewWidth = cameraView.bounds.width
        let viewHeight = cameraView.bounds.height
        
        // Draw key connections (simplified skeleton - only key body parts like Android)
        let connections: [(Int, Int)] = [
            // Face
            (9, 10), // mouth left to mouth right
            
            // Core body
            (11, 12), // left shoulder to right shoulder
            (11, 23), // left shoulder to left hip
            (12, 24), // right shoulder to right hip
            (23, 24), // left hip to right hip
            
            // Arms
            (11, 13), // left shoulder to left elbow
            (13, 15), // left elbow to left wrist
            (12, 14), // right shoulder to right elbow
            (14, 16), // right elbow to right wrist
            
            // Legs
            (23, 25), // left hip to left knee
            (25, 27), // left knee to left ankle
            (24, 26), // right hip to right knee
            (26, 28)  // right knee to right ankle
        ]
        
        for (start, end) in connections {
            guard start < currentLandmarks.count && end < currentLandmarks.count else { continue }
            
            let startPoint = CGPoint(
                x: CGFloat(currentLandmarks[start].x) * viewWidth,
                y: CGFloat(currentLandmarks[start].y) * viewHeight
            )
            let endPoint = CGPoint(
                x: CGFloat(currentLandmarks[end].x) * viewWidth,
                y: CGFloat(currentLandmarks[end].y) * viewHeight
            )
            
            path.move(to: startPoint)
            path.addLine(to: endPoint)
        }
        
        skeletonLayer.path = path.cgPath
        cameraView.layer.addSublayer(skeletonLayer)
        self.skeletonLayer = skeletonLayer
    }
}

// MARK: - AVCaptureVideoDataOutputSampleBufferDelegate
extension CameraViewController: AVCaptureVideoDataOutputSampleBufferDelegate {
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        // Frame rate limiting for better performance
        let currentTime = CACurrentMediaTime()
        if currentTime - lastProcessTime < minFrameInterval {
            return
        }
        lastProcessTime = currentTime
        
        let presentationTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
        let timestamp = Int(CMTimeGetSeconds(presentationTime) * 1000)
        poseLandmarkerService?.detectPose(in: sampleBuffer, timestamp: timestamp)
    }
}

// MARK: - PoseDetectionDelegate
extension CameraViewController: PoseDetectionDelegate {
    func didDetectPose(_ landmarks: [PoseLandmark], worldLandmarks: [PoseLandmark]) {
        updatePoseData(landmarks)
    }
    
    func didEncounterError(_ error: Error) {
        updateStatus("Error: \(error.localizedDescription)")
    }
} 