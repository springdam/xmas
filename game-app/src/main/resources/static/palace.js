// Palace art: a 64x40 pixel throne room drawn from six parts, each at tier 0 (bare) .. 4 (gold).
// Pure trophy; see PalaceService on the server. palaceSvg(palace) returns an <svg> string.

const PALACE_MATERIALS = ["bare", "wood", "stone", "marble", "gold"];
const PALACE_TIER_NAMES = ["Bare", "Wooden", "Stone", "Marble", "Golden"];
const PALACE_PARTS = [
  ["walls", "WALLS"], ["floor", "FLOOR"], ["throne", "THRONE"],
  ["pillars", "PILLARS"], ["windows", "WINDOWS"], ["treasures", "TREASURES"],
];

// dark, mid, light per material
const PAL = {
  bare: ["#211d27", "#2f2a37", "#423b4c"],
  wood: ["#4f3016", "#7a4e24", "#a8743c"],
  stone: ["#4e4e58", "#7c7c88", "#a8a8b4"],
  marble: ["#a7a094", "#d9d4c8", "#f6f3ec"],
  gold: ["#8a5d00", "#d4a000", "#ffe066"],
};
const RED = ["#6a0d1c", "#b3192f", "#e0445a"];
const GLASS = ["#c0303a", "#2e62c8", "#e6b422", "#3a9a48"];

function palaceSvg(palace) {
  const out = [];
  const r = (x, y, w, h, c) => out.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${c}"/>`);
  const t = (part) => Math.max(0, Math.min(4, palace?.[part] ?? 0));

  drawWalls(r, t("walls"));
  drawWindows(r, t("windows"));
  drawFloor(r, t("floor"));
  drawPillars(r, t("pillars"));
  drawThrone(r, t("throne"));
  drawTreasures(r, t("treasures"));
  return `<svg viewBox="0 0 64 40" shape-rendering="crispEdges" aria-hidden="true">${out.join("")}</svg>`;
}

function drawWalls(r, tier) {
  // Golden walls are marble dressed in gold trim, so gold pillars and throne stand out.
  const [d, m, l] = PAL[tier === 4 ? "marble" : PALACE_MATERIALS[tier]];
  r(0, 0, 64, 31, m);
  if (tier === 0) {                       // cracked bare plaster
    r(10, 4, 1, 6, d); r(11, 9, 1, 4, d); r(52, 14, 1, 7, d); r(51, 20, 1, 3, d); r(33, 2, 1, 4, d);
    return;
  }
  if (tier === 1) {                       // log planks
    for (let y = 3; y < 31; y += 4) r(0, y, 64, 1, d);
    return;
  }
  if (tier === 2) {                       // brickwork
    for (let y = 2, row = 0; y < 31; y += 3, row++) {
      r(0, y, 64, 1, d);
      for (let x = row % 2 ? 4 : 0; x < 64; x += 8) r(x, y - 2, 1, 2, d);
    }
    return;
  }
  // marble panels with a dado rail; gold adds trim and a tapestry behind the throne
  for (let x = 15; x < 64; x += 16) r(x, 0, 1, 31, d);
  r(0, 22, 64, 1, d);
  r(0, 23, 64, 1, l);
  if (tier === 4) {
    const g = PAL.gold;
    r(0, 1, 64, 2, g[1]); r(0, 1, 64, 1, g[2]); r(0, 22, 64, 1, g[1]);
    r(25, 4, 14, 18, RED[1]); r(25, 4, 14, 1, g[2]); r(25, 21, 14, 1, g[1]);
    r(25, 4, 1, 18, g[1]); r(38, 4, 1, 18, g[1]);
  }
}

function drawWindows(r, tier) {
  if (tier === 0) return;
  for (const x of [6, 50]) {
    const y = 6;
    if (tier === 1) {                     // wooden shutters
      const [d, m, l] = PAL.wood;
      r(x, y, 8, 10, d); r(x + 1, y + 1, 3, 8, m); r(x + 4, y + 1, 3, 8, l);
      continue;
    }
    const frame = tier === 2 ? PAL.stone : tier === 3 ? PAL.marble : PAL.gold;
    r(x + 1, y - 1, 6, 1, frame[0]);      // arched top
    r(x, y, 8, 11, frame[0]);
    if (tier === 2) {
      r(x + 1, y, 6, 10, "#6fb7e0"); r(x + 2, y, 4, 1, "#9ad2f0"); r(x + 4, y, 1, 10, frame[0]);
    } else {                              // stained glass
      r(x + 1, y, 3, 5, GLASS[0]); r(x + 4, y, 3, 5, GLASS[1]);
      r(x + 1, y + 5, 3, 5, GLASS[2]); r(x + 4, y + 5, 3, 5, GLASS[3]);
      r(x + 3, y + 4, 2, 2, "#f4e8c0");
    }
    if (tier === 4) {                     // hanging banners beside each window
      const bx = x === 6 ? 15 : 46;
      r(bx, 2, 3, 13, RED[1]); r(bx, 2, 3, 1, PAL.gold[2]); r(bx + 1, 15, 1, 1, RED[0]);
      r(bx + 1, 6, 1, 3, PAL.gold[1]);
    }
  }
}

function drawFloor(r, tier) {
  const [d, m, l] = PAL[tier === 4 ? "marble" : PALACE_MATERIALS[tier]];
  r(0, 31, 64, 9, m);
  r(0, 31, 64, 1, d);
  if (tier === 0) {                       // packed dirt
    for (const [x, y] of [[5, 34], [17, 37], [29, 35], [44, 38], [57, 34], [38, 33]]) r(x, y, 1, 1, l);
    return;
  }
  if (tier === 1) {                       // planks with staggered seams
    for (let y = 34; y < 40; y += 3) r(0, y, 64, 1, d);
    for (let x = 0; x < 64; x += 10) { r(x, 32, 1, 2, d); r(x + 5, 35, 1, 2, d); r(x + 2, 38, 1, 2, d); }
    return;
  }
  if (tier === 2) {                       // flagstones
    for (let y = 35; y < 40; y += 4) r(0, y, 64, 1, d);
    for (let x = 0; x < 64; x += 8) { r(x, 32, 1, 3, d); r(x + 4, 36, 1, 3, d); }
    return;
  }
  // checkered marble; gold adds a red runner to the throne
  for (let y = 32, row = 0; y < 40; y += 3, row++) {
    for (let x = row % 2 ? 4 : 0; x < 64; x += 8) r(x, y, 4, 3, "#3a3a44");
  }
  if (tier === 4) {
    r(28, 31, 8, 9, RED[1]); r(28, 31, 1, 9, PAL.gold[1]); r(35, 31, 1, 9, PAL.gold[1]);
  }
}

function drawPillars(r, tier) {
  for (const x of [20, 40]) {
    if (tier === 0) {                     // broken stumps
      const [d, , l] = PAL.bare;
      r(x, 26, 4, 5, l); r(x, 25, 2, 1, l); r(x + 3, 26, 1, 5, d); r(x + 2, 28, 1, 1, d);
      continue;
    }
    const [d, m, l] = PAL[PALACE_MATERIALS[tier]];
    if (tier === 1) {                     // wooden posts
      r(x + 1, 2, 2, 29, m); r(x + 2, 2, 1, 29, d); r(x, 2, 4, 1, d);
      continue;
    }
    r(x, 4, 4, 26, m); r(x + 3, 4, 1, 26, d); r(x, 4, 1, 26, l);
    if (tier >= 3) { r(x + 1, 5, 1, 24, d); }                        // fluting
    r(x - 1, 2, 6, 2, tier === 4 ? l : d); r(x - 1, 30, 6, 1, d);   // capital and base
    if (tier === 4) { r(x + 1, 2, 2, 1, RED[2]); r(x - 1, 29, 6, 1, l); }
  }
}

function drawThrone(r, tier) {
  if (tier === 0) return;
  const [d, m, l] = PAL[PALACE_MATERIALS[tier]];
  if (tier === 1) {                       // a humble wooden stool
    r(27, 24, 10, 1, d); r(28, 25, 8, 2, l); r(28, 27, 1, 4, d); r(35, 27, 1, 4, d); r(28, 26, 8, 1, m);
    return;
  }
  if (tier === 2) {                       // stone chair
    r(28, 16, 8, 9, m); r(28, 16, 8, 1, l); r(35, 16, 1, 9, d);
    r(27, 24, 10, 3, l); r(27, 27, 10, 4, d);
    return;
  }
  // marble or golden throne
  const top = tier === 4 ? 10 : 12;
  r(27, top, 10, 25 - top, m); r(27, top, 10, 1, l); r(36, top, 1, 25 - top, d);
  r(29, top - 1, 6, 1, m); r(31, top - 2, 2, 1, l);                  // crest
  r(25, 20, 2, 7, d); r(37, 20, 2, 7, d);                          // armrests
  r(28, 23, 8, 3, RED[1]); r(28, 23, 8, 1, RED[2]);                // cushion
  r(26, 26, 12, 5, d); r(26, 26, 12, 1, l);
  if (tier === 4) {
    r(31, 14, 2, 2, GLASS[1]); r(31, 14, 1, 1, "#9ab8ff");          // sapphire
    r(23, 4, 18, 2, RED[1]); r(23, 6, 18, 1, PAL.gold[1]);           // canopy
    for (let x = 23; x < 41; x += 2) r(x, 7, 1, 1, PAL.gold[2]);    // fringe
  }
}

function drawTreasures(r, tier) {
  if (tier === 0) return;
  const W = PAL.wood;
  // barrel (left)
  r(3, 30, 6, 7, W[1]); r(3, 31, 6, 1, W[0]); r(3, 35, 6, 1, W[0]); r(4, 30, 1, 7, W[2]);
  if (tier >= 2) {                        // chest (right)
    const band = tier === 4 ? PAL.gold : PAL.stone;
    r(52, 32, 9, 5, W[1]); r(52, 31, 9, 2, W[2]); r(52, 33, 9, 1, band[1]);
    r(55, 33, 2, 2, band[2]); r(52, 32, 1, 5, band[0]); r(60, 32, 1, 5, band[0]);
  }
  if (tier >= 3) {                        // statue on a pedestal (left)
    const M = PAL.marble;
    r(11, 33, 5, 4, M[0]); r(12, 26, 3, 7, M[2]); r(12, 24, 3, 2, M[1]); r(11, 28, 1, 3, M[1]);
  }
  if (tier === 4) {                       // heaps of gold
    const G = PAL.gold;
    r(45, 35, 7, 2, G[1]); r(46, 34, 5, 1, G[1]); r(47, 33, 3, 1, G[2]);
    r(16, 36, 6, 1, G[1]); r(17, 35, 4, 1, G[2]); r(53, 30, 3, 1, G[2]);
  }
}
