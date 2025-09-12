import Foundation
import MediaPipeTasksVision
import AVFoundation

// Protocol for communicating pose detection results back to Compose
protocol PoseDetectionDelegate: AnyObject {
    func didDetectPose(_ landmarks: [PoseLandmark], worldLandmarks: [PoseLandmark])
    func didEncounterError(_ error: Error)
}

class PoseLandmarkerService: NSObject {
    private var poseLandmarker: PoseLandmarker?
    private weak var delegate: PoseDetectionDelegate?
    
    init(delegate: PoseDetectionDelegate) {
        self.delegate = delegate
        super.init()
        setupPoseLandmarker()
    }
    
    private func setupPoseLandmarker() {
        // Use full model like the successful Android implementation
        guard let modelPath = Bundle.main.path(forResource: "pose_landmarker", ofType: "task") else {
            delegate?.didEncounterError(NSError(domain: "PoseLandmarker", code: 1, userInfo: [NSLocalizedDescriptionKey: "Model file not found"]))
            return
        }
        
        let options = PoseLandmarkerOptions()
        options.baseOptions.modelAssetPath = modelPath
        options.runningMode = .liveStream
        // Remove custom confidence thresholds to use defaults like Android
        options.poseLandmarkerLiveStreamDelegate = self
        
        do {
            poseLandmarker = try PoseLandmarker(options: options)
        } catch {
            delegate?.didEncounterError(error)
        }
    }
    
    func detectPose(in sampleBuffer: CMSampleBuffer, timestamp: Int) {
        guard let poseLandmarker = poseLandmarker else {
            return
        }
        
        do {
            let image = try MPImage(sampleBuffer: sampleBuffer)
            try poseLandmarker.detectAsync(image: image, timestampInMilliseconds: timestamp)
        } catch {
            delegate?.didEncounterError(error)
        }
    }
    
    func stopDetection() {
        poseLandmarker = nil
    }
}

// MARK: - PoseLandmarkerLiveStreamDelegate
extension PoseLandmarkerService: PoseLandmarkerLiveStreamDelegate {
    func poseLandmarker(
        _ poseLandmarker: PoseLandmarker,
        didFinishDetection result: PoseLandmarkerResult?,
        timestampInMilliseconds: Int,
        error: Error?
    ) {
        DispatchQueue.main.async { [weak self] in
            if let error = error {
                self?.delegate?.didEncounterError(error)
                return
            }
            
            guard let result = result,
                  !result.landmarks.isEmpty,
                  !result.worldLandmarks.isEmpty else {
                return
            }
            
            // Convert MediaPipe landmarks to our custom format
            let landmarks = result.landmarks[0].map { landmark in
                PoseLandmark(
                    x: landmark.x,
                    y: landmark.y,
                    z: landmark.z,
                    visibility: landmark.visibility?.floatValue ?? 0.0,
                    presence: landmark.presence?.floatValue ?? 0.0
                )
            }
            
            let worldLandmarks = result.worldLandmarks[0].map { landmark in
                PoseLandmark(
                    x: landmark.x,
                    y: landmark.y,
                    z: landmark.z,
                    visibility: landmark.visibility?.floatValue ?? 0.0,
                    presence: landmark.presence?.floatValue ?? 0.0
                )
            }
            
            self?.delegate?.didDetectPose(landmarks, worldLandmarks: worldLandmarks)
        }
    }
} 