import { Rank, SpotStatus } from './models';

export interface RankStyle {
  label: string;
  color: string;
  glow: string;
}

export const RANK_STYLES: Record<Rank, RankStyle> = {
  BRONZE: { label: 'Bronze', color: '#b5733a', glow: 'rgba(181, 115, 58, .35)' },
  SILVER: { label: 'Silver', color: '#8e9aa6', glow: 'rgba(142, 154, 166, .35)' },
  GOLD: { label: 'Gold', color: '#e0a526', glow: 'rgba(224, 165, 38, .4)' },
  PLATINUM: { label: 'Platinum', color: '#3aa7a0', glow: 'rgba(58, 167, 160, .4)' },
  DIAMOND: { label: 'Diamond', color: '#4f7cf0', glow: 'rgba(79, 124, 240, .45)' },
};

export interface StatusStyle {
  label: string;
  icon: 'trash' | 'broom' | 'check';
  cssVar: string;
}

export const STATUS_STYLES: Record<SpotStatus, StatusStyle> = {
  DIRTY: { label: 'Dirty', icon: 'trash', cssVar: 'var(--dirty)' },
  CLEANING: { label: 'Cleaning', icon: 'broom', cssVar: 'var(--cleaning)' },
  CLEAN: { label: 'Clean', icon: 'check', cssVar: 'var(--clean)' },
};
