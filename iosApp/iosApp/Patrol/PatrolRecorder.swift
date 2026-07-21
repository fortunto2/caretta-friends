import Foundation
import CoreLocation
import Combine

/// Records a GPS breadcrumb track (Strava-style) while patrolling.
/// SAFETY: location stays on-device — nothing is shared live. The finished track is saved (and
/// only then optionally published) on `stop()`; the app never broadcasts the live position.
final class PatrolRecorder: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var isRecording = false
    @Published var meters: Double = 0
    @Published var seconds: Int = 0
    @Published var coords: [CLLocationCoordinate2D] = []

    private let manager = CLLocationManager()
    private var timer: Timer?
    private var lastLoc: CLLocation?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 5          // metres between breadcrumb updates
        manager.activityType = .fitness
    }

    func start() {
        guard !isRecording else { return }
        manager.requestWhenInUseAuthorization()
        coords = []; meters = 0; seconds = 0; lastLoc = nil
        isRecording = true
        manager.startUpdatingLocation()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            self?.seconds += 1
        }
    }

    /// Stops recording and returns the track as "lat,lng;lat,lng;…" plus rounded metres and seconds.
    @discardableResult
    func stop() -> (trackCsv: String, meters: Int, seconds: Int) {
        isRecording = false
        manager.stopUpdatingLocation()
        timer?.invalidate(); timer = nil
        let csv = coords.map { "\($0.latitude),\($0.longitude)" }.joined(separator: ";")
        return (csv, Int(meters.rounded()), seconds)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard isRecording else { return }
        for loc in locations {
            if let last = lastLoc { meters += loc.distance(from: last) }
            lastLoc = loc
            coords.append(loc.coordinate)
        }
    }

    var timeLabel: String {
        let m = seconds / 60, s = seconds % 60
        return String(format: "%d:%02d", m, s)
    }
    var kmLabel: String { String(format: "%.2f km", meters / 1000) }
}
