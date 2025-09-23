import UIKit
import SwiftUI
import ComposeApp
import RevenueCatUI
import RevenueCat

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

// Simple helper to present the RevenueCat dynamic paywall when needed from SwiftUI
// You can call PaywallPresenter.presentCurrentOffering() from any SwiftUI action.
enum PaywallPresenter {
    static func presentCurrentOffering(from presenter: UIViewController? = nil) {
        let presentingVC: UIViewController
        if let p = presenter {
            presentingVC = p
        } else {
            presentingVC = UIApplication.shared.connectedScenes
                .compactMap { ($0 as? UIWindowScene)?.keyWindow }
                .first?.rootViewController ?? UIViewController()
        }
        let paywall = RevenueCatUI.PaywallViewController()
        presentingVC.present(paywall, animated: true, completion: nil)
    }

    static func present(offering: Offering, from presenter: UIViewController? = nil) {
        let presentingVC: UIViewController
        if let p = presenter {
            presentingVC = p
        } else {
            presentingVC = UIApplication.shared.connectedScenes
                .compactMap { ($0 as? UIWindowScene)?.keyWindow }
                .first?.rootViewController ?? UIViewController()
        }
        let paywall = RevenueCatUI.PaywallViewController(offering: offering)
        presentingVC.present(paywall, animated: true, completion: nil)
    }
}



