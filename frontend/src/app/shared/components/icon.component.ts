import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** circle as a path so every icon is a list of `d` strings */
const c = (cx: number, cy: number, r: number) => `M${cx - r} ${cy}a${r} ${r} 0 1 0 ${2 * r} 0a${r} ${r} 0 1 0 ${-2 * r} 0`;

const ICONS = {
  map: ['M9 3 3 5.5v15L9 18l6 3 6-2.5v-15L15 6 9 3z', 'M9 3v15', 'M15 6v15'],
  trophy: [
    'M6 9H4.5a2.5 2.5 0 0 1 0-5H6',
    'M18 9h1.5a2.5 2.5 0 0 0 0-5H18',
    'M4 22h16',
    'M10 14.7V17c0 .6-.5 1-1 1.2C7.9 18.8 7 20.2 7 22',
    'M14 14.7V17c0 .6.5 1 1 1.2 1.1.6 2 2 2 3.8',
    'M18 2H6v7a6 6 0 0 0 12 0V2z',
  ],
  plus: ['M12 5v14', 'M5 12h14'],
  store: ['M3 9l1.5-5h15L21 9', 'M3 9h18v1.5a3 3 0 0 1-6 0 3 3 0 0 1-6 0 3 3 0 0 1-6 0z', 'M5 13v8h14v-8', 'M10 21v-5h4v5'],
  user: [c(12, 8, 4), 'M4 21a8 8 0 0 1 16 0'],
  users: ['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', c(9, 7, 4), 'M22 21v-2a4 4 0 0 0-3-3.9', 'M16 3.1a4 4 0 0 1 0 7.8'],
  trash: ['M3 6h18', 'M8 6V4h8v2', 'M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6', 'M10 11v6', 'M14 11v6'],
  broom: ['M20 3.5 12.6 11', 'M9.3 11.3l3.6 3.6-2.6 6.1H4.2l.6-4.8z', 'M7.2 21l1.5-3.6', 'M4.6 16.6l3.6 1.4'],
  check: ['M20 6 9 17l-5-5'],
  leaf: [
    'M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19 2c1 2 2 4.2 2 8 0 5.5-4.8 10-10 10z',
    'M2 21c0-3 1.9-5.4 5.1-6C9.5 14.5 12 13 13 12',
  ],
  sprout: [
    'M7 20h10',
    'M12 20v-9',
    'M12 13c-4.4 0-7-2.6-7-7 4.4 0 7 2.6 7 7z',
    'M12 11c0-4.4 2.6-7 7-7 0 4.4-2.6 7-7 7z',
  ],
  locate: [c(12, 12, 3), c(12, 12, 7.5), 'M12 2v2.5', 'M12 19.5V22', 'M2 12h2.5', 'M19.5 12H22'],
  login: ['M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4', 'M10 17l5-5-5-5', 'M15 12H3'],
  logout: ['M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'M16 17l5-5-5-5', 'M21 12H9'],
  camera: ['M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3z', c(12, 13, 3.5)],
  qr: ['M3 3h7v7H3z', 'M14 3h7v7h-7z', 'M3 14h7v7H3z', 'M14 14h3v3h-3z', 'M20 14v.01', 'M14 20h.01', 'M17 17h4v4h-4z'],
  scan: ['M3 7V5a2 2 0 0 1 2-2h2', 'M17 3h2a2 2 0 0 1 2 2v2', 'M21 17v2a2 2 0 0 1-2 2h-2', 'M7 21H5a2 2 0 0 1-2-2v-2', 'M7 12h10'],
  clock: [c(12, 12, 10), 'M12 6v6l4 2'],
  pin: ['M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0z', c(12, 10, 3)],
  coins: [c(8, 8, 6), 'M18.1 10.4A6 6 0 1 1 10.3 18', 'M7 6h1v4', 'M16.7 13.9l.7.7-2.8 2.8'],
  x: ['M18 6 6 18', 'M6 6l12 12'],
  'chevron-left': ['M15 18l-6-6 6-6'],
  'chevron-right': ['M9 18l6-6-6-6'],
  sparkles: ['M12 3l1.9 5.8L20 11l-6.1 2.2L12 19l-1.9-5.8L4 11l6.1-2.2z', 'M19 3v4', 'M21 5h-4'],
  shield: ['M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z'],
  lock: ['M5 11h14v10H5z', 'M7 11V7a5 5 0 0 1 10 0v4'],
  ticket: [
    'M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z',
    'M13 5v2',
    'M13 17v2',
    'M13 11v2',
  ],
  history: ['M3 12a9 9 0 1 0 9-9 9.8 9.8 0 0 0-6.7 2.7L3 8', 'M3 3v5h5', 'M12 7v5l4 2'],
  edit: ['M12 20h9', 'M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z'],
  play: ['M6 3l14 9-14 9z'],
  alert: [c(12, 12, 10), 'M12 8v4', 'M12 16h.01'],
  refresh: ['M21 12a9 9 0 1 1-3-6.7L21 8', 'M21 3v5h-5'],
  gift: ['M20 12v10H4V12', 'M2 7h20v5H2z', 'M12 22V7', 'M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z', 'M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z'],
  image: ['M3 3h18v18H3z', c(9, 9, 2), 'M21 15l-5-5L5 21'],
  eye_off: ['M3 3l18 18', 'M10.6 10.6a2 2 0 0 0 2.8 2.8', 'M9.9 5.1A10 10 0 0 1 12 5c7 0 10 7 10 7a13 13 0 0 1-1.7 2.7', 'M6.6 6.6A13.5 13.5 0 0 0 2 12s3 7 10 7a9.7 9.7 0 0 0 5.4-1.6'],
  arrow_right: ['M5 12h14', 'M12 5l7 7-7 7'],
  star: ['M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z'],
} as const;

export type IconName = keyof typeof ICONS;

@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      [attr.stroke-width]="stroke()"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      @for (d of paths(); track $index) {
        <path [attr.d]="d" />
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
    }
  `,
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input(22);
  readonly stroke = input(2);
  protected readonly paths = computed(() => ICONS[this.name()]);
}
