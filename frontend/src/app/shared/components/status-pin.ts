import { SpotStatus } from '../../core/models/models';

const GLYPHS: Record<SpotStatus, string> = {
  DIRTY:
    '<path d="M3 6h18M8 6V4h8v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M10 11v6M14 11v6"/>',
  CLEANING: '<path d="M20 3.5 12.6 11M9.3 11.3l3.6 3.6-2.6 6.1H4.2l.6-4.8zM7.2 21l1.5-3.6M4.6 16.6l3.6 1.4"/>',
  CLEAN: '<path d="M20 6 9 17l-5-5"/>',
};

const COLORS: Record<SpotStatus, string> = {
  DIRTY: '#e03a3e',
  CLEANING: '#f5b82e',
  CLEAN: '#1f8a46',
};

/** Marker HTML for Leaflet divIcons. Markers grow with the number of participants. */
export function pinHtml(status: SpotStatus, participants: number): { html: string; size: number } {
  const size = Math.round(38 + Math.min(participants, 20) * 1.4);
  const glyph = Math.round(size * 0.46);
  const html = `
    <div class="nq-pin" style="--pin:${COLORS[status]};--s:${size}px">
      <div class="nq-pin-head">
        <svg width="${glyph}" height="${glyph}" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.6"
             stroke-linecap="round" stroke-linejoin="round">${GLYPHS[status]}</svg>
      </div>
      ${participants > 1 ? `<span class="nq-pin-count">${participants}</span>` : ''}
    </div>`;
  return { html, size };
}
