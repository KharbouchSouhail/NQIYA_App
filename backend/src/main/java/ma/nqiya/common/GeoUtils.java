package ma.nqiya.common;

/**
 * Small geodesy helpers (great-circle distance, bounding boxes).
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtils() {
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /** Latitude degrees spanned by {@code km}. */
    public static double latDelta(double km) {
        return Math.toDegrees(km / EARTH_RADIUS_KM);
    }

    /** Longitude degrees spanned by {@code km} at the given latitude. */
    public static double lngDelta(double km, double atLat) {
        return Math.toDegrees(km / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(atLat))));
    }
}
