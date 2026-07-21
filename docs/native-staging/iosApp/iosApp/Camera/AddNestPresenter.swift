import SwiftUI

/// Drop this button (or wire its `isPresented` toggle) into the SwiftUI TabView/NavigationStack
/// shell where the '+' action lives.
struct AddNestButton: View {
    /// Current signed-in ranger/author label burned into the photo.
    let author: String
    /// Bridge into the shared KMP layer / add-nest form.
    let onPhotoReady: (_ imagePath: String, _ lat: Double?, _ lng: Double?) -> Void

    @State private var showCamera = false

    var body: some View {
        Button {
            showCamera = true
        } label: {
            Image(systemName: "plus")
                .font(.title2.weight(.semibold))
        }
        .fullScreenCover(isPresented: $showCamera) {
            CameraCaptureView(
                author: author,
                onDone: { imagePath, lat, lng in
                    showCamera = false
                    // e.g. push the shared Compose "add nest" screen via ComposeUIViewController,
                    // or set draft state consumed by the shell's NavigationStack.
                    onPhotoReady(imagePath, lat, lng)
                },
                onCancel: {
                    showCamera = false
                }
            )
        }
    }
}
