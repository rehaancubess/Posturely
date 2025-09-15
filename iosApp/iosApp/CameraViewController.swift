import UIKit
import AVFoundation
import MediaPipeTasksVision

// App theme colors (match Compose)
private let appYellow = UIColor(red: 0xFE/255.0, green: 0xD8/255.0, blue: 0x67/255.0, alpha: 1.0) // 0xFFFED867
private let textPrimary = UIColor(red: 0x0F/255.0, green: 0x19/255.0, blue: 0x31/255.0, alpha: 1.0)

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
    private var statusFill: UIView!
    private var topBar: UIView!
    private var backButton: UIButton!
    private var titleLabel: UILabel!
    private var statusLabel: UILabel!
    private var poseDataLabel: UILabel!
    private var metricsLabel: UILabel!
    
    // Pose landmarks for visualization
    private var currentLandmarks: [PoseLandmark] = []
    private var flowStage = 0 // 0=find front, 1=find side after first countdown, 2=done
    private var countdownWorkItem: DispatchWorkItem?
    private var countdownPlayer: AVAudioPlayer?
    private var countdownDuration: Double = 3.0
    private var authToken: String = ""
    private var userId: String = ""
    private var scanId: String = UUID().uuidString
    // For base64 flow
    private var frontImageB64: String? = nil
    private var sideImageB64: String? = nil
    // Keep most recent video frame as UIImage (portrait + mirrored like preview)
    private var lastFrameImage: UIImage? = nil
    
    // Performance optimization: frame rate limiting
    private var lastProcessTime: TimeInterval = 0
    private let minFrameInterval: TimeInterval = 0.1 // 10 FPS like Android
    private var frontStableSince: CFTimeInterval? = nil
    private var sideStableSince: CFTimeInterval? = nil
    private let requiredStableSeconds: CFTimeInterval = 0.7
    
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
        // Ensure preview sits below UI but skeletal overlay draws above preview
        if let preview = videoPreviewLayer {
            // Keep existing order; do not remove other layers (like skeleton)
            if preview.superlayer == nil { cameraView.layer.addSublayer(preview) }
            view.bringSubviewToFront(statusFill)
            view.bringSubviewToFront(topBar)
        }
    }
    
    private func setupUI() {
        view.backgroundColor = appYellow
        
        // Camera view
        cameraView = UIView()
        cameraView.translatesAutoresizingMaskIntoConstraints = false
        cameraView.backgroundColor = .black
        view.addSubview(cameraView)
        
        // Status bar background fill (matches app yellow)
        statusFill = UIView()
        statusFill.translatesAutoresizingMaskIntoConstraints = false
        statusFill.backgroundColor = appYellow
        view.addSubview(statusFill)

        // Top bar with back button and centered title
        topBar = UIView()
        topBar.translatesAutoresizingMaskIntoConstraints = false
        topBar.backgroundColor = appYellow
        view.addSubview(topBar)
        
        backButton = UIButton(type: .system)
        backButton.translatesAutoresizingMaskIntoConstraints = false
        let chevron = UIImage(systemName: "chevron.left")
        backButton.setImage(chevron, for: .normal)
        backButton.tintColor = textPrimary
        backButton.contentEdgeInsets = UIEdgeInsets(top: 8, left: 8, bottom: 8, right: 8)
        backButton.addTarget(self, action: #selector(closeButtonTapped), for: .touchUpInside)
        topBar.addSubview(backButton)
        
        titleLabel = UILabel()
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.text = "Full Body Scan"
        titleLabel.textColor = textPrimary
        titleLabel.font = UIFont.systemFont(ofSize: 24, weight: .heavy)
        titleLabel.textAlignment = .center
        topBar.addSubview(titleLabel)
        
        // Optional labels (hidden, retained for debugging)
        statusLabel = UILabel()
        statusLabel.text = ""
        statusLabel.textColor = textPrimary
        statusLabel.backgroundColor = .clear
        statusLabel.textAlignment = .center
        statusLabel.isHidden = true
        statusLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(statusLabel)
        
        poseDataLabel = UILabel()
        poseDataLabel.text = ""
        poseDataLabel.textColor = textPrimary
        poseDataLabel.backgroundColor = .clear
        poseDataLabel.textAlignment = .left
        poseDataLabel.numberOfLines = 0
        poseDataLabel.isHidden = true
        poseDataLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(poseDataLabel)

        // Metrics HUD label (kept but hidden per request)
        metricsLabel = UILabel()
        metricsLabel.translatesAutoresizingMaskIntoConstraints = false
        metricsLabel.textColor = .white
        metricsLabel.numberOfLines = 0
        metricsLabel.textAlignment = .left
        metricsLabel.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        metricsLabel.layer.cornerRadius = 12
        metricsLabel.layer.masksToBounds = true
        metricsLabel.font = UIFont.monospacedDigitSystemFont(ofSize: 14, weight: .semibold)
        view.addSubview(metricsLabel)
        metricsLabel.isHidden = true
        
        // Setup constraints
        NSLayoutConstraint.activate([
            cameraView.topAnchor.constraint(equalTo: view.topAnchor),
            cameraView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            cameraView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            cameraView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            // Status fill covers the area above safe area
            statusFill.topAnchor.constraint(equalTo: view.topAnchor),
            statusFill.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            statusFill.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            statusFill.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),

            topBar.topAnchor.constraint(equalTo: statusFill.bottomAnchor),
            topBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            topBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            topBar.heightAnchor.constraint(equalToConstant: 56),
            
            backButton.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),
            backButton.leadingAnchor.constraint(equalTo: topBar.leadingAnchor, constant: 12),
            backButton.widthAnchor.constraint(equalToConstant: 40),
            backButton.heightAnchor.constraint(equalToConstant: 40),
            
            titleLabel.centerXAnchor.constraint(equalTo: topBar.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: topBar.centerYAnchor),
            
            statusLabel.topAnchor.constraint(equalTo: topBar.bottomAnchor, constant: 8),
            statusLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            statusLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            statusLabel.heightAnchor.constraint(equalToConstant: 1),
            
            poseDataLabel.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            poseDataLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            poseDataLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            poseDataLabel.heightAnchor.constraint(equalToConstant: 1),

            metricsLabel.topAnchor.constraint(equalTo: topBar.bottomAnchor, constant: 12),
            metricsLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            metricsLabel.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -16)
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

            // Ensure portrait orientation and front-camera mirroring so overlay matches preview
            if let connection = output.connection(with: .video) {
                if #available(iOS 17.0, *) {
                    if connection.isVideoRotationAngleSupported(90) { connection.videoRotationAngle = 90 }
                } else {
                    if connection.isVideoOrientationSupported { connection.videoOrientation = .portrait }
                }
                // Do NOT mirror the analysis output; keep it in camera-space.
                if connection.isVideoMirroringSupported {
                    connection.automaticallyAdjustsVideoMirroring = true
                }
            }
            
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
            // Bring top UI above preview
            view.bringSubviewToFront(topBar)
            view.bringSubviewToFront(statusFill)
            // Match orientation and mirror with output for consistency
            if let conn = videoPreviewLayer.connection {
                if #available(iOS 17.0, *) {
                    if conn.isVideoRotationAngleSupported(90) { conn.videoRotationAngle = 90 }
                } else {
                    if conn.isVideoOrientationSupported { conn.videoOrientation = .portrait }
                }
                if conn.isVideoMirroringSupported {
                    conn.automaticallyAdjustsVideoMirroring = false
                    conn.isVideoMirrored = true
                }
            }
        }
    }
    
    private func setupPoseDetection() {
        poseLandmarkerService = PoseLandmarkerService(delegate: self)
        // Ask Kotlin for Supabase token/userId for later upload
        NotificationCenter.default.addObserver(self, selector: #selector(handleSupabaseToken(_:)), name: NSNotification.Name("SupabaseToken"), object: nil)
        NotificationCenter.default.post(name: NSNotification.Name("RequestSupabaseToken"), object: nil)
    }

    @objc private func handleSupabaseToken(_ notif: Notification) {
        if let info = notif.userInfo {
            self.authToken = (info["token"] as? String) ?? ""
            self.userId = (info["userId"] as? String) ?? ""
        }
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
        // Hidden in final UI; keep method for potential debugging
        DispatchQueue.main.async { [weak self] in
            self?.statusLabel.text = message
        }
    }
    
    private func updatePoseData(_ landmarks: [PoseLandmark]) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            // Simple temporal smoothing (exponential moving average)
            let alpha: CGFloat = 0.4 // higher = snappier, lower = smoother
            if self.currentLandmarks.count == landmarks.count && !self.currentLandmarks.isEmpty {
                var smoothed: [PoseLandmark] = []
                smoothed.reserveCapacity(landmarks.count)
                for i in 0..<landmarks.count {
                    let prev = self.currentLandmarks[i]
                    let cur = landmarks[i]
                    let sx = Float((1 - alpha) * CGFloat(prev.x) + alpha * CGFloat(cur.x))
                    let sy = Float((1 - alpha) * CGFloat(prev.y) + alpha * CGFloat(cur.y))
                    let sz = Float((1 - alpha) * CGFloat(prev.z) + alpha * CGFloat(cur.z))
                    smoothed.append(PoseLandmark(x: sx, y: sy, z: sz, visibility: cur.visibility, presence: cur.presence))
                }
                self.currentLandmarks = smoothed
            } else {
            self.currentLandmarks = landmarks
            }
            
            // Optional debug text (hidden)
            self.poseDataLabel.text = PoseDataManager.shared.currentPoseData
            
            // Draw skeleton
            self.drawSkeleton()
            
            // Update shared data for Compose
            PoseDataManager.shared.updatePoseData(self.currentLandmarks)

            // Auto-flow: detect front, then side frames with countdowns
            self.evaluateAutoFlow()

            // HUD is hidden by design now
        }
    }

    private func updateMetricsHUD() {
        guard currentLandmarks.count >= 29 else { metricsLabel.text = "Detecting…"; return }
        let ls = currentLandmarks[11]
        let rs = currentLandmarks[12]
        let lh = currentLandmarks[23]
        let rh = currentLandmarks[24]
        let shoulderDx = abs(ls.x - rs.x)
        let shoulderDy = abs(ls.y - rs.y)
        let leftKnee = kneeAngleDeg(hip: currentLandmarks[23], knee: currentLandmarks[25], ankle: currentLandmarks[27])
        let rightKnee = kneeAngleDeg(hip: currentLandmarks[24], knee: currentLandmarks[26], ankle: currentLandmarks[28])
        let sideMetric = abs(Float(ls.x - rs.x)) / max(0.001, abs(Float(ls.y - rs.y))) // width-to-vertical ratio
        metricsLabel.text = String(format: "ShoulderWidth: %.2f\nShoulderLevel: %.3f\nLeftKnee: %.0f°  RightKnee: %.0f°\nSideRatio: %.2f",
                                   shoulderDx, shoulderDy, leftKnee, rightKnee, sideMetric)
    }

    private func evaluateAutoFlow() {
        // Require at least core joints visible
        guard currentLandmarks.count >= 29 else { frontStableSince = nil; sideStableSince = nil; return }
        let now = CACurrentMediaTime()
        if flowStage == 0 {
            if isFrontPoseVisible() {
                if frontStableSince == nil { frontStableSince = now }
                if let since = frontStableSince, now - since >= requiredStableSeconds {
                    flowStage = 1
                    frontStableSince = nil
                    print("[Scan] Front pose confirmed → playing countdown")
                    playCountdownThen(delaySeconds: 3.0) {
                        // After countdown we start looking for side view
                    }
                }
            } else {
                frontStableSince = nil
            }
        } else if flowStage == 1 {
            if isSidePoseVisible() {
                if sideStableSince == nil { sideStableSince = now }
                if let since = sideStableSince, now - since >= requiredStableSeconds {
                    flowStage = 2
                    sideStableSince = nil
                    print("[Scan] Side pose confirmed → playing countdown & finishing")
                    playCountdownThen(delaySeconds: 3.0) {
                        self.closeAfterFlow()
                    }
                }
            } else {
                sideStableSince = nil
            }
        }
    }

    private func isFrontPoseVisible() -> Bool {
        // Heuristic per observed values:
        // - ShoulderWidth spike indicates person/front (typically 0.15–0.30)
        // - Shoulders roughly level
        // - At least one knee angle in 150°–180° range
        let ls = currentLandmarks[11]
        let rs = currentLandmarks[12]
        let shoulderDx = abs(ls.x - rs.x)
        let shoulderDy = abs(ls.y - rs.y)
        let shouldersOK = (shoulderDx > 0.15) && (shoulderDy < 0.06)
        let leftKnee = kneeAngleDeg(hip: currentLandmarks[23], knee: currentLandmarks[25], ankle: currentLandmarks[27])
        let rightKnee = kneeAngleDeg(hip: currentLandmarks[24], knee: currentLandmarks[26], ankle: currentLandmarks[28])
        let kneeOK = (leftKnee >= 150 && leftKnee <= 185) || (rightKnee >= 150 && rightKnee <= 185)
        return shouldersOK && kneeOK
    }

    private func kneeAngleDeg(hip: PoseLandmark, knee: PoseLandmark, ankle: PoseLandmark) -> Double {
        // Angle at knee between hip->knee and ankle->knee
        let v1x = Double(hip.x - knee.x)
        let v1y = Double(hip.y - knee.y)
        let v2x = Double(ankle.x - knee.x)
        let v2y = Double(ankle.y - knee.y)
        let dot = v1x * v2x + v1y * v2y
        let n1 = sqrt(v1x * v1x + v1y * v1y)
        let n2 = sqrt(v2x * v2x + v2y * v2y)
        if n1 < 1e-6 || n2 < 1e-6 { return 180.0 } // treat as straight if degenerate
        var cosT = dot / (n1 * n2)
        cosT = max(-1.0, min(1.0, cosT))
        return acos(cosT) * 180.0 / Double.pi
    }

    private func isSidePoseVisible() -> Bool {
        // Updated heuristic based on your readings:
        // - Shoulder width very small (profile) < 0.08
        // - Torso length reasonable (> ~0.18) to ensure person presence
        // - Knee angles plausible (150–185)
        let ls = currentLandmarks[11]
        let rs = currentLandmarks[12]
        let lh = currentLandmarks[23]
        let rh = currentLandmarks[24]
        let shoulderDx = abs(ls.x - rs.x)
        let torsoLen = abs(((lh.y + rh.y) * 0.5) - ((ls.y + rs.y) * 0.5))
        let shoulderNarrow = shoulderDx < 0.08
        let torsoOK = torsoLen > 0.18
        let leftKnee = kneeAngleDeg(hip: currentLandmarks[23], knee: currentLandmarks[25], ankle: currentLandmarks[27])
        let rightKnee = kneeAngleDeg(hip: currentLandmarks[24], knee: currentLandmarks[26], ankle: currentLandmarks[28])
        let kneesOK = (leftKnee >= 150 && leftKnee <= 185) || (rightKnee >= 150 && rightKnee <= 185)
        return shoulderNarrow && torsoOK && kneesOK
    }

    private func playCountdownThen(delaySeconds: Double, completion: @escaping () -> Void) {
        // Play countdown.mp3 from bundle and use its actual duration
        playCountdownSound()
        countdownWorkItem?.cancel()
        let work = DispatchWorkItem(block: completion)
        countdownWorkItem = work
        let wait = max(delaySeconds, countdownDuration)
        DispatchQueue.main.asyncAfter(deadline: .now() + wait) {
            // Hide HUD while capturing
            self.metricsLabel.isHidden = true
            // Capture screenshot first
            let image = self.compositeCurrentFrameWithSkeleton()
            // Store image for edge function (base64 payload)
            if self.flowStage == 1 {
                self.frontImageB64 = self.jpegBase64(image: image)
                print("[Scan] Saved FRONT image (base64 length=\(self.frontImageB64?.count ?? 0))")
            } else if self.flowStage == 2 {
                self.sideImageB64 = self.jpegBase64(image: image)
                print("[Scan] Saved SIDE image (base64 length=\(self.sideImageB64?.count ?? 0))")
                // Notify Compose (Kotlin) with both base64s so it can call API if desired
                let info: [String: Any] = [
                    "scanId": self.scanId,
                    "frontBase64": self.frontImageB64 ?? "",
                    "sideBase64": self.sideImageB64 ?? ""
                ]
                NotificationCenter.default.post(name: NSNotification.Name("ScanImagesReady"), object: nil, userInfo: info)
                self.triggerEdgeFunctionIfReady()
            }
            // Continue flow AFTER starting any network calls
            work.perform()
        }
    }

    private func playCountdownSound() {
        do {
            // Prepare audio session so it plays even in silent mode
            let session = AVAudioSession.sharedInstance()
            try? session.setCategory(.playback, mode: .default)
            try? session.setActive(true)

            guard let url = resolveAudioURL(basename: "countdown", ext: "mp3") else { return }
            countdownPlayer?.stop()
            countdownPlayer = try AVAudioPlayer(contentsOf: url)
            countdownPlayer?.numberOfLoops = 0
            countdownPlayer?.prepareToPlay()
            countdownDuration = max(0.1, countdownPlayer?.duration ?? 3.0)
            countdownPlayer?.play()
        } catch {
            // Ignore audio errors silently
        }
    }

    private func resolveAudioURL(basename: String, ext: String) -> URL? {
        // Try common locations
        if let u = Bundle.main.url(forResource: basename, withExtension: ext) { return u }
        if let u = Bundle.main.url(forResource: "files/\(basename)", withExtension: ext) { return u }
        if let u = Bundle.main.url(forResource: "Sounds/\(basename)", withExtension: ext) { return u }
        // Fallback: scan all bundle mp3s and match by filename
        if let urls = Bundle.main.urls(forResourcesWithExtension: ext, subdirectory: nil) {
            let lower = (basename + "." + ext).lowercased()
            if let exact = urls.first(where: { $0.lastPathComponent.lowercased() == lower }) { return exact }
            if let byBase = urls.first(where: { $0.deletingPathExtension().lastPathComponent.lowercased() == basename.lowercased() }) { return byBase }
        }
        return nil
    }

    private func closeAfterFlow() {
        // Stop camera and dismiss
        countdownWorkItem?.cancel()
        self.stopCamera()
        self.dismiss(animated: true)
    }

    private func captureFrame() -> UIImage? {
        UIGraphicsBeginImageContextWithOptions(cameraView.bounds.size, false, UIScreen.main.scale)
        defer { UIGraphicsEndImageContext() }
        cameraView.drawHierarchy(in: cameraView.bounds, afterScreenUpdates: false)
        let img = UIGraphicsGetImageFromCurrentImageContext()
        return img
    }

    // Convert sample buffer to UIImage (portrait + mirrored to match preview)
    private func uiImage(from sampleBuffer: CMSampleBuffer) -> UIImage? {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return nil }
        let ciImage = CIImage(cvImageBuffer: pixelBuffer)
        // Rotate to portrait and mirror horizontally
        let oriented = ciImage.oriented(forExifOrientation: 6) // 6 = rotate 90° right (portrait)
        let mirrored = oriented.oriented(forExifOrientation: 2) // 2 = upMirrored
        let context = CIContext(options: nil)
        guard let cg = context.createCGImage(mirrored, from: mirrored.extent) else { return nil }
        return UIImage(cgImage: cg)
    }

    // Composite latest video frame with current skeleton overlay into a single UIImage
    private func compositeCurrentFrameWithSkeleton() -> UIImage? {
        guard let base = lastFrameImage else { return captureFrame() }
        let size = base.size
        UIGraphicsBeginImageContextWithOptions(size, true, base.scale)
        base.draw(in: CGRect(origin: .zero, size: size))
        let ctx = UIGraphicsGetCurrentContext()
        ctx?.setStrokeColor(UIColor.green.cgColor)
        ctx?.setLineWidth(6.0)
        // Mapping from normalized (x,y) → portrait mirrored pixel space
        func pt(_ norm: CGPoint) -> CGPoint {
            let x = CGFloat(norm.y) * size.width
            let y = CGFloat(1.0 - norm.x) * size.height
            return CGPoint(x: x, y: y)
        }
        // For the saved/composited image, flip overlay vertically (top ↔ bottom) only
        func ptFlipVertical(_ norm: CGPoint) -> CGPoint {
            let p = pt(norm)
            return CGPoint(x: p.x, y: size.height - p.y)
        }
        let connections: [(Int, Int)] = [
            (9,10), (11,12), (11,23), (12,24), (23,24), (11,13), (13,15), (12,14), (14,16), (23,25), (25,27), (24,26), (26,28)
        ]
        for (a,b) in connections {
            if a < currentLandmarks.count && b < currentLandmarks.count {
                let aN = CGPoint(x: CGFloat(currentLandmarks[a].x), y: CGFloat(currentLandmarks[a].y))
                let bN = CGPoint(x: CGFloat(currentLandmarks[b].x), y: CGFloat(currentLandmarks[b].y))
                let A = ptFlipVertical(aN); let B = ptFlipVertical(bN)
                ctx?.move(to: A)
                ctx?.addLine(to: B)
                ctx?.strokePath()
            }
        }
        let composed = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return composed
    }

    private func jpegBase64(image: UIImage?) -> String? {
        guard let image = image else { return nil }
        guard let data = image.jpegData(compressionQuality: 0.85) else { return nil }
        return data.base64EncodedString()
    }

    private func triggerEdgeFunctionIfReady() {
        // Prefer base64 payload to avoid storage setup
        guard let front = frontImageB64, let side = sideImageB64 else { return }
        guard let url = URL(string: "https://lexlrxlvmbpfzenzzgld.supabase.co/functions/v1/posture_report") else { return }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = [
            "scanId": scanId,
            "frontBase64": front,
            "sideBase64": side
        ]
        req.httpBody = try? JSONSerialization.data(withJSONObject: body, options: [])
        print("[Scan] Calling edge function posture_report with scanId=\(scanId)")
        URLSession.shared.dataTask(with: req) { data, response, error in
            if let error = error { print("[Scan] Edge function error: \(error.localizedDescription)"); return }
            if let http = response as? HTTPURLResponse { print("[Scan] Edge function status: \(http.statusCode)") }
        }.resume()
    }
    
    private func drawSkeleton() {
        // Remove previous skeleton
        // Remove previous skeleton if present, then recreate
        skeletonLayer?.removeFromSuperlayer()
        
        guard !currentLandmarks.isEmpty else { return }
        
        let skeletonLayer = CAShapeLayer()
        skeletonLayer.strokeColor = UIColor.green.cgColor
        skeletonLayer.lineWidth = 3.0
        skeletonLayer.fillColor = UIColor.clear.cgColor
        
        let path = UIBezierPath()
        
        // Helper: convert MediaPipe normalized image coords to layer points accounting for portrait and mirroring
        func layerPoint(fromNormalized norm: CGPoint) -> CGPoint {
            // MediaPipe normalized -> capture device point in portrait (no horizontal flip)
            // Rotate 90°: (x, y) -> (y, 1 - x)
            let devicePoint = CGPoint(x: norm.y, y: 1.0 - norm.x)
            if let preview = videoPreviewLayer {
                return preview.layerPointConverted(fromCaptureDevicePoint: devicePoint)
            } else {
                return CGPoint(x: devicePoint.x * cameraView.bounds.width,
                               y: devicePoint.y * cameraView.bounds.height)
            }
        }
        
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
            
            let normStart = CGPoint(x: CGFloat(currentLandmarks[start].x), y: CGFloat(currentLandmarks[start].y))
            let normEnd = CGPoint(x: CGFloat(currentLandmarks[end].x), y: CGFloat(currentLandmarks[end].y))
            let startPoint = layerPoint(fromNormalized: normStart)
            let endPoint = layerPoint(fromNormalized: normEnd)
            
            path.move(to: startPoint)
            path.addLine(to: endPoint)
        }
        
        skeletonLayer.path = path.cgPath
        // Add above preview layer
        cameraView.layer.addSublayer(skeletonLayer)
        // Keep UI above everything
        view.bringSubviewToFront(statusFill)
        view.bringSubviewToFront(topBar)
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

        // Cache latest frame as UIImage for compositing
        if let img = uiImage(from: sampleBuffer) {
            self.lastFrameImage = img
        }
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