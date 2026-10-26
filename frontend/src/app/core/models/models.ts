/** API contracts — mirror the Spring Boot DTOs. */

export type Rank = 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND';
export type Role = 'USER' | 'ADMIN';
export type SpotStatus = 'DIRTY' | 'CLEANING' | 'CLEAN';
export type Visibility = 'PUBLIC' | 'PRIVATE';
export type DirtLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface RankProgress {
  current: Rank;
  next: Rank | null;
  currentMinXp: number;
  nextMinXp: number | null;
  percent: number;
}

export interface Me {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  xp: number;
  coins: number;
  rank: RankProgress;
  ticketCode: string;
  completedCleanups: number;
  createdAt: string;
}

export interface PublicUser {
  id: number;
  displayName: string;
  rank: Rank;
  xp: number;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  newUser: boolean;
  user: Me;
}

export interface RequestCodeResponse {
  sent: boolean;
  devCode: string | null;
}

export interface SpotSummary {
  id: number;
  title: string;
  status: SpotStatus;
  visibility: Visibility;
  latitude: number;
  longitude: number;
  participantCount: number;
  meetingAt: string | null;
  dirtLevel: DirtLevel | null;
  beforePhotoUrl: string;
  distanceKm: number | null;
}

export interface ViewerState {
  authenticated: boolean;
  organizer: boolean;
  joined: boolean;
  checkedIn: boolean;
  canJoin: boolean;
}

export interface SpotDetail {
  id: number;
  title: string;
  description: string | null;
  status: SpotStatus;
  visibility: Visibility;
  anonymous: boolean;
  closed: boolean;
  latitude: number;
  longitude: number;
  meetingAt: string | null;
  organizer: PublicUser | null;
  beforePhotoUrl: string;
  afterPhotoUrl: string | null;
  dirtLevel: DirtLevel | null;
  wasteType: string | null;
  participantCount: number;
  checkedInCount: number;
  createdAt: string;
  startedAt: string | null;
  cleanedAt: string | null;
  viewer: ViewerState;
}

export interface ReportDraft {
  photoId: string | null;
  accepted: boolean;
  title: string | null;
  description: string | null;
  dirtLevel: DirtLevel | null;
  wasteType: string | null;
  reason: string;
}

export interface CreateSpotRequest {
  photoId: string;
  title: string;
  description: string | null;
  latitude: number;
  longitude: number;
  accuracyMeters: number;
  visibility: Visibility;
  anonymous: boolean;
  meetingAt: string | null;
}

export interface CheckInResult {
  volunteer: PublicUser;
  checkedInAt: string;
  alreadyCheckedIn: boolean;
  spotStatus: SpotStatus;
  checkedInCount: number;
}

export interface Participant {
  user: PublicUser;
  joinedAt: string;
  checkedInAt: string | null;
  organizer: boolean;
}

export interface RewardSummary {
  userId: number;
  xpGained: number;
  coinsGained: number;
  giftCoins: number;
  rankBefore: Rank;
  rankAfter: Rank;
  totalXp: number;
  totalCoins: number;
}

export interface FinishResult {
  passed: boolean;
  reason: string;
  spot: SpotDetail;
  myReward: RewardSummary | null;
  paidVolunteers: number;
}

export interface MyCleanup {
  spotId: number;
  title: string;
  status: SpotStatus;
  organizer: boolean;
  checkedIn: boolean;
  joinedAt: string;
  meetingAt: string | null;
  beforePhotoUrl: string;
}

export type RewardType = 'CLEANUP' | 'ORGANIZER_BONUS' | 'RANK_UP_GIFT' | 'PURCHASE';

export interface HistoryItem {
  id: number;
  type: RewardType;
  xpDelta: number;
  coinsDelta: number;
  spotId: number | null;
  description: string;
  createdAt: string;
}

export interface LeaderboardEntry {
  position: number;
  userId: number;
  displayName: string;
  rank: Rank;
  xp: number;
}

export interface Leaderboard {
  top: LeaderboardEntry[];
  me: LeaderboardEntry | null;
}

export interface Voucher {
  id: number;
  partnerName: string;
  title: string;
  description: string;
  category: string;
  costCoins: number;
  city: string;
}

export interface WalletItem {
  id: number;
  voucher: Voucher;
  code: string;
  status: 'ACTIVE' | 'USED';
  costCoins: number;
  purchasedAt: string;
  usedAt: string | null;
}

export interface PurchaseResult {
  item: WalletItem;
  coinsLeft: number;
}

export interface RankInfo {
  rank: Rank;
  minXp: number;
  xpMultiplier: number;
  coinMultiplier: number;
  levelUpGift: number;
  tagline: string;
}

export interface PublicConfig {
  ranks: RankInfo[];
  rewards: { cleanupXp: number; cleanupCoins: number; organizerXp: number; organizerCoins: number };
  maxRadiusKm: number;
  maxGpsAccuracyMeters: number;
  aiProvider: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface AdminStats {
  users: number;
  dirty: number;
  cleaning: number;
  clean: number;
}

export interface AdminSpot {
  id: number;
  title: string;
  status: SpotStatus;
  visibility: Visibility;
  closed: boolean;
  organizerEmail: string;
  participantCount: number;
  createdAt: string;
}

export interface AdminUser {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  rank: Rank;
  xp: number;
  coins: number;
  createdAt: string;
}

export interface ActionLog {
  id: number;
  adminEmail: string;
  action: string;
  targetType: string;
  targetId: number;
  details: string | null;
  createdAt: string;
}

/** RFC 9457 problem detail returned by the API. */
export interface ApiProblem {
  status: number;
  detail?: string;
  code?: string;
  errors?: Record<string, string>;
}
