import SwiftUI

@main
struct iOSApp: App {
    // Initialize the notification observer
    @StateObject private var notificationObserver = NotificationObserverWrapper()
    
    // Initialize AirPods detection service
    private let airPodsService = AirPodsDetectionService.shared
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// Wrapper class to manage the notification observer lifecycle
class NotificationObserverWrapper: ObservableObject {
    private var observer: NotificationObserver?
    
    init() {
        observer = NotificationObserver()
    }
    
    deinit {
        observer = nil
    }
}