import Foundation
import UIKit

#if canImport(DeviceActivity)
import DeviceActivity
#endif
#if canImport(ManagedSettings)
import ManagedSettings
#endif
#if canImport(FamilyControls)
import FamilyControls
#endif

/// Central place to manage Screen Time (App Lock) on iOS.
/// Uses FamilyControls to pick apps, DeviceActivity to schedule, and ManagedSettings to apply shields.
/// All calls are guarded so the app still compiles on platforms or SDKs that don't include these frameworks.
enum AppLockManager {

    /// Ask for authorization to use Screen Time APIs (Family Controls).
    static func requestAuthorizationIfNeeded(completion: @escaping (Bool) -> Void) {
        #if canImport(FamilyControls)
        if #available(iOS 16.0, *) {
            AuthorizationCenter.shared.requestAuthorization { result in
                switch result {
                case .success:
                    completion(true)
                case .failure:
                    completion(false)
                }
            }
            return
        }
        #endif
        completion(false)
    }

    /// Present Apple's system UI to choose which apps to lock.
    static func presentPicker(from presenter: UIViewController?) {
        #if canImport(FamilyControls)
        if #available(iOS 16.0, *) {
            let host = UIHostingController(rootView: FamilyPickerHostView())
            host.modalPresentationStyle = .formSheet
            (presenter ?? UIApplication.shared.keyWindowTopController())?.present(host, animated: true)
            return
        }
        #endif
        // Fallback: open app settings if Screen Time not available
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    /// Start a lock schedule until the provided "till" time. Time format: "HH:mm"
    static func startLock(from: String, till: String) {
        #if canImport(DeviceActivity) && canImport(ManagedSettings)
        if #available(iOS 16.0, *) {
            // Parse end time today
            let fmt = DateFormatter()
            fmt.dateFormat = "HH:mm"
            let now = Date()
            var endComponents = Calendar.current.dateComponents([.year, .month, .day], from: now)
            if let t = fmt.date(from: till) {
                let parts = Calendar.current.dateComponents([.hour, .minute], from: t)
                endComponents.hour = parts.hour
                endComponents.minute = parts.minute
            } else {
                endComponents.hour = Calendar.current.component(.hour, from: now) + 1
                endComponents.minute = 0
            }
            let endDate = Calendar.current.date(from: endComponents) ?? now.addingTimeInterval(3600)

            // Apply an immediate shield (simple mode). When the time elapses, remove shield.
            ShieldApplier.applyShield()
            let remaining = endDate.timeIntervalSinceNow
            if remaining > 0 {
                DispatchQueue.main.asyncAfter(deadline: .now() + remaining) {
                    ShieldApplier.removeShield()
                }
            }
            return
        }
        #endif
    }
}

#if canImport(FamilyControls)
import SwiftUI
@available(iOS 16.0, *)
fileprivate struct FamilyPickerHostView: View {
    @State private var selection = FamilyActivitySelection()

    var body: some View {
        FamilyActivityPicker(selection: $selection)
            .onChange(of: selection) { _, newValue in
                // Persist selection token for future use if needed
                if let data = try? JSONEncoder().encode(newValue) {
                    UserDefaults.standard.set(data, forKey: "AppLockSelection")
                }
            }
            .navigationTitle("Choose Apps to Lock")
            .presentationDetents([.medium, .large])
    }
}
#endif

#if canImport(ManagedSettings)
@available(iOS 16.0, *)
fileprivate enum ShieldApplier {
    static let store = ManagedSettingsStore()

    static func applyShield() {
        // If you have a FamilyActivitySelection saved, you can target specific apps.
        // For now, apply a broad shield policy except our app.
        store.shield.applicationCategories = .all()
        store.shield.webDomainCategories = .all()
        // Do not specify individual applications here to avoid API mismatches.
        store.shield.applications = nil
    }

    static func removeShield() {
        store.shield.applicationCategories = nil
        store.shield.webDomainCategories = nil
        store.shield.applications = nil
    }
}
#endif

private extension UIApplication {
    func keyWindowTopController() -> UIViewController? {
        return connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first?
            .rootViewController?
            .topMostPresented()
    }
}

private extension UIViewController {
    func topMostPresented() -> UIViewController {
        var top: UIViewController = self
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}


