import { Routes } from '@angular/router';

import { adminGuard, authGuard } from './core/guards/guards';

/** Every feature is lazy-loaded so the first paint only ships the map. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'map' },
  {
    path: 'map',
    title: 'NQIYA · Map',
    loadComponent: () => import('./features/map/map.page').then((m) => m.MapPage),
  },
  {
    path: 'login',
    title: 'NQIYA · Log in',
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'report',
    title: 'NQIYA · Spot a mess',
    canActivate: [authGuard],
    loadComponent: () => import('./features/report/report.page').then((m) => m.ReportPage),
  },
  {
    path: 'spots/:id',
    title: 'NQIYA · Cleanup',
    loadComponent: () => import('./features/spot/spot-detail.page').then((m) => m.SpotDetailPage),
  },
  {
    path: 'leaderboard',
    title: 'NQIYA · Leaderboard',
    loadComponent: () => import('./features/leaderboard/leaderboard.page').then((m) => m.LeaderboardPage),
  },
  {
    path: 'shop',
    title: 'NQIYA · Shop',
    loadComponent: () => import('./features/shop/shop.page').then((m) => m.ShopPage),
  },
  {
    path: 'profile',
    title: 'NQIYA · Profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile.page').then((m) => m.ProfilePage),
  },
  {
    path: 'admin',
    title: 'NQIYA · Admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./features/admin/admin.page').then((m) => m.AdminPage),
  },
  { path: '**', redirectTo: 'map' },
];
