import SwiftUI
import UIKit
import RevenueCat

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
                .onAppear {
                    // Initialize RevenueCat with Apple API key
                    Purchases.configure(withAPIKey: "appl_cvHpElSxdfMMbxbzfRSXtdqPKym")
                    Purchases.shared.getOfferings { offerings, error in
                        if let error = error {
                            print("[RC] Offerings fetch error: \(error)")
                            return
                        }
                        if let current = offerings?.current {
                            print("[RC] Current offering loaded: \(current.identifier)")
                        } else {
                            print("[RC] No current offering configured")
                        }
                    }
                }
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