import { Injectable } from '@angular/core';

export interface GeoFix {
  lat: number;
  lng: number;
  accuracy: number;
}

@Injectable({ providedIn: 'root' })
export class GeolocationService {
  /** A fresh, high-accuracy GPS fix (no cached position). */
  current(timeoutMs = 15000): Promise<GeoFix> {
    return new Promise((resolve, reject) => {
      if (!('geolocation' in navigator)) {
        reject(new Error('Location is not available on this device'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude, accuracy: pos.coords.accuracy }),
        (err) =>
          reject(
            new Error(
              err.code === err.PERMISSION_DENIED
                ? 'Allow location access to pin the spot'
                : 'Could not get your position, try again outside',
            ),
          ),
        { enableHighAccuracy: true, maximumAge: 0, timeout: timeoutMs },
      );
    });
  }
}
