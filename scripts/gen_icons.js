// Regenerate the legacy launcher PNGs for the 太极 icon.
// Run from anywhere: `node scripts/gen_icons.js`
// Design mirrors res/drawable/ic_launcher_foreground.xml + _background.xml.
const fs = require('fs');
const zlib = require('zlib');
const path = require('path');

const RES = path.resolve(__dirname, '..', 'app', 'src', 'main', 'res');
const BUCKETS = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };

const C_TOP = [0xC6, 0x3D, 0x2F];   // 朱砂中心
const C_EDGE = [0x6E, 0x0A, 0x0A];  // 朱砂边
const C_WHITE = [0xF5, 0xF0, 0xE6]; // 阳(米白)
const C_BLACK = [0x1A, 0x1A, 0x1A]; // 阴(墨)

const lerp = (a, b, t) => a + (b - a) * t;
const mix = (c1, c2, t) => [lerp(c1[0], c2[0], t), lerp(c1[1], c2[1], t), lerp(c1[2], c2[2], t)];

function render(S, ss = 3) {
  const W = S * ss, H = S * ss;
  const cx = W / 2, cy = H / 2;
  const R = W * 0.40, bulbR = R / 2;
  const Uc = [cx, cy - bulbR], Lc = [cx, cy + bulbR];
  const eyeR = R * 0.19, bgR = W * 0.72;
  const raw = Buffer.alloc(W * H * 4);
  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      const px = x + 0.5, py = y + 0.5;
      const dC = Math.hypot(px - cx, py - cy);
      let col;
      if (dC <= R) {
        const inU = Math.hypot(px - Uc[0], py - Uc[1]) <= bulbR;
        const inL = Math.hypot(px - Lc[0], py - Lc[1]) <= bulbR;
        let reg = inU ? 'white' : inL ? 'black' : (px > cx ? 'black' : 'white');
        if (Math.hypot(px - Uc[0], py - Uc[1]) <= eyeR) reg = 'black'; // 阳中阴
        if (Math.hypot(px - Lc[0], py - Lc[1]) <= eyeR) reg = 'white'; // 阴中阳
        col = reg === 'white' ? C_WHITE : C_BLACK;
      } else {
        const t = Math.min(1, dC / bgR);
        col = mix(C_TOP, C_EDGE, t * t);
      }
      const i = (y * W + x) * 4;
      raw[i] = col[0]; raw[i + 1] = col[1]; raw[i + 2] = col[2]; raw[i + 3] = 255;
    }
  }
  const out = Buffer.alloc(S * S * 4);
  for (let y = 0; y < S; y++)
    for (let x = 0; x < S; x++) {
      let r = 0, g = 0, b = 0;
      for (let dy = 0; dy < ss; dy++)
        for (let dx = 0; dx < ss; dx++) {
          const i = ((y * ss + dy) * W + (x * ss + dx)) * 4;
          r += raw[i]; g += raw[i + 1]; b += raw[i + 2];
        }
      const n = ss * ss, j = (y * S + x) * 4;
      out[j] = r / n; out[j + 1] = g / n; out[j + 2] = b / n; out[j + 3] = 255;
    }
  return out;
}

function writePNG(file, S, rgba) {
  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(S, 0); ihdr.writeUInt32BE(S, 4);
  ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length, 0);
    const t = Buffer.from(type, 'ascii');
    const crc = Buffer.alloc(4); crc.writeUInt32BE(zlib.crc32(Buffer.concat([t, data])), 0);
    return Buffer.concat([len, t, data, crc]);
  };
  const raw = Buffer.alloc((S * 4 + 1) * S);
  for (let y = 0; y < S; y++) {
    raw[y * (S * 4 + 1)] = 0;
    rgba.copy(raw, y * (S * 4 + 1) + 1, y * S * 4, (y + 1) * S * 4);
  }
  fs.writeFileSync(file, Buffer.concat([
    sig, chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0)),
  ]));
}

for (const [bucket, S] of Object.entries(BUCKETS)) {
  const dir = path.join(RES, `mipmap-${bucket}`);
  const img = render(S);
  for (const name of ['ic_launcher.png', 'ic_launcher_round.png']) {
    writePNG(path.join(dir, name), S, img);
    const webp = path.join(dir, name.replace('.png', '.webp'));
    if (fs.existsSync(webp)) fs.unlinkSync(webp);
  }
  console.log(`wrote ${bucket} (${S}px)`);
}
console.log('done');
