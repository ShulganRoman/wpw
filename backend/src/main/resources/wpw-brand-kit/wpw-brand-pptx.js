/**
 * WPW Creative Cutting Solutions — pptxgenjs Brand Module v1.0
 * Source: wpw-tools.com
 * Generated: 2026-03-14
 *
 * Usage:
 *   const wpw = require('./wpw-brand-pptx');
 *   const pres = wpw.newPresentation();
 *   const s = pres.addSlide();
 *   wpw.addLogo(s);
 *   wpw.darkFooter(s);
 *   wpw.sectionLabel(s, 'My Section', 0.35, 0.88);
 *   wpw.card(s, 1, 1, 4, 2, { shadow: true });
 */

const pptxgen   = require('pptxgenjs');
const React     = require('react');
const ReactDOM  = require('react-dom/server');
const sharp     = require('sharp');
const path      = require('path');

// ─────────────────────────────────────────────────────────────────────────────
// BRAND COLORS
// ─────────────────────────────────────────────────────────────────────────────
const B = {
  blue:      '68C7ED',   // Primary brand blue — logo, active nav, accents
  darkBlue:  '1A5F8A',   // Dark blue — headers, icon backgrounds
  navy:      '0D3D5E',   // Deep navy — dark slide backgrounds
  navyDeep:  '0A2B42',   // Deepest navy — footer bars, overlays
  white:     'FFFFFF',
  offWhite:  'F7F9FC',   // Light slide backgrounds
  lightBlue: 'E8F4FB',   // Tag/chip backgrounds on white
  lightGray: 'F3F3F3',
  midGray:   '9E9E9E',   // Footer text, captions
  gray:      '4A4A4A',   // Body text on light backgrounds
  darkText:  '1A1A2E',
  orange:    'E8820C',   // Accent
  border:    'E0ECF5',   // Default card border
};
exports.B = B;

// ─────────────────────────────────────────────────────────────────────────────
// TEXT PRESETS
// All font sizes are in points (pptxgenjs units)
// ─────────────────────────────────────────────────────────────────────────────
const T = {
  // On dark backgrounds
  h1Dark:    { fontSize: 34, bold: true,  color: B.white,    fontFace: 'Roboto', lineSpacingMultiple: 1.15 },
  h2Dark:    { fontSize: 26, bold: true,  color: B.white,    fontFace: 'Roboto' },
  h3Dark:    { fontSize: 18, bold: true,  color: B.white,    fontFace: 'Roboto' },
  bodyDark:  { fontSize: 13, bold: false, color: 'AACDE8',   fontFace: 'Roboto', lineSpacingMultiple: 1.45 },
  // On light backgrounds
  h1Light:   { fontSize: 34, bold: true,  color: B.darkBlue, fontFace: 'Roboto', lineSpacingMultiple: 1.15 },
  h2Light:   { fontSize: 26, bold: true,  color: B.darkBlue, fontFace: 'Roboto' },
  h3Light:   { fontSize: 18, bold: true,  color: B.darkBlue, fontFace: 'Roboto' },
  bodyLight: { fontSize: 13, bold: false, color: B.gray,     fontFace: 'Roboto', lineSpacingMultiple: 1.45 },
  // Utility
  caption:   { fontSize:  9, bold: false, color: B.midGray,  fontFace: 'Roboto' },
  label:     { fontSize: 8.5, bold: true, color: B.white,    fontFace: 'Roboto', align: 'center', valign: 'middle', charSpacing: 2 },
  cardTitle: { fontSize: 13, bold: true,  color: B.darkBlue, fontFace: 'Roboto' },
  cardBody:  { fontSize: 10.5, color: B.gray, fontFace: 'Roboto', lineSpacingMultiple: 1.35 },
};
exports.T = T;

// ─────────────────────────────────────────────────────────────────────────────
// LOGO
// ─────────────────────────────────────────────────────────────────────────────
const LOGO_PATH        = path.join(__dirname, 'wpw-logo.png');
const LOGO_ASPECT      = 180 / 624;   // height/width ratio
exports.LOGO_PATH      = LOGO_PATH;
exports.LOGO_ASPECT    = LOGO_ASPECT;

/**
 * Add WPW logo to a slide.
 * @param {object} slide  - pptxgenjs slide object
 * @param {number} x      - left position in inches (default 0.3)
 * @param {number} y      - top position in inches (default 0.18)
 * @param {number} w      - width in inches (default 1.3)
 */
function addLogo(slide, x = 0.3, y = 0.18, w = 1.3) {
  slide.addImage({ path: LOGO_PATH, x, y, w, h: w * LOGO_ASPECT });
}
exports.addLogo = addLogo;

// ─────────────────────────────────────────────────────────────────────────────
// DARK FOOTER
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Add standard dark footer bar to a slide.
 * @param {object} slide
 * @param {string} text  - footer text (optional)
 */
function darkFooter(slide, text = 'WPW Creative Cutting Solutions  |  wpw-tools.com') {
  slide.addShape('rect', { x: 0, y: 5.3, w: 10, h: 0.325, fill: { color: B.navy } });
  slide.addText(text, {
    x: 0.3, y: 5.31, w: 9.4, h: 0.28,
    fontSize: 9, color: B.midGray, fontFace: 'Roboto',
    align: 'left', valign: 'middle', margin: 0,
  });
}
exports.darkFooter = darkFooter;

// For dark-background slides (navy footer)
function deepFooter(slide, text = 'WPW Creative Cutting Solutions  |  wpw-tools.com') {
  slide.addShape('rect', { x: 0, y: 5.3, w: 10, h: 0.325, fill: { color: B.navyDeep } });
  slide.addText(text, {
    x: 0.3, y: 5.31, w: 9.4, h: 0.28,
    fontSize: 9, color: '4A6A80', fontFace: 'Roboto',
    align: 'left', valign: 'middle', margin: 0,
  });
}
exports.deepFooter = deepFooter;

// ─────────────────────────────────────────────────────────────────────────────
// SECTION LABEL (blue pill)
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Add a WPW section label badge.
 * @param {object} slide
 * @param {string} text   - label text (will be uppercased)
 * @param {number} x
 * @param {number} y
 * @param {number} w      - width in inches (default 2.2)
 */
function sectionLabel(slide, text, x, y, w = 2.2) {
  slide.addShape('rect', { x, y, w, h: 0.28, fill: { color: B.blue } });
  slide.addText(text.toUpperCase(), {
    x, y, w, h: 0.28,
    ...T.label,
    margin: 0,
  });
}
exports.sectionLabel = sectionLabel;

// ─────────────────────────────────────────────────────────────────────────────
// CARD
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Draw a card rectangle.
 * @param {object} slide
 * @param {number} x, y, w, h  - position and size in inches
 * @param {object} opts         - { fill, border, shadow, accentTop }
 *   opts.fill       - hex color (default white)
 *   opts.border     - hex color (default E0ECF5)
 *   opts.shadow     - boolean, adds drop shadow
 *   opts.accentTop  - hex color, adds thin colored top stripe
 *   opts.dark       - boolean, uses dark card preset
 */
function card(slide, x, y, w, h, opts = {}) {
  const fill   = opts.fill   || (opts.dark ? B.navyDeep : B.white);
  const border = opts.border || (opts.dark ? B.darkBlue : B.border);
  slide.addShape('rect', {
    x, y, w, h,
    fill: { color: fill },
    line: { color: border, width: 1 },
    shadow: opts.shadow
      ? { type: 'outer', color: '000000', blur: 8, offset: 2, angle: 135, opacity: 0.08 }
      : undefined,
  });
  if (opts.accentTop) {
    slide.addShape('rect', { x, y, w, h: 0.06, fill: { color: opts.accentTop } });
  }
}
exports.card = card;

// ─────────────────────────────────────────────────────────────────────────────
// ICON HELPER
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Render a react-icons icon as a PNG base64 data URI for pptxgenjs.
 * @param  {Function} IconComponent  - react-icons component
 * @param  {string}   color          - CSS color string (default '#FFFFFF')
 * @param  {number}   size           - SVG size in px (default 256)
 * @returns {Promise<string>}  base64 data URI suitable for slide.addImage({ data: ... })
 */
async function iconData(IconComponent, color = '#FFFFFF', size = 256) {
  const svg = ReactDOM.renderToStaticMarkup(
    React.createElement(IconComponent, { color, size: String(size) })
  );
  const buf = await sharp(Buffer.from(svg)).png().toBuffer();
  return 'image/png;base64,' + buf.toString('base64');
}
exports.iconData = iconData;

// ─────────────────────────────────────────────────────────────────────────────
// ICON BLOCK (icon + title + body on a slide)
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Add an icon block to a slide (icon box + title text + body text).
 * @param {object} slide
 * @param {object} opts
 *   opts.iconData   - base64 PNG from iconData()
 *   opts.title      - string
 *   opts.body       - string
 *   opts.x, opts.y  - position
 *   opts.w          - content width (default 3.5)
 *   opts.iconColor  - background color for icon box (default B.navy)
 *   opts.dark       - boolean, uses light text colours
 */
async function iconBlock(slide, opts) {
  const x = opts.x || 0;
  const y = opts.y || 0;
  const w = opts.w || 3.5;
  const iconBg  = opts.iconColor || B.navy;
  const titleC  = opts.dark ? B.white    : B.darkBlue;
  const bodyC   = opts.dark ? 'AACDE8'  : B.gray;

  // Icon square
  slide.addShape('rect', { x, y, w: 0.48, h: 0.48, fill: { color: iconBg } });
  if (opts.iconData) {
    slide.addImage({ data: opts.iconData, x: x + 0.04, y: y + 0.04, w: 0.4, h: 0.4 });
  }
  // Title
  slide.addText(opts.title || '', {
    x: x + 0.62, y, w: w - 0.7, h: 0.4,
    fontSize: 13, bold: true, color: titleC, fontFace: 'Roboto',
  });
  // Body
  if (opts.body) {
    slide.addText(opts.body, {
      x, y: y + 0.55, w, h: 0.7,
      fontSize: 10.5, color: bodyC, fontFace: 'Roboto', lineSpacingMultiple: 1.35,
    });
  }
}
exports.iconBlock = iconBlock;

// ─────────────────────────────────────────────────────────────────────────────
// STANDARD SLIDE LAYOUTS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Apply "light" slide base: off-white bg + navy top bar + logo + section label.
 */
function layoutLight(slide, sectionText) {
  slide.background = { color: B.offWhite };
  slide.addShape('rect', { x: 0, y: 0, w: 10, h: 0.85, fill: { color: B.navy } });
  addLogo(slide, 0.35, 0.12, 1.2);
  if (sectionText) sectionLabel(slide, sectionText, 7.5, 0.28, 2.05);
  darkFooter(slide);
}
exports.layoutLight = layoutLight;

/**
 * Apply "dark" slide base: navy bg + logo + section label.
 */
function layoutDark(slide, sectionText) {
  slide.background = { color: B.navy };
  addLogo(slide, 0.35, 0.2, 1.3);
  if (sectionText) sectionLabel(slide, sectionText, 0.35, 0.88, 1.85);
  deepFooter(slide);
}
exports.layoutDark = layoutDark;

/**
 * Apply "split" slide base: dark left panel + light right panel.
 * @param {number} splitX  - x position of the split (default 4.2)
 */
function layoutSplit(slide, splitX = 4.2) {
  slide.addShape('rect', { x: 0,      y: 0, w: splitX,      h: 5.625, fill: { color: B.navy } });
  slide.addShape('rect', { x: splitX - 0.11, y: 0, w: 0.22, h: 5.625, fill: { color: B.blue } });
  slide.addShape('rect', { x: splitX, y: 0, w: 10 - splitX, h: 5.625, fill: { color: B.offWhite } });
  addLogo(slide, 0.4, 0.35, 1.6);
  darkFooter(slide);
}
exports.layoutSplit = layoutSplit;

// ─────────────────────────────────────────────────────────────────────────────
// FACTORY: new presentation with WPW defaults
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Create a new pptxgenjs instance pre-configured for WPW.
 * @param {string} title  - presentation title
 */
function newPresentation(title = 'WPW Creative Cutting Solutions') {
  const pres = new pptxgen();
  pres.layout  = 'LAYOUT_16x9';
  pres.title   = title;
  pres.author  = 'WPW Creative Cutting Solutions';
  pres.company = 'WPW';
  return pres;
}
exports.newPresentation = newPresentation;
