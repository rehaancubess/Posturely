import SwiftUI
import UIKit

@main
struct iOSApp: App {
    // Initialize the notification observer
    @StateObject private var notificationObserver = NotificationObserverWrapper()
    
    // Initialize AirPods detection service
    private let airPodsService = AirPodsDetectionService.shared
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear { UIApplication.shared.isIdleTimerDisabled = true }
                .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
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