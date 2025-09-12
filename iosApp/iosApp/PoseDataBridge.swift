import Foundation

// Global variables for easy access from Kotlin
var globalLandmarkCount: Int = 0
var globalLandmarkX: [Float] = Array(repeating: 0.5, count: 33)
var globalLandmarkY: [Float] = Array(repeating: 0.5, count: 33)

@objc class PoseDataBridge: NSObject {
    @objc static func getCurrentPoseData() -> String {
        return PoseDataManager.shared.currentPoseData
    }
    
    @objc static func getLandmarkCount() -> Int {
        return globalLandmarkCount
    }
    
    @objc static func getLandmarkX(_ index: Int) -> Float {
        guard index >= 0 && index < 33 else { return 0.5 }
        return globalLandmarkX[index]
    }
    
    @objc static func getLandmarkY(_ index: Int) -> Float {
        guard index >= 0 && index < 33 else { return 0.5 }
        return globalLandmarkY[index]
    }
    
    @objc static func getLandmarkZ(_ index: Int) -> Float {
        guard index < PoseDataManager.shared.currentLandmarks.count else { return 0.0 }
        return PoseDataManager.shared.currentLandmarks[index].z
    }
    
    @objc static func getLandmarkVisibility(_ index: Int) -> Float {
        guard index < PoseDataManager.shared.currentLandmarks.count else { return 0.0 }
        return PoseDataManager.shared.currentLandmarks[index].visibility
    }
    
    @objc static func getLandmarkPresence(_ index: Int) -> Float {
        guard index < PoseDataManager.shared.currentLandmarks.count else { return 0.0 }
        return PoseDataManager.shared.currentLandmarks[index].presence
    }
    
    static func updateGlobalLandmarks(_ landmarks: [PoseLandmark]) {
        globalLandmarkCount = landmarks.count
        for i in 0..<min(landmarks.count, 33) {
            globalLandmarkX[i] = landmarks[i].x
            globalLandmarkY[i] = landmarks[i].y
        }

    }
} 