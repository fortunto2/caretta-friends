//
//  CameraCaptureView.swift
//  Caretta Friends — native iOS camera (iOS 16+, device-only)
//
//  Presented from the native SwiftUI shell when the user taps "+".
//  Flow: live preview / recent-photos strip / full gallery -> capture or pick
//        -> burn overlay (GPS + date + author) + write EXIF/GPS metadata
//        -> onDone(imagePath, lat, lng).
//
//  Frameworks: SwiftUI, AVFoundation, Photos, PhotosUI, CoreLocation,
//              ImageIO, UniformTypeIdentifiers, UIKit (all system, no deps).
//
//  NOTE: The camera does NOT work in the iOS Simulator (device-only). The view
//  degrades to a "camera unavailable" state; the gallery strip still works.
//

import SwiftUI
import AVFoundation
import Photos
import PhotosUI
import CoreLocation
import ImageIO
import UniformTypeIdentifiers
import UIKit
import ComposeApp

// MARK: - Public entry point

/// Full-screen native camera. Hand the result back via `onDone(imagePath, lat, lng)`.
/// `imagePath` is an absolute file path (JPEG in the app's Documents dir) with the
/// overlay burned in AND GPS/timestamp written into EXIF. `lat`/`lng` are nil when
/// no location was available.
struct CameraCaptureView: View {

    let author: String
    let onDone: (_ imagePath: String, _ lat: Double?, _ lng: Double?) -> Void
    let onCancel: () -> Void

    @StateObject private var camera = CameraController()
    @StateObject private var location = LocationProvider()
    @StateObject private var recents = RecentPhotosProvider()

    @State private var pickerItem: PhotosPickerItem?
    @State private var isProcessing = false

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if camera.isConfigured {
                CameraPreview(session: camera.session, isConfigured: camera.isConfigured)
                    .ignoresSafeArea()
            } else {
                unavailableState
            }

            VStack(spacing: 0) {
                topBar
                Spacer()
                bottomBar
            }
            .padding(.vertical, 8)

            if isProcessing {
                Color.black.opacity(0.55).ignoresSafeArea()
                ProgressView().tint(.white).scaleEffect(1.4)
            }
        }
        .preferredColorScheme(.dark)
        .onAppear {
            camera.onPhoto = { image in process(image, assetLocation: nil) }
            camera.start()
            location.start()
            recents.load()
        }
        .onDisappear { camera.stop() }
        .onChange(of: pickerItem) { item in loadFromPicker(item) }
    }

    // MARK: Sub-views

    private var topBar: some View {
        HStack {
            Button(action: onCancel) {
                Image(systemName: "xmark")
                    .font(.title3.weight(.semibold))
                    .foregroundColor(.white)
                    .padding(12)
                    .background(.ultraThinMaterial, in: Circle())
            }
            Spacer()
            Button { camera.toggleFlash() } label: {
                Image(systemName: camera.flashMode == .on ? "bolt.fill" : "bolt.slash.fill")
                    .font(.title3.weight(.semibold))
                    .foregroundColor(.white)
                    .padding(12)
                    .background(.ultraThinMaterial, in: Circle())
            }
        }
        .padding(.horizontal)
    }

    private var bottomBar: some View {
        VStack(spacing: 14) {
            galleryStrip
            HStack {
                // Full gallery (system picker)
                PhotosPicker(selection: $pickerItem, matching: .images, photoLibrary: .shared()) {
                    Image(systemName: "photo.on.rectangle.angled")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(width: 52, height: 52)
                        .background(.ultraThinMaterial, in: Circle())
                }
                Spacer()
                // Shutter
                Button { camera.capture() } label: {
                    ZStack {
                        Circle().strokeBorder(.white, lineWidth: 4).frame(width: 74, height: 74)
                        Circle().fill(.white).frame(width: 60, height: 60)
                    }
                }
                .disabled(!camera.isConfigured || isProcessing)
                .opacity(camera.isConfigured ? 1 : 0.4)
                Spacer()
                Color.clear.frame(width: 52, height: 52) // symmetry spacer
            }
            .padding(.horizontal, 24)
        }
    }

    private var galleryStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(recents.assets, id: \.localIdentifier) { asset in
                    RecentThumbnail(asset: asset, provider: recents)
                        .onTapGesture { pickRecent(asset) }
                }
            }
            .padding(.horizontal, 16)
        }
        .frame(height: 72)
    }

    private var unavailableState: some View {
        VStack(spacing: 12) {
            Image(systemName: "camera.fill").font(.system(size: 44)).foregroundColor(.white.opacity(0.6))
            Text(camera.statusMessage)
                .multilineTextAlignment(.center)
                .foregroundColor(.white.opacity(0.8))
                .padding(.horizontal, 40)
            if camera.permissionDenied {
                Button(IosEntryKt.currentStrings().openSettings) {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
                .buttonStyle(.borderedProminent)
            }
        }
    }

    // MARK: Result handling

    private func pickRecent(_ asset: PHAsset) {
        recents.fullImage(for: asset) { image, assetLocation in
            guard let image else { return }
            process(image, assetLocation: assetLocation)
        }
    }

    private func loadFromPicker(_ item: PhotosPickerItem?) {
        guard let item else { return }
        Task {
            guard
                let data = try? await item.loadTransferable(type: Data.self),
                let image = UIImage(data: data)
            else { return }
            // No PHAsset here -> use current device location for the burn.
            await MainActor.run { process(image, assetLocation: nil) }
        }
    }

    /// Burn overlay + write metadata off the main thread, then fire `onDone`.
    private func process(_ image: UIImage, assetLocation: CLLocation?) {
        guard !isProcessing else { return }
        isProcessing = true
        let author = self.author
        let loc = assetLocation ?? location.current
        let date = Date()
        DispatchQueue.global(qos: .userInitiated).async {
            let lines = OverlayFormatter.lines(author: author, date: date, location: loc)
            let burned = ImageMetadataWriter.burn(lines: lines, on: image)
            let url = ImageMetadataWriter.writeJPEG(burned, location: loc, author: author, date: date)
            DispatchQueue.main.async {
                isProcessing = false
                if let url {
                    onDone(url.path, loc?.coordinate.latitude, loc?.coordinate.longitude)
                }
            }
        }
    }
}

// MARK: - Camera controller (AVFoundation)

final class CameraController: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate {

    let session = AVCaptureSession()
    private let sessionQueue = DispatchQueue(label: "camera.session.queue")
    private let photoOutput = AVCapturePhotoOutput()

    @Published var isConfigured = false
    @Published var permissionDenied = false
    @Published var statusMessage = "Starting camera…"
    @Published var flashMode: AVCaptureDevice.FlashMode = .auto

    /// Delivered on the main thread.
    var onPhoto: ((UIImage) -> Void)?

    func start() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configure()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                if granted { self?.configure() }
                else { self?.fail("Camera access denied.", denied: true) }
            }
        default:
            fail("Camera access denied. Enable it in Settings.", denied: true)
        }
    }

    func stop() {
        sessionQueue.async { [weak self] in
            guard let self, self.session.isRunning else { return }
            self.session.stopRunning()
        }
    }

    func toggleFlash() {
        switch flashMode {
        case .auto: flashMode = .on
        case .on:   flashMode = .off
        default:    flashMode = .auto
        }
    }

    func capture() {
        sessionQueue.async { [weak self] in
            guard let self, self.session.isRunning else { return }
            let settings = AVCapturePhotoSettings()
            if self.photoOutput.supportedFlashModes.contains(self.flashMode) {
                settings.flashMode = self.flashMode
            }
            if let connection = self.photoOutput.connection(with: .video) {
                self.applyPortrait(to: connection)
            }
            self.photoOutput.capturePhoto(with: settings, delegate: self)
        }
    }

    // MARK: - Private

    private func configure() {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            self.session.beginConfiguration()
            self.session.sessionPreset = .photo

            guard
                let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
                let input = try? AVCaptureDeviceInput(device: device),
                self.session.canAddInput(input)
            else {
                self.session.commitConfiguration()
                self.fail("Camera unavailable (not supported in the Simulator).", denied: false)
                return
            }
            self.session.addInput(input)

            guard self.session.canAddOutput(self.photoOutput) else {
                self.session.commitConfiguration()
                self.fail("Could not attach the photo output.", denied: false)
                return
            }
            self.session.addOutput(self.photoOutput)
            self.session.commitConfiguration()
            self.session.startRunning()

            DispatchQueue.main.async { self.isConfigured = true }
        }
    }

    private func applyPortrait(to connection: AVCaptureConnection) {
        if #available(iOS 17.0, *) {
            if connection.isVideoRotationAngleSupported(90) { connection.videoRotationAngle = 90 }
        } else if connection.isVideoOrientationSupported {
            connection.videoOrientation = .portrait
        }
    }

    private func fail(_ message: String, denied: Bool) {
        DispatchQueue.main.async {
            self.statusMessage = message
            self.permissionDenied = denied
            self.isConfigured = false
        }
    }

    // AVCapturePhotoCaptureDelegate — called on a private queue.
    func photoOutput(_ output: AVCapturePhotoOutput,
                     didFinishProcessingPhoto photo: AVCapturePhoto,
                     error: Error?) {
        guard error == nil,
              let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data)
        else { return }
        DispatchQueue.main.async { self.onPhoto?(image) }
    }
}

// MARK: - Camera preview (AVCaptureVideoPreviewLayer)

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession
    let isConfigured: Bool // change forces updateUIView so we can set orientation once the connection exists

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.videoPreviewLayer.session = session
        view.videoPreviewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {
        guard let connection = uiView.videoPreviewLayer.connection else { return }
        if #available(iOS 17.0, *) {
            if connection.isVideoRotationAngleSupported(90) { connection.videoRotationAngle = 90 }
        } else if connection.isVideoOrientationSupported {
            connection.videoOrientation = .portrait
        }
    }

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var videoPreviewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }
}

// MARK: - Location

final class LocationProvider: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published var current: CLLocation?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func start() { manager.requestWhenInUseAuthorization() }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            manager.startUpdatingLocation()
        default:
            break
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let last = locations.last { current = last }
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) { /* keep last known */ }
}

// MARK: - Recent photos (gallery strip)

final class RecentPhotosProvider: NSObject, ObservableObject {
    @Published var assets: [PHAsset] = []
    private let imageManager = PHCachingImageManager()

    func load() {
        PHPhotoLibrary.requestAuthorization(for: .readWrite) { [weak self] status in
            guard status == .authorized || status == .limited else { return }
            let options = PHFetchOptions()
            options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
            options.fetchLimit = 30
            let result = PHAsset.fetchAssets(with: .image, options: options)
            var fetched: [PHAsset] = []
            result.enumerateObjects { asset, _, _ in fetched.append(asset) }
            DispatchQueue.main.async { self?.assets = fetched }
        }
    }

    func thumbnail(for asset: PHAsset, size: CGSize, completion: @escaping (UIImage?) -> Void) {
        let options = PHImageRequestOptions()
        options.deliveryMode = .opportunistic
        options.resizeMode = .fast
        options.isNetworkAccessAllowed = true
        imageManager.requestImage(for: asset,
                                  targetSize: size,
                                  contentMode: .aspectFill,
                                  options: options) { image, _ in completion(image) }
    }

    /// Full-resolution image + the asset's original CLLocation (may be nil).
    func fullImage(for asset: PHAsset, completion: @escaping (UIImage?, CLLocation?) -> Void) {
        let options = PHImageRequestOptions()
        options.deliveryMode = .highQualityFormat
        options.resizeMode = .exact
        options.isNetworkAccessAllowed = true
        imageManager.requestImage(for: asset,
                                  targetSize: PHImageManagerMaximumSize,
                                  contentMode: .default,
                                  options: options) { image, info in
            let degraded = (info?[PHImageResultIsDegradedKey] as? Bool) ?? false
            guard !degraded else { return } // ignore the temporary low-res callback
            completion(image, asset.location)
        }
    }
}

struct RecentThumbnail: View {
    let asset: PHAsset
    let provider: RecentPhotosProvider
    @State private var image: UIImage?

    var body: some View {
        ZStack {
            if let image {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Color.white.opacity(0.12)
            }
        }
        .frame(width: 60, height: 60)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .onAppear {
            provider.thumbnail(for: asset, size: CGSize(width: 120, height: 120)) { image = $0 }
        }
    }
}

// MARK: - Overlay text formatting

enum OverlayFormatter {
    static func lines(author: String, date: Date, location: CLLocation?) -> [String] {
        var out: [String] = []
        out.append("Caretta Friends • \(author)")
        out.append(dateFormatter.string(from: date))
        if let c = location?.coordinate {
            out.append(String(format: "GPS %.6f, %.6f", c.latitude, c.longitude))
        } else {
            out.append("GPS unavailable")
        }
        return out
    }

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm:ss ZZZZZ"
        return f
    }()
}

// MARK: - Overlay burning + EXIF/GPS writing (ImageIO)

enum ImageMetadataWriter {

    /// Draw `lines` as a bottom banner onto the image and return an upright (.up) UIImage.
    static func burn(lines: [String], on image: UIImage) -> UIImage {
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1 // work in pixel space; final JPEG has no misleading scale
        let size = CGSize(width: image.size.width * image.scale,
                          height: image.size.height * image.scale)
        let renderer = UIGraphicsImageRenderer(size: size, format: format)

        return renderer.image { ctx in
            image.draw(in: CGRect(origin: .zero, size: size)) // draws upright regardless of source orientation

            let fontSize = max(size.width * 0.028, 20)
            let font = UIFont.systemFont(ofSize: fontSize, weight: .semibold)
            let shadow = NSShadow()
            shadow.shadowColor = UIColor.black.withAlphaComponent(0.85)
            shadow.shadowBlurRadius = fontSize * 0.18
            shadow.shadowOffset = CGSize(width: 0, height: 1)
            let paragraph = NSMutableParagraphStyle()
            paragraph.lineSpacing = fontSize * 0.18
            let attributes: [NSAttributedString.Key: Any] = [
                .font: font,
                .foregroundColor: UIColor.white,
                .paragraphStyle: paragraph,
                .shadow: shadow
            ]

            let padding = fontSize * 0.7
            let maxTextWidth = size.width - padding * 2
            let text = lines.joined(separator: "\n") as NSString
            let bounds = text.boundingRect(
                with: CGSize(width: maxTextWidth, height: .greatestFiniteMagnitude),
                options: [.usesLineFragmentOrigin, .usesFontLeading],
                attributes: attributes, context: nil)

            let bannerHeight = ceil(bounds.height) + padding * 2
            let bannerRect = CGRect(x: 0, y: size.height - bannerHeight, width: size.width, height: bannerHeight)
            UIColor.black.withAlphaComponent(0.38).setFill()
            ctx.cgContext.fill(bannerRect)

            text.draw(with: CGRect(x: padding, y: bannerRect.minY + padding,
                                   width: maxTextWidth, height: ceil(bounds.height)),
                      options: [.usesLineFragmentOrigin, .usesFontLeading],
                      attributes: attributes, context: nil)
        }
    }

    /// Encode as JPEG in Documents with EXIF DateTimeOriginal, TIFF Artist, and full GPS dict.
    static func writeJPEG(_ image: UIImage, location: CLLocation?, author: String, date: Date) -> URL? {
        guard let cgImage = image.cgImage else { return nil }

        let dir = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let url = dir.appendingPathComponent("nest_\(UUID().uuidString).jpg")

        guard let destination = CGImageDestinationCreateWithURL(
            url as CFURL, UTType.jpeg.identifier as CFString, 1, nil
        ) else { return nil }

        let exifDate = exifFormatter.string(from: date)
        var properties: [CFString: Any] = [
            kCGImageDestinationLossyCompressionQuality: 0.9,
            kCGImagePropertyExifDictionary: [
                kCGImagePropertyExifDateTimeOriginal: exifDate,
                kCGImagePropertyExifDateTimeDigitized: exifDate
            ] as [CFString: Any],
            kCGImagePropertyTIFFDictionary: [
                kCGImagePropertyTIFFArtist: author,
                kCGImagePropertyTIFFDateTime: exifDate,
                kCGImagePropertyTIFFSoftware: "Caretta Friends"
            ] as [CFString: Any]
        ]

        if let location {
            properties[kCGImagePropertyGPSDictionary] = gpsDictionary(for: location)
        }

        CGImageDestinationAddImage(destination, cgImage, properties as CFDictionary)
        guard CGImageDestinationFinalize(destination) else { return nil }
        return url
    }

    // MARK: Helpers

    private static func gpsDictionary(for location: CLLocation) -> [CFString: Any] {
        let coord = location.coordinate
        var gps: [CFString: Any] = [
            kCGImagePropertyGPSLatitude: abs(coord.latitude),
            kCGImagePropertyGPSLatitudeRef: coord.latitude >= 0 ? "N" : "S",
            kCGImagePropertyGPSLongitude: abs(coord.longitude),
            kCGImagePropertyGPSLongitudeRef: coord.longitude >= 0 ? "E" : "W",
            kCGImagePropertyGPSTimeStamp: gpsTimeFormatter.string(from: location.timestamp),
            kCGImagePropertyGPSDateStamp: gpsDateFormatter.string(from: location.timestamp)
        ]
        if location.verticalAccuracy >= 0 {
            gps[kCGImagePropertyGPSAltitude] = abs(location.altitude)
            gps[kCGImagePropertyGPSAltitudeRef] = location.altitude < 0 ? 1 : 0
        }
        if location.horizontalAccuracy >= 0 {
            gps[kCGImagePropertyGPSHPositioningError] = location.horizontalAccuracy
        }
        return gps
    }

    private static let exifFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy:MM:dd HH:mm:ss"
        return f
    }()

    private static let gpsTimeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "HH:mm:ss.SS"
        return f
    }()

    private static let gpsDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = "yyyy:MM:dd"
        return f
    }()
}
