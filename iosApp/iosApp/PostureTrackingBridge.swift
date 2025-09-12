import Foundation
import UIKit

@objc class PostureTrackingBridge: NSObject {
    private var cameraViewController: CameraViewController?
    private var navigationController: UINavigationController?
    private var presentingViewController: UIViewController?
    
    @objc func startPostureTracking(presentingFrom viewController: UIViewController) {
        let cameraVC = CameraViewController()
        self.cameraViewController = cameraVC
        
        let navController = UINavigationController(rootViewController: cameraVC)
        navController.navigationBar.isHidden = true
        navController.modalPresentationStyle = .fullScreen
        self.navigationController = navController
        self.presentingViewController = viewController
        
        viewController.present(navController, animated: true, completion: nil)
    }
    
    @objc func stopPostureTracking() {
        cameraViewController?.dismiss(animated: true, completion: nil)
        cameraViewController = nil
        navigationController = nil
        presentingViewController = nil
    }
    
    @objc func getCurrentPoseData() -> [String: Any] {
        // This method can be called from Compose to get current pose data
        // For now, return empty data - we'll implement this later
        return [:]
    }
    
    // Static method to get the shared instance
    @objc static func shared() -> PostureTrackingBridge {
        return PostureTrackingBridge()
    }
} 