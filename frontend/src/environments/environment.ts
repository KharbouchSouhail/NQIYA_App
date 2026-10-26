export const environment = {
  /** The SPA and the API share one origin (dev proxy / nginx), so the API base is relative. */
  apiUrl: '/api',
  /** Default map centre when geolocation is unavailable: Mohammedia. */
  defaultCenter: { lat: 33.6861, lng: -7.3829 },
  /** Raster tiles. OSM's public server is fine for development; use your own tile provider in production. */
  mapTilesUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
};
