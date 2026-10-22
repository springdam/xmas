// Palace art: a 256x160 throne room in one of four styles, drawn from six parts, each at
// tier 0 (bare) .. 4 (golden). Smooth vector shapes (gradients, arches) rather than pixels.
// palaceArt(palace) returns an <svg> string; palace = {style, walls, floor, throne, pillars, windows, treasures}.

const PALACE_STYLES = {
  PRESIDENTIAL: { label: "Presidential", tiers: ["Bare", "Clapboard", "Plaster", "Marble", "Gilded"] },
  CASTLE: { label: "Castle", tiers: ["Bare", "Wooden", "Stone", "Marble", "Golden"] },
  MIDDLE_EASTERN: { label: "Middle Eastern", tiers: ["Bare", "Adobe", "Sandstone", "Tiled", "Golden"] },
  ASIAN: { label: "Asian", tiers: ["Bare", "Bamboo", "Lacquered", "Jade", "Golden"] },
};
const PALACE_PARTS = [
  ["walls", "WALLS"], ["floor", "FLOOR"], ["throne", "THRONE"],
  ["pillars", "PILLARS"], ["windows", "WINDOWS"], ["treasures", "TREASURES"],
];
/** Tier names for a palace (castle names until a style is chosen). */
const palaceTierNames = (palace) => (PALACE_STYLES[palace?.style] || PALACE_STYLES.CASTLE).tiers;

const palaceArt = (() => {
  const FLOOR = 124;
  let seq = 0;

  // ---- drawing kit ---------------------------------------------------------------
  class Art {
    constructor() { this.id = `pa${++seq}`; this.n = 0; this.defs = []; this.body = []; }
    attrs(o = {}) {
      let s = "";
      if (o.stroke) s += ` stroke="${o.stroke}" stroke-width="${o.sw ?? 1}"`;
      if (o.op != null) s += ` opacity="${o.op}"`;
      if (o.rx != null) s += ` rx="${o.rx}"`;
      if (o.lj) s += ` stroke-linejoin="${o.lj}" stroke-linecap="round"`;
      return s;
    }
    rect(x, y, w, h, fill, o) { this.body.push(`<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}"${this.attrs(o)}/>`); }
    path(d, fill, o) { this.body.push(`<path d="${d}" fill="${fill}"${this.attrs(o)}/>`); }
    ellipse(cx, cy, rx, ry, fill, o) { this.body.push(`<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="${fill}"${this.attrs(o)}/>`); }
    circle(cx, cy, r, fill, o) { this.ellipse(cx, cy, r, r, fill, o); }
    line(x1, y1, x2, y2, stroke, sw = 1, op) {
      this.body.push(`<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${stroke}" stroke-width="${sw}"${op != null ? ` opacity="${op}"` : ""}/>`);
    }
    /** Linear gradient (vertical by default); returns a fill reference. */
    lin(stops, x2 = 0, y2 = 1) {
      const id = `${this.id}g${++this.n}`;
      this.defs.push(`<linearGradient id="${id}" x1="0" y1="0" x2="${x2}" y2="${y2}">${stops.map((c, i) =>
        `<stop offset="${(i / (stops.length - 1)).toFixed(2)}" stop-color="${c}"/>`).join("")}</linearGradient>`);
      return `url(#${id})`;
    }
    rad(stops) {
      const id = `${this.id}g${++this.n}`;
      this.defs.push(`<radialGradient id="${id}">${stops.map((c, i) =>
        `<stop offset="${(i / (stops.length - 1)).toFixed(2)}" stop-color="${c}"/>`).join("")}</radialGradient>`);
      return `url(#${id})`;
    }
    /** Draws whatever fn adds clipped to the shape d. */
    clipped(d, fn) {
      const id = `${this.id}c${++this.n}`;
      this.defs.push(`<clipPath id="${id}"><path d="${d}"/></clipPath>`);
      this.body.push(`<g clip-path="url(#${id})">`);
      fn();
      this.body.push("</g>");
    }
    svg() {
      return `<svg viewBox="0 0 256 160" aria-hidden="true"><defs>${this.defs.join("")}</defs>${this.body.join("")}</svg>`;
    }
  }

  // shared colours
  const GOLD = { d: "#8a5c00", m: "#d19a10", l: "#f6c840", hi: "#fff2a8" };
  const RED = { d: "#5a0c18", m: "#9e1a2c", l: "#cc3346", hi: "#f0788a" };
  const INK = "#2a2633";
  const SKY = ["#9fd6f5", "#5ea8dc"];

  /** Path for an opening of a given shape, filling x..x+w, top y, bottom y+h. */
  function opening(kind, x, y, w, h) {
    const r = w / 2, cx = x + r;
    switch (kind) {
      case "round": return `M${x} ${y + h}V${y + r}A${r} ${r} 0 0 1 ${x + w} ${y + r}V${y + h}Z`;
      case "pointed": return `M${x} ${y + h}V${y + r}Q${x} ${y} ${cx} ${y - r * 0.4}Q${x + w} ${y} ${x + w} ${y + r}V${y + h}Z`;
      case "horseshoe": return `M${x + w * 0.1} ${y + h}V${y + r * 1.2}A${r * 1.1} ${r * 1.1} 0 1 1 ${x + w * 0.9} ${y + r * 1.2}V${y + h}Z`;
      case "ogee": return `M${x} ${y + h}V${y + r}C${x} ${y + r * 0.3} ${cx - 2} ${y + r * 0.4} ${cx} ${y - r * 0.5}C${cx + 2} ${y + r * 0.4} ${x + w} ${y + r * 0.3} ${x + w} ${y + r}V${y + h}Z`;
      case "moon": return `M${cx - r} ${y + r}A${r} ${r} 0 1 1 ${cx + r} ${y + r}A${r} ${r} 0 1 1 ${cx - r} ${y + r}Z`;
      default: return `M${x} ${y}H${x + w}V${y + h}H${x}Z`;
    }
  }

  /** A round column with base and capital; cap = "plain" | "doric" | "ionic" | "corinthian" | "bracket" | "muqarnas". */
  function column(a, x, w, top, pal, o = {}) {
    const bottom = FLOOR;
    a.rect(x, top, w, bottom - top, a.lin([pal.d, pal.l, pal.hi, pal.m, pal.d], 1, 0));
    if (o.fluted) for (let k = 1; k < 4; k++) a.line(x + (w * k) / 4, top + 4, x + (w * k) / 4, bottom - 4, pal.d, 0.6, 0.6);
    if (o.bands) for (const y of o.bands) a.rect(x - 1, y, w + 2, 3, a.lin([GOLD.hi, GOLD.m, GOLD.d]));
    a.rect(x - 3, bottom - 5, w + 6, 5, pal.m, { stroke: pal.d, sw: 0.6 });         // base
    const cap = o.cap || "plain";
    if (cap === "bracket") {                                                          // Asian dougong
      a.rect(x - 4, top - 4, w + 8, 4, o.capFill || pal.m, { stroke: pal.d, sw: 0.6 });
      a.rect(x - 8, top - 8, w + 16, 4, o.capFill || pal.l, { stroke: pal.d, sw: 0.6 });
      a.rect(x - 12, top - 12, w + 24, 4, o.capFill || pal.m, { stroke: pal.d, sw: 0.6 });
      return;
    }
    if (cap === "muqarnas") {                                                         // stepped honeycomb
      for (let k = 0; k < 3; k++) a.rect(x - 2 - k * 2, top - 3 - k * 3, w + 4 + k * 4, 3, k % 2 ? pal.l : pal.m, { stroke: pal.d, sw: 0.5 });
      return;
    }
    a.rect(x - 3, top - 4, w + 6, 4, pal.l, { stroke: pal.d, sw: 0.6 });
    if (cap === "ionic" || cap === "corinthian") {
      a.circle(x - 2, top - 1, 2.5, pal.l, { stroke: pal.d, sw: 0.6 });
      a.circle(x + w + 2, top - 1, 2.5, pal.l, { stroke: pal.d, sw: 0.6 });
    }
    if (cap === "corinthian") a.path(`M${x} ${top}Q${x + w / 2} ${top + 7} ${x + w} ${top}Z`, o.capFill || pal.l, { stroke: pal.d, sw: 0.5 });
  }

  // ---- bare (tier 0) parts, tinted per style ---------------------------------------
  const BARE = {
    PRESIDENTIAL: ["#23262e", "#30343e", "#40454f"],
    CASTLE: ["#1e1b24", "#2c2833", "#3c3645"],
    MIDDLE_EASTERN: ["#2a2019", "#3a2d23", "#4c3c2e"],
    ASIAN: ["#2a1a1a", "#3a2424", "#4c3232"],
  };
  function bareWalls(a, style) {
    const [d, m, l] = BARE[style];
    a.rect(0, 0, 256, FLOOR, a.lin([d, m]));
    a.ellipse(40, 80, 26, 18, d, { op: 0.6 }); a.ellipse(210, 50, 20, 26, d, { op: 0.5 });   // damp
    for (const c of ["M50 10l4 12-3 8 5 14", "M200 30l-3 10 4 9-2 12", "M128 6l2 9-4 6", "M20 60l6 8-2 10"]) {
      a.path(c, "none", { stroke: d, sw: 1.2 });
    }
    a.rect(150, 70, 30, 16, l, { op: 0.5 });                                           // exposed brick
    for (let y = 74; y < 86; y += 4) a.line(150, y, 180, y, d, 0.8);
  }
  function bareFloor(a, style) {
    const [d, m, l] = BARE[style];
    a.rect(0, FLOOR, 256, 36, a.lin([d, m]));
    for (const [x, y] of [[20, 134], [60, 150], [100, 140], [150, 154], [190, 138], [230, 148], [128, 132]]) a.ellipse(x, y, 3, 1.2, l, { op: 0.7 });
  }
  function bareStumps(a, style) {
    const [d, , l] = BARE[style];
    for (const x of [80, 160]) {
      a.path(`M${x} ${FLOOR}V${FLOOR - 22}l4 -4 3 5 4 -7 5 6V${FLOOR}Z`, l, { stroke: d, sw: 0.8 });
      a.ellipse(x + 24, FLOOR - 2, 5, 3, l); a.ellipse(x - 6, FLOOR - 1, 4, 2, l, { op: 0.8 });
    }
  }

  // ---- CASTLE -----------------------------------------------------------------------
  const CASTLE_MAT = [null,
    { d: "#4a2c12", m: "#7a4e24", l: "#a06c38", hi: "#c8925a" },
    { d: "#45454f", m: "#6f6f7c", l: "#9a9aa6", hi: "#c4c4cc" },
    { d: "#a39c8e", m: "#d2ccbe", l: "#eae6dc", hi: "#fbf9f4" },
    { d: "#a39c8e", m: "#d2ccbe", l: "#eae6dc", hi: "#fbf9f4" }];

  const castle = {
    walls(a, t) {
      if (t === 0) return bareWalls(a, "CASTLE");
      const p = CASTLE_MAT[t];
      if (t === 1) {
        for (let y = 0, row = 0; y < FLOOR; y += 12, row++) {
          a.rect(0, y, 256, 12, a.lin([p.l, p.m, p.d]));
          for (let x = row % 2 ? 30 : 8; x < 256; x += 56) a.line(x, y + 1, x, y + 11, p.d, 1);
          for (let x = (row * 37) % 50; x < 256; x += 50) a.ellipse(x + 14, y + 6, 3, 1.5, p.d, { op: 0.7 });
        }
        return;
      }
      if (t === 2) {
        a.rect(0, 0, 256, FLOOR, p.d);
        for (let y = 0, row = 0; y < FLOOR; y += 14, row++) {
          for (let x = row % 2 ? -16 : 0, k = row; x < 256; x += 32, k++) {
            a.rect(x + 1, y + 1, 30, 12, a.lin([k % 3 ? p.l : p.hi, p.m]), { rx: 1.5 });
          }
        }
        return;
      }
      a.rect(0, 0, 256, FLOOR, a.lin([p.hi, p.m]));
      for (let x = 0; x < 256; x += 64) {
        a.rect(x + 6, 12, 52, 64, a.lin([p.l, p.m]), { stroke: p.d, sw: 1, rx: 2 });
        a.path(`M${x + 14} 20q12 18 4 30t18 24`, "none", { stroke: p.d, sw: 0.6, op: 0.5 });     // veins
      }
      a.rect(0, 88, 256, 4, p.hi); a.rect(0, 92, 256, 2, p.d); a.rect(0, 94, 256, 30, a.lin([p.m, p.d]));
      if (t === 4) {
        a.rect(0, 0, 256, 9, a.lin([GOLD.hi, GOLD.m, GOLD.d])); a.rect(0, 88, 256, 4, a.lin([GOLD.hi, GOLD.d]));
        a.rect(92, 14, 72, 74, a.lin([RED.l, RED.m, RED.d]), { stroke: GOLD.m, sw: 3 });      // tapestry
        a.path("M128 30l9 10-9 10-9-10z", GOLD.l); a.circle(128, 40, 3, RED.d);
        for (let x = 94; x < 164; x += 5) a.path(`M${x} 88l2 6 2-6`, GOLD.m);
      }
    },
    windows(a, t) {
      if (t === 0) return;
      for (const x of [24, 192]) {
        if (t === 1) {
          a.rect(x - 2, 30, 44, 50, castle.matOf(1).d);
          a.rect(x, 32, 19, 46, a.lin([castle.matOf(1).l, castle.matOf(1).m], 1, 0));
          a.rect(x + 21, 32, 19, 46, a.lin([castle.matOf(1).m, castle.matOf(1).l], 1, 0));
          for (const y of [38, 70]) { a.rect(x, y, 6, 3, INK); a.rect(x + 34, y, 6, 3, INK); }
          continue;
        }
        const frame = t === 4 ? GOLD : CASTLE_MAT[t === 2 ? 2 : 3];
        const kind = t === 2 ? "round" : "pointed";
        a.path(opening(kind, x - 4, 30, 48, 54), frame.m, { stroke: frame.d, sw: 1.5 });
        a.path(opening(kind, x, 34, 40, 48), t === 2 ? a.lin(SKY) : a.lin(["#7a1f8a", "#2e62c8", "#e6b422"]));
        if (t >= 3) {
          for (const [cx, cy, c] of [[x + 10, 52, "#c0303a"], [x + 30, 52, "#3a9a48"], [x + 20, 66, "#e6b422"], [x + 20, 42, "#c0303a"]]) a.circle(cx, cy, 5, c, { stroke: INK, sw: 0.8 });
        }
        a.line(x + 20, 32, x + 20, 82, frame.d, 1.5); a.line(x, 60, x + 40, 60, frame.d, 1.5);
        a.rect(x - 6, 82, 52, 4, frame.l, { stroke: frame.d, sw: 0.8 });
        if (t === 4) {                                                                    // banners
          const bx = x === 24 ? 70 : 174;
          a.path(`M${bx} 6h12v54l-6-6-6 6z`, a.lin([RED.l, RED.m]), { stroke: GOLD.m, sw: 1 });
          a.path(`M${bx + 2} 22l2 4 2-4 2 4 2-4v8h-8z`, GOLD.l);
        }
      }
    },
    floor(a, t) {
      if (t === 0) return bareFloor(a, "CASTLE");
      const p = CASTLE_MAT[t];
      if (t === 1) {
        for (let y = FLOOR, row = 0; y < 160; y += 9, row++) {
          a.rect(0, y, 256, 9, a.lin([p.l, p.m]));
          for (let x = row % 2 ? 20 : 50; x < 256; x += 60) a.line(x, y, x, y + 9, p.d, 1);
        }
        return;
      }
      if (t === 2) {
        a.rect(0, FLOOR, 256, 36, p.d);
        for (let y = FLOOR, row = 0; y < 160; y += 12, row++)
          for (let x = row % 2 ? -20 : 0; x < 256; x += 40) a.rect(x + 1, y + 1, 38, 10, a.lin([p.l, p.m]), { rx: 1 });
        return;
      }
      a.rect(0, FLOOR, 256, 36, p.hi);
      for (let y = FLOOR, row = 0; y < 160; y += 12, row++)
        for (let x = row % 2 ? 16 : 0; x < 256; x += 32) a.rect(x, y, 16, 12, "#34303c");
      a.rect(0, FLOOR, 256, 36, a.lin(["#ffffff", "#000000"]), { op: 0.12 });
      if (t === 4) {
        a.path(`M108 ${FLOOR}H148L160 160H96Z`, a.lin([RED.l, RED.d]), { stroke: GOLD.m, sw: 2 });
      }
    },
    pillars(a, t) {
      if (t === 0) return bareStumps(a, "CASTLE");
      for (const x of [80, 160]) {
        if (t === 1) {
          a.rect(x + 3, 6, 10, FLOOR - 6, a.lin([CASTLE_MAT[1].l, CASTLE_MAT[1].d], 1, 0));
          a.rect(x - 4, 2, 24, 6, CASTLE_MAT[1].d);
          continue;
        }
        const pal = t === 4 ? GOLD : CASTLE_MAT[t];
        column(a, x, 16, 14, { ...pal, hi: pal.hi || GOLD.hi }, { fluted: t >= 3, cap: t >= 3 ? "ionic" : "doric" });
      }
    },
    throne(a, t) {
      if (t === 0) return;
      if (t === 1) {
        const p = CASTLE_MAT[1];
        a.rect(110, 96, 36, 8, a.lin([p.hi, p.m]), { rx: 2 });
        a.rect(113, 104, 5, 20, p.d); a.rect(138, 104, 5, 20, p.d); a.rect(116, 112, 24, 3, p.m);
        return;
      }
      if (t === 2) {
        const p = CASTLE_MAT[2];
        a.rect(110, 56, 36, 46, a.lin([p.hi, p.m]), { stroke: p.d, sw: 1, rx: 2 });
        a.rect(116, 62, 24, 30, p.m, { stroke: p.d, sw: 0.8 });
        a.rect(102, 84, 10, 22, p.l, { stroke: p.d, sw: 0.8 }); a.rect(144, 84, 10, 22, p.l, { stroke: p.d, sw: 0.8 });
        a.rect(104, 96, 48, 10, p.l, { stroke: p.d, sw: 0.8 }); a.rect(100, 106, 56, 18, a.lin([p.m, p.d]));
        return;
      }
      const p = t === 4 ? GOLD : CASTLE_MAT[3];
      if (t === 4) {                                                                       // canopy
        a.path("M84 4H172V18Q128 30 84 18Z", a.lin([RED.l, RED.d]), { stroke: GOLD.m, sw: 2 });
        a.path("M86 18Q90 60 96 96H84Z", a.lin([RED.l, RED.m], 1, 0)); a.path("M170 18Q166 60 160 96H172Z", a.lin([RED.m, RED.l], 1, 0));
      }
      a.path("M106 100V44Q106 30 128 24Q150 30 150 44V100Z", a.lin([p.hi, p.l, p.m]), { stroke: p.d, sw: 1.2 });
      a.path("M114 92V48Q114 38 128 34Q142 38 142 48V92Z", a.lin([RED.l, RED.m, RED.d]));
      a.rect(96, 82, 12, 26, a.lin([p.l, p.m], 1, 0), { stroke: p.d, sw: 0.8, rx: 3 });
      a.rect(148, 82, 12, 26, a.lin([p.m, p.l], 1, 0), { stroke: p.d, sw: 0.8, rx: 3 });
      a.rect(104, 90, 48, 10, a.lin([RED.hi, RED.m]), { rx: 3 });
      a.rect(100, 100, 56, 8, p.l, { stroke: p.d, sw: 0.8 });
      a.rect(94, 108, 68, 8, a.lin([p.l, p.m])); a.rect(88, 116, 80, 8, a.lin([p.m, p.d]));
      if (t === 4) {
        a.circle(128, 52, 5, a.rad(["#cfe0ff", "#2e62c8", "#1a3a8a"]));
        for (const [cx, cy] of [[118, 26], [128, 20], [138, 26]]) a.path(`M${cx - 4} ${cy + 4}L${cx} ${cy - 4}L${cx + 4} ${cy + 4}Z`, GOLD.l);
      }
    },
    treasures(a, t) {
      if (t === 0) return;
      const w = CASTLE_MAT[1];
      a.path("M14 124V100Q22 94 30 100V124Z", a.lin([w.l, w.d], 1, 0)); a.ellipse(22, 100, 8, 3, w.hi);
      a.rect(13, 104, 18, 2, "#3a3a44"); a.rect(13, 117, 18, 2, "#3a3a44");
      if (t >= 2) {
        const band = t === 4 ? GOLD : CASTLE_MAT[2];
        a.rect(206, 108, 38, 16, a.lin([w.l, w.d]), { stroke: w.d, sw: 0.8 });
        a.path("M206 108V102Q225 94 244 102V108Z", a.lin([w.hi, w.m]));
        a.rect(206, 106, 38, 3, band.m); a.rect(222, 108, 6, 7, band.l, { stroke: band.d, sw: 0.6 });
      }
      if (t >= 3) {                                                                         // suit of armour
        const s = CASTLE_MAT[2];
        a.rect(40, 116, 22, 8, s.d);
        a.path("M44 116V90Q51 84 58 90V116Z", a.lin([s.hi, s.m], 1, 0), { stroke: s.d, sw: 0.8 });
        a.circle(51, 80, 6, a.lin([s.hi, s.m]), { stroke: s.d, sw: 0.8 }); a.rect(47, 79, 8, 2, INK);
        a.line(63, 72, 63, 116, "#8a8a96", 1.5);
      }
      if (t === 4) {
        a.path("M180 124Q192 108 204 124Z", a.lin([GOLD.hi, GOLD.d]));
        a.path("M66 124Q76 112 86 124Z", a.lin([GOLD.hi, GOLD.d]));
        a.path("M210 102Q225 90 240 102Z", a.lin([GOLD.hi, GOLD.m])); a.circle(225, 97, 2.5, "#3a9a48");
      }
    },
    matOf: (t) => CASTLE_MAT[t],
  };

  // ---- PRESIDENTIAL ------------------------------------------------------------------
  const PRES_MAT = [null,
    { d: "#6c7a85", m: "#a3b0b9", l: "#c9d3da", hi: "#e8eef2" },
    { d: "#b3a283", m: "#d9cbae", l: "#efe4cf", hi: "#fbf6ea" },
    { d: "#a8a8b0", m: "#d6d6dc", l: "#eeeef2", hi: "#ffffff" },
    { d: "#b3a283", m: "#efe2bf", l: "#f8f0d8", hi: "#fffaf0" }];
  const NAVY = { d: "#132a52", m: "#1f3f78", l: "#2f5aa6" };
  const MAHOGANY = { d: "#3a160c", m: "#5e2614", l: "#84402a", hi: "#a8603e" };

  const presidential = {
    walls(a, t) {
      if (t === 0) return bareWalls(a, "PRESIDENTIAL");
      const p = PRES_MAT[t];
      if (t === 1) {                                                                         // lap siding
        for (let y = 0; y < FLOOR; y += 8) { a.rect(0, y, 256, 8, a.lin([p.hi, p.m])); a.line(0, y + 8, 256, y + 8, p.d, 0.8); }
        return;
      }
      a.rect(0, 0, 256, FLOOR, a.lin([p.hi, p.l]));
      if (t === 4) {                                                                         // damask wallpaper
        for (let y = 16; y < 84; y += 14) for (let x = (y / 14) % 2 ? 10 : 0; x < 256; x += 20)
          a.path(`M${x} ${y}l3 -5 3 5 -3 5z`, GOLD.l, { op: 0.55 });
      }
      a.rect(0, 0, 256, 7, t === 4 ? a.lin([GOLD.hi, GOLD.m]) : p.hi, { stroke: p.d, sw: 0.6 });   // crown moulding
      a.rect(0, 86, 256, 38, a.lin([p.l, p.m]));                                             // wainscot
      a.rect(0, 85, 256, 3, t === 4 ? GOLD.m : p.hi, { stroke: p.d, sw: 0.5 });
      if (t >= 3) for (let x = 8; x < 256; x += 40) a.rect(x, 92, 30, 26, "none", { stroke: p.d, sw: 0.8, rx: 1 });
    },
    windows(a, t) {
      if (t === 0) return;
      for (const x of [24, 192]) {
        const frame = PRES_MAT[3];
        if (t === 4) a.path(opening("round", x, 14, 40, 70), frame.hi, { stroke: GOLD.m, sw: 2 });     // fanlight
        a.rect(x, 28, 40, 56, a.lin(SKY), { stroke: frame.d, sw: 2 });
        for (let k = 1; k < 3; k++) a.line(x + (40 * k) / 3, 28, x + (40 * k) / 3, 84, frame.hi, 2);
        for (const y of [46, 65]) a.line(x, y, x + 40, y, frame.hi, 2);
        if (t >= 2) {                                                                        // curtains
          const cloth = t === 4 ? GOLD : t === 3 ? NAVY : { d: "#7a6a52", m: "#a8946e", l: "#c8b48c" };
          a.path(`M${x - 6} 22Q${x + 4} 54 ${x - 4} 90H${x - 12}V22Z`, a.lin([cloth.l, cloth.m, cloth.d], 1, 0));
          a.path(`M${x + 46} 22Q${x + 36} 54 ${x + 44} 90H${x + 52}V22Z`, a.lin([cloth.d, cloth.m, cloth.l], 1, 0));
          if (t >= 3) a.path(`M${x - 12} 20H${x + 52}V28Q${x + 20} 40 ${x - 12} 28Z`, cloth.m, { stroke: cloth.d, sw: 0.8 });   // swag
          if (t === 4) for (const tx of [x - 8, x + 48]) { a.line(tx, 56, tx, 66, GOLD.d, 1); a.circle(tx, 68, 2.5, GOLD.l); }
        }
      }
    },
    floor(a, t) {
      if (t === 0) return bareFloor(a, "PRESIDENTIAL");
      if (t === 1) {
        for (let y = FLOOR; y < 160; y += 6) a.rect(0, y, 256, 6, a.lin(["#b88a5a", "#9a6c40"]));
        return;
      }
      a.rect(0, FLOOR, 256, 36, "#8a5a32");
      for (let y = FLOOR, row = 0; y < 160; y += 6, row++)                                   // herringbone parquet
        for (let x = row % 2 ? -6 : 0; x < 256; x += 12) a.path(`M${x} ${y + 6}l6 -6 6 6z`, row % 2 ? "#a8743c" : "#b8844a", { op: 0.9 });
      if (t >= 3) {                                                                          // oval rug
        const rug = t === 4 ? NAVY : { d: "#7a6448", m: "#b49c74", l: "#d8c49a" };
        a.ellipse(128, 144, 96, 15, a.rad([rug.l, rug.m]), { stroke: t === 4 ? GOLD.m : rug.d, sw: 3 });
        if (t === 4) {
          a.ellipse(128, 144, 30, 7, GOLD.d, { op: 0.5 }); a.ellipse(128, 144, 24, 5, NAVY.l);
          for (let k = 0; k < 13; k++) { const an = (k / 13) * Math.PI * 2; a.circle(128 + Math.cos(an) * 27, 144 + Math.sin(an) * 6, 0.9, "#ffffff"); }
          a.path("M122 145l6-4 6 4-6 1z", GOLD.l);
        }
      }
    },
    pillars(a, t) {
      if (t === 0) return bareStumps(a, "PRESIDENTIAL");
      for (const x of [80, 160]) {
        if (t === 1) { a.rect(x + 2, 8, 12, FLOOR - 8, a.lin([PRES_MAT[1].hi, PRES_MAT[1].m], 1, 0), { stroke: PRES_MAT[1].d, sw: 0.6 }); continue; }
        const pal = PRES_MAT[t === 4 ? 3 : t];
        column(a, x, 16, 14, pal, { fluted: t >= 3, cap: t === 2 ? "doric" : t === 3 ? "ionic" : "corinthian", capFill: t === 4 ? GOLD.l : undefined });
      }
    },
    throne(a, t) {                                                                           // the desk
      if (t === 0) return;
      const chair = (top, fill, stroke) => {
        a.path(`M114 ${top + 30}V${top + 6}Q114 ${top} 128 ${top}Q142 ${top} 142 ${top + 6}V${top + 30}Z`, fill, { stroke, sw: 1 });
      };
      if (t === 1) {
        chair(76, a.lin(["#b88a5a", "#8a5a32"]), "#5a3a1a");
        a.rect(112, 104, 32, 5, "#8a5a32"); a.rect(114, 109, 4, 15, "#5a3a1a"); a.rect(138, 109, 4, 15, "#5a3a1a");
        return;
      }
      chair(t >= 3 ? 50 : 62, a.lin(t >= 3 ? ["#5a2a1a", "#2e140c"] : ["#7a5a42", "#4a3424"]), "#1a0c06");
      const wood = t >= 3 ? MAHOGANY : { d: "#5a3a1a", m: "#8a5a32", l: "#b88a5a", hi: "#d8aa78" };
      if (t === 4) {                                                                         // flags behind the desk
        for (const [fx, dir, colours] of [[100, 1, ["#b22234", "#ffffff"]], [156, -1, [NAVY.m, NAVY.l]]]) {
          a.line(fx, 20, fx, FLOOR, GOLD.m, 2); a.circle(fx, 18, 2.5, GOLD.l);
          const fl = dir > 0 ? fx + 1 : fx - 23;
          for (let k = 0; k < 7; k++) a.rect(fl, 22 + k * 4, 22, 4, colours[k % 2]);
          if (dir > 0) a.rect(fl, 22, 9, 12, NAVY.m); else a.circle(fl + 11, 36, 5, GOLD.l);
        }
      }
      a.rect(84, 84, 88, 10, a.lin([wood.hi, wood.l]), { stroke: wood.d, sw: 1, rx: 1 });      // desktop
      a.rect(88, 94, 80, 30, a.lin([wood.l, wood.m, wood.d]), { stroke: wood.d, sw: 1 });
      a.rect(116, 100, 24, 24, wood.d, { op: 0.5 });                                            // knee panel
      if (t >= 3) for (const px of [92, 146]) a.rect(px, 98, 18, 22, "none", { stroke: t === 4 ? GOLD.l : wood.hi, sw: 1 });
      if (t === 4) { a.circle(128, 110, 6, GOLD.l, { stroke: GOLD.d, sw: 1 }); a.rect(100, 80, 10, 4, "#22663a"); a.circle(150, 82, 3, GOLD.l); }
    },
    treasures(a, t) {
      if (t === 0) return;
      a.rect(14, 108, 22, 16, "#8a5a32", { stroke: "#5a3a1a", sw: 0.8 });                    // side table + lamp
      a.line(25, 108, 25, 92, "#3a3a44", 1.5); a.path("M17 92h16l-4 -10h-8z", "#efe4cf", { stroke: "#b3a283", sw: 0.6 });
      if (t >= 2) {                                                                          // globe
        a.rect(222, 116, 16, 8, MAHOGANY.m); a.line(230, 116, 230, 106, MAHOGANY.d, 2);
        a.circle(230, 98, 10, a.rad(["#9fd0ff", "#2f6aa6"]), { stroke: GOLD.d, sw: 1 });
        a.path("M224 94q4 4 2 8t6 4", "#4a8a3a"); a.path("M233 92q3 3 1 6", "#4a8a3a");
      }
      if (t >= 3) {                                                                          // bookcase
        a.rect(40, 62, 30, 62, MAHOGANY.m, { stroke: MAHOGANY.d, sw: 1 });
        for (let y = 70; y < 120; y += 13) {
          a.rect(42, y + 9, 26, 2, MAHOGANY.d);
          for (let x = 43; x < 66; x += 3) a.rect(x, y, 2.5, 9, ["#7a1a1a", "#1f3f78", "#2a6a3a", "#b08a3a"][(x + y) % 4]);
        }
      }
      if (t === 4) {                                                                         // gilded eagle and bust
        a.rect(196, 96, 14, 28, PRES_MAT[3].l, { stroke: PRES_MAT[3].d, sw: 0.8 });
        a.path("M190 92q13 -16 26 0l-8 -3-5 6-5-6z", a.lin([GOLD.hi, GOLD.d]));
        a.rect(84, 112, 12, 12, PRES_MAT[3].l); a.circle(90, 106, 5, PRES_MAT[3].hi, { stroke: PRES_MAT[3].d, sw: 0.6 });
      }
    },
  };

  // ---- MIDDLE EASTERN -------------------------------------------------------------------
  const ME_MAT = [null,
    { d: "#6a4428", m: "#a8764c", l: "#c99a6c", hi: "#e2bf92" },
    { d: "#8a6a42", m: "#c99f6a", l: "#e2c08c", hi: "#f3dcb0" },
    { d: "#c8c2b4", m: "#ebe5d8", l: "#f6f2e8", hi: "#ffffff" },
    { d: "#c8c2b4", m: "#ebe5d8", l: "#f6f2e8", hi: "#ffffff" }];
  const TEAL = { d: "#145a5a", m: "#1f8a86", l: "#3fb8b0", hi: "#9be4dc" };
  const AZURE = { d: "#123a6a", m: "#1f5aa0", l: "#3f86c8" };

  /** An eight-point star tile centred on (cx, cy). */
  function star8(a, cx, cy, r, fill, o) {
    const pts = [];
    for (let k = 0; k < 16; k++) { const an = (k / 16) * Math.PI * 2; const rr = k % 2 ? r * 0.55 : r; pts.push(`${(cx + Math.cos(an) * rr).toFixed(1)} ${(cy + Math.sin(an) * rr).toFixed(1)}`); }
    a.path(`M${pts.join("L")}Z`, fill, o);
  }

  const middleEastern = {
    walls(a, t) {
      if (t === 0) return bareWalls(a, "MIDDLE_EASTERN");
      const p = ME_MAT[t];
      a.rect(0, 0, 256, FLOOR, a.lin([p.hi, p.m]));
      if (t === 1) { for (const c of ["M30 20q6 10 0 20", "M220 40q-4 8 2 16"]) a.path(c, "none", { stroke: p.d, sw: 0.8, op: 0.6 }); return; }
      if (t === 2) {
        for (let y = 0, row = 0; y < FLOOR; y += 16, row++)
          for (let x = row % 2 ? -24 : 0; x < 256; x += 48) a.rect(x + 1, y + 1, 46, 14, "none", { stroke: p.d, sw: 0.8, op: 0.7 });
        return;
      }
      // tiled dado (zellige) under white stucco
      a.rect(0, 80, 256, 44, AZURE.d);
      for (let y = 86; y < 124; y += 12) for (let x = (y % 24 ? 0 : 6); x < 256; x += 12) star8(a, x + 6, y, 5, (x / 12 + y) % 3 ? TEAL.l : "#ffffff");
      a.rect(0, 76, 256, 4, t === 4 ? a.lin([GOLD.hi, GOLD.d]) : TEAL.m);
      a.rect(0, 0, 256, 14, t === 4 ? a.lin([GOLD.l, GOLD.d]) : AZURE.m);                     // inscription band
      for (let x = 6; x < 256; x += 14) a.path(`M${x} 10q4 -8 8 0`, "none", { stroke: t === 4 ? GOLD.hi : "#ffffff", sw: 1.2 });
      if (t === 4) for (let x = 0; x < 256; x += 32) a.path(`M${x + 16} 22q10 10 0 20q-10 -10 0 -20z`, "none", { stroke: GOLD.m, sw: 1, op: 0.7 });   // arabesque
    },
    windows(a, t) {
      if (t === 0) return;
      for (const x of [24, 192]) {
        if (t === 1) {
          a.rect(x + 6, 36, 28, 28, ME_MAT[1].d);
          for (let k = 0; k < 4; k++) { a.line(x + 6 + k * 7 + 3.5, 36, x + 6 + k * 7 + 3.5, 64, ME_MAT[1].hi, 1.5); a.line(x + 6, 36 + k * 7 + 3.5, x + 34, 36 + k * 7 + 3.5, ME_MAT[1].hi, 1.5); }
          continue;
        }
        const kind = t === 2 ? "pointed" : t === 3 ? "horseshoe" : "ogee";
        const frame = t === 4 ? GOLD : t === 3 ? TEAL : ME_MAT[2];
        a.path(opening(kind, x - 4, 30, 48, 56), frame.m, { stroke: frame.d, sw: 1.5 });
        a.path(opening(kind, x, 34, 40, 50), t === 2 ? a.lin(SKY) : a.lin(["#f6c870", "#c86a3a"]));
        if (t >= 3) {                                                                          // mashrabiya lattice
          a.clipped(opening(kind, x, 34, 40, 50), () => {
            for (let k = -4; k < 9; k++) { a.line(x + k * 7, 20, x + k * 7 + 36, 86, "#5a3a1a", 1.2, 0.8); a.line(x + 40 - k * 7, 20, x + 4 - k * 7, 86, "#5a3a1a", 1.2, 0.8); }
          });
          a.path(opening(kind, x, 34, 40, 50), "none", { stroke: frame.d, sw: 2 });
        }
      }
    },
    floor(a, t) {
      if (t === 0) return bareFloor(a, "MIDDLE_EASTERN");
      if (t === 1) { a.rect(0, FLOOR, 256, 36, a.lin(["#c8a070", "#a07a4a"])); return; }
      if (t === 2) {                                                                           // woven mat
        a.rect(0, FLOOR, 256, 36, "#b89058");
        for (let y = FLOOR + 3; y < 160; y += 6) a.line(0, y, 256, y, "#8a6a3a", 1);
        return;
      }
      a.rect(0, FLOOR, 256, 36, "#f2ece0");
      for (let y = FLOOR + 6, row = 0; y < 160; y += 12, row++) for (let x = row % 2 ? 12 : 0; x < 256; x += 24) star8(a, x + 6, y, 6, TEAL.m, { op: 0.8 });
      if (t === 4) {                                                                            // persian carpet
        a.rect(56, FLOOR + 4, 144, 30, a.lin([RED.m, RED.d]), { stroke: GOLD.m, sw: 3 });
        a.rect(62, FLOOR + 8, 132, 22, "none", { stroke: AZURE.m, sw: 1.5 });
        a.ellipse(128, FLOOR + 19, 22, 8, AZURE.m, { stroke: GOLD.l, sw: 1.5 }); star8(a, 128, FLOOR + 19, 6, GOLD.l);
      }
    },
    pillars(a, t) {
      if (t === 0) return bareStumps(a, "MIDDLE_EASTERN");
      if (t >= 3) {                                                                             // grand horseshoe arch spanning the pillars
        const f = t === 4 ? GOLD : TEAL;
        a.path("M80 60V44A48 46 0 1 1 176 44V60H164V46A36 34 0 1 0 92 46V60Z", a.lin([f.l, f.m]), { stroke: f.d, sw: 1 });
        for (let k = 0; k < 9; k++) { const an = Math.PI * (0.1 + (k / 8) * 0.8); a.circle(128 - Math.cos(an) * 42, 46 - Math.sin(an) * 40, 2, k % 2 ? "#ffffff" : AZURE.m); }
      }
      for (const x of [80, 160]) {
        if (t === 1) {                                                                            // palm trunks
          a.rect(x + 3, 8, 10, FLOOR - 8, a.lin([ME_MAT[1].l, ME_MAT[1].d], 1, 0));
          for (let y = 14; y < FLOOR; y += 8) a.path(`M${x + 3} ${y}l5 4 5 -4`, "none", { stroke: ME_MAT[1].d, sw: 1 });
          continue;
        }
        const pal = t === 4 ? GOLD : ME_MAT[t === 2 ? 2 : 3];
        column(a, x + 2, 12, t >= 3 ? 60 : 16, pal, { cap: "muqarnas", bands: t === 4 ? [80, 100] : undefined });
      }
    },
    throne(a, t) {
      if (t === 0) return;
      const cushion = (x, y, w, h, c) => a.rect(x, y, w, h, a.lin([c.hi || c.l, c.m, c.d]), { rx: h / 2.5 });
      if (t === 1) {                                                                            // floor cushions
        cushion(104, 110, 48, 14, RED); cushion(110, 98, 36, 14, { l: "#e2bf92", m: "#c99a6c", d: "#8a6a42" });
        return;
      }
      if (t === 2) {                                                                            // low divan
        a.rect(92, 98, 72, 26, a.lin([ME_MAT[2].l, ME_MAT[2].d]), { rx: 3 });
        cushion(96, 88, 30, 14, TEAL); cushion(130, 88, 30, 14, TEAL); cushion(94, 98, 68, 10, RED);
        return;
      }
      const p = t === 4 ? GOLD : ME_MAT[3];
      if (t === 4) {                                                                            // peacock fan
        for (let k = 0; k < 11; k++) {
          const an = Math.PI * (0.05 + (k / 10) * 0.9);
          const ex = 128 - Math.cos(an) * 46, ey = 92 - Math.sin(an) * 60;
          a.path(`M128 92L${(ex - 6).toFixed(1)} ${ey.toFixed(1)}Q${ex.toFixed(1)} ${(ey - 8).toFixed(1)} ${(ex + 6).toFixed(1)} ${ey.toFixed(1)}Z`, a.lin([TEAL.l, TEAL.d]), { op: 0.95 });
          a.circle(ex, ey, 4, AZURE.m, { stroke: GOLD.l, sw: 1 }); a.circle(ex, ey, 1.5, GOLD.hi);
        }
      }
      a.path(opening("ogee", 106, 48, 44, 54), a.lin([p.hi, p.m]), { stroke: p.d, sw: 1.2 });
      a.path(opening("ogee", 112, 56, 32, 42), a.lin([RED.l, RED.d]));
      a.rect(98, 92, 60, 14, a.lin([p.l, p.m]), { stroke: p.d, sw: 1, rx: 2 });
      cushion(100, 86, 56, 10, t === 4 ? TEAL : RED);
      a.rect(94, 106, 68, 9, a.lin([p.l, p.d])); a.rect(88, 115, 80, 9, a.lin([p.m, p.d]));
      if (t === 4) { a.circle(128, 70, 4, a.rad(["#ffd0d8", RED.l, RED.d])); }
    },
    treasures(a, t) {
      if (t === 0) return;
      const clay = { d: "#7a4a2a", m: "#b06a3a", l: "#d8905a" };
      a.path("M14 124Q8 108 16 100Q22 96 28 100Q36 108 30 124Z", a.lin([clay.l, clay.d], 1, 0)); a.rect(17, 96, 8, 5, clay.m);
      if (t >= 2) {                                                                              // brass lantern
        a.line(230, 0, 230, 58, "#5a4a2a", 1);
        a.path("M222 60h16l-3 22h-10z", a.lin([GOLD.l, GOLD.d]), { stroke: GOLD.d, sw: 0.6 });
        a.circle(230, 71, 4, "#fff2b0", { op: 0.9 }); a.path("M226 60l4 -6 4 6z", GOLD.m);
      }
      if (t >= 3) {                                                                              // blue urns
        for (const x of [40, 206]) {
          a.path(`M${x} 124Q${x - 8} 104 ${x + 2} 94H${x + 14}Q${x + 24} 104 ${x + 16} 124Z`, a.lin([AZURE.l, AZURE.d], 1, 0));
          a.path(`M${x + 2} 104h12`, "none", { stroke: "#ffffff", sw: 1.5 });
        }
      }
      if (t === 4) {                                                                              // oil lamp and gold
        a.path("M60 120q10 -8 22 -2l8 -6-4 8q-8 6 -26 0z", a.lin([GOLD.hi, GOLD.d]));
        a.path("M180 124Q190 110 200 124Z", a.lin([GOLD.hi, GOLD.d])); a.circle(190, 116, 2, RED.l);
      }
    },
  };

  // ---- ASIAN --------------------------------------------------------------------------------
  const AS_MAT = [null,
    { d: "#6b6a2e", m: "#9a9644", l: "#c4be6a", hi: "#e6dfa0" },
    { d: "#5a1010", m: "#9a1c1c", l: "#c83030", hi: "#e86a5a" },
    { d: "#1f5a44", m: "#2f8a66", l: "#5cbf94", hi: "#a8e6c8" },
    { d: "#5a1010", m: "#9a1c1c", l: "#c83030", hi: "#e86a5a" }];
  const PAPER = ["#efe6cf", "#ddd0b0"];

  const asian = {
    walls(a, t) {
      if (t === 0) return bareWalls(a, "ASIAN");
      if (t === 1) {                                                                            // shoji screens
        a.rect(0, 0, 256, FLOOR, a.lin(PAPER));
        for (let x = 0; x <= 256; x += 8) a.line(x, 0, x, FLOOR, AS_MAT[1].m, 0.8);
        for (let y = 0; y <= FLOOR; y += 12) a.line(0, y, 256, y, AS_MAT[1].m, 0.8);
        for (let x = 0; x <= 256; x += 64) a.rect(x - 2, 0, 4, FLOOR, AS_MAT[1].d);
        return;
      }
      const p = AS_MAT[t];
      if (t === 2) {
        a.rect(0, 0, 256, FLOOR, a.lin(["#4a2a1a", "#2e180e"]));
        for (let x = 0; x < 256; x += 32) a.rect(x + 3, 10, 26, 70, "none", { stroke: "#6a4028", sw: 1.5 });
        a.rect(0, 0, 256, 8, p.m);
        return;
      }
      a.rect(0, 0, 256, FLOOR, t === 4 ? a.lin([GOLD.hi, GOLD.l, GOLD.m]) : a.lin([p.l, p.m]));
      for (let x = 0; x < 256; x += 64) a.rect(x + 4, 8, 56, 76, "none", { stroke: t === 4 ? GOLD.d : GOLD.m, sw: 1.5 });
      for (const [cx, cy] of [[32, 30], [224, 34], [100, 70], [160, 66]]) {                    // cloud scrolls
        a.path(`M${cx - 10} ${cy}q0 -8 8 -6q2 -8 10 -4q8 -2 8 6q6 2 0 6h-22q-6 -1 -4 -2z`, "none", { stroke: t === 4 ? RED.m : GOLD.l, sw: 1.2 });
      }
      if (t === 4) {                                                                             // dragon mural
        a.path("M20 60q30 -40 60 -10t60 0q30 -30 60 0t40 -20", "none", { stroke: RED.m, sw: 5, lj: "round" });
        a.path("M20 60q30 -40 60 -10t60 0q30 -30 60 0t40 -20", "none", { stroke: RED.hi, sw: 1.5, lj: "round" });
        a.circle(240, 30, 5, RED.m); a.circle(242, 29, 1.2, GOLD.hi);
      }
      a.rect(0, 88, 256, 36, a.lin([AS_MAT[2].m, AS_MAT[2].d]));
    },
    windows(a, t) {
      if (t === 0) return;
      for (const x of [24, 192]) {
        const cx = x + 20, cy = 54;
        const frame = t === 1 ? AS_MAT[1] : t === 4 ? GOLD : AS_MAT[t];
        a.circle(cx, cy, 25, frame.m, { stroke: frame.d, sw: 2 });
        a.circle(cx, cy, 20, t === 1 ? PAPER[0] : a.lin(SKY));
        if (t >= 3) {                                                                             // blossom branch
          a.path(`M${cx - 18} ${cy + 8}q12 -4 20 -14t16 -8`, "none", { stroke: "#4a2a1a", sw: 1.5 });
          for (const [bx, by] of [[cx - 6, cy + 1], [cx + 4, cy - 8], [cx + 12, cy - 13], [cx - 12, cy + 6]]) a.circle(bx, by, 2.2, "#f4a8c0");
        }
        for (let k = -2; k <= 2; k++) { a.line(cx + k * 8, cy - 20, cx + k * 8, cy + 20, frame.d, 1, 0.7); a.line(cx - 20, cy + k * 8, cx + 20, cy + k * 8, frame.d, 1, 0.7); }
        a.circle(cx, cy, 20, "none", { stroke: frame.d, sw: 2 });
        if (t === 4) {                                                                            // red lanterns
          const lx = x === 24 ? 68 : 184;
          a.line(lx, 0, lx, 20, GOLD.d, 1);
          a.ellipse(lx, 30, 8, 10, a.rad([RED.hi, RED.l, RED.m]), { stroke: GOLD.m, sw: 1 });
          a.rect(lx - 4, 19, 8, 2, GOLD.m); a.rect(lx - 4, 39, 8, 2, GOLD.m); a.line(lx, 41, lx, 48, GOLD.l, 1);
        }
      }
    },
    floor(a, t) {
      if (t === 0) return bareFloor(a, "ASIAN");
      if (t === 1) {                                                                              // tatami
        a.rect(0, FLOOR, 256, 36, a.lin(["#c8c07a", "#a8a058"]));
        for (let x = 0; x < 256; x += 64) a.rect(x, FLOOR, 64, 36, "none", { stroke: "#3a3a1a", sw: 2 });
        return;
      }
      if (t === 2) {
        for (let y = FLOOR; y < 160; y += 6) a.rect(0, y, 256, 6, a.lin(["#7a4a2a", "#5a3018"]));
        a.rect(0, FLOOR, 256, 36, a.lin(["#ffffff", "#000000"]), { op: 0.1 });
        return;
      }
      a.rect(0, FLOOR, 256, 36, a.lin(["#9a9aa2", "#6a6a74"]));
      for (let y = FLOOR, row = 0; y < 160; y += 12, row++) for (let x = row % 2 ? -24 : 0; x < 256; x += 48) a.rect(x + 1, y + 1, 46, 10, "none", { stroke: "#5a5a64", sw: 1 });
      if (t === 4) a.path(`M104 ${FLOOR}H152L164 160H92Z`, a.lin([RED.l, RED.d]), { stroke: GOLD.m, sw: 2.5 });
    },
    pillars(a, t) {
      if (t === 0) return bareStumps(a, "ASIAN");
      for (const x of [80, 160]) {
        if (t === 1) {                                                                            // bamboo poles
          a.rect(x + 3, 4, 10, FLOOR - 4, a.lin([AS_MAT[1].hi, AS_MAT[1].m, AS_MAT[1].d], 1, 0));
          for (let y = 16; y < FLOOR; y += 18) a.rect(x + 2, y, 12, 2, AS_MAT[1].d);
          continue;
        }
        const red = t === 3 ? AS_MAT[2] : AS_MAT[2];
        column(a, x, 16, 18, red, { cap: "bracket", capFill: t >= 3 ? (t === 4 ? GOLD.l : AS_MAT[3].m) : AS_MAT[2].d, bands: t >= 3 ? [40, 100] : undefined });
        if (t === 4) a.path(`M${x} 110q16 -8 0 -16t0 -16q16 -8 0 -16t0 -16`, "none", { stroke: GOLD.l, sw: 3, lj: "round" });   // dragon coil
      }
    },
    throne(a, t) {
      if (t === 0) return;
      if (t === 1) {                                                                               // mat and cushion
        a.rect(98, 116, 60, 8, "#c8c07a", { stroke: "#3a3a1a", sw: 1 });
        a.ellipse(128, 112, 18, 6, a.lin([RED.l, RED.d]));
        return;
      }
      if (t >= 3) {                                                                                // folding screen
        const sp = t === 4 ? GOLD : AS_MAT[3];
        for (let k = 0; k < 5; k++) a.rect(84 + k * 18, 30, 18, 70, a.lin([sp.hi, sp.l, sp.m]), { stroke: t === 4 ? RED.m : GOLD.m, sw: 1 });
        if (t === 4) a.path("M88 60q20 -20 40 0t40 0", "none", { stroke: RED.m, sw: 3, lj: "round" });
      }
      const wood = t === 2 ? { d: "#3a1a0a", m: "#6a3418", l: "#9a5228", hi: "#c07a44" } : t === 4 ? GOLD : AS_MAT[2];
      a.rect(108, 66, 40, 36, a.lin([wood.hi, wood.m]), { stroke: wood.d, sw: 1, rx: 2 });          // back
      a.path("M104 66q24 -14 48 0", wood.l, { stroke: wood.d, sw: 1 });
      a.rect(98, 84, 12, 22, wood.m, { stroke: wood.d, sw: 0.8 }); a.rect(146, 84, 12, 22, wood.m, { stroke: wood.d, sw: 0.8 });
      a.rect(102, 96, 52, 10, a.lin([RED.l, RED.d]), { rx: 2 });
      a.rect(96, 106, 64, 8, wood.l, { stroke: wood.d, sw: 0.8 }); a.rect(90, 114, 76, 10, a.lin([wood.m, wood.d]));
      if (t >= 3) a.circle(128, 80, 6, t === 4 ? a.rad([AS_MAT[3].hi, AS_MAT[3].m]) : GOLD.l, { stroke: GOLD.d, sw: 1 });
      if (t === 4) for (const hx of [104, 152]) { a.circle(hx, 82, 5, GOLD.l, { stroke: GOLD.d, sw: 1 }); a.circle(hx + (hx < 128 ? -2 : 2), 81, 1.2, RED.m); }   // dragon heads
    },
    treasures(a, t) {
      if (t === 0) return;
      a.rect(16, 114, 18, 10, "#5a3a2a"); a.path("M25 114q-14 -8 -6 -20q4 10 6 -6q4 12 10 6q2 12 -10 20z", "#3a7a3a");   // bonsai
      if (t >= 2) {                                                                                 // blue and white vase
        a.path("M214 124Q204 108 214 96V90H226V96Q236 108 226 124Z", a.lin(["#ffffff", "#d8e2f0"], 1, 0), { stroke: "#2a4a8a", sw: 0.8 });
        a.path("M210 110q10 -6 20 0", "none", { stroke: "#2a4a8a", sw: 1.5 }); a.circle(220, 102, 2, "#2a4a8a");
      }
      if (t >= 3) {                                                                                 // bronze incense burner
        a.path("M40 108h28l-4 12h-20z", a.lin(["#a8884a", "#5a4a2a"])); a.rect(43, 120, 3, 4, "#5a4a2a"); a.rect(62, 120, 3, 4, "#5a4a2a");
        a.path("M52 104q-4 -10 2 -16t-2 -14", "none", { stroke: "#c8c8c8", sw: 1, op: 0.7 });
      }
      if (t === 4) {                                                                                // gold ingots and jade
        for (const [x, y] of [[76, 118], [88, 120], [180, 118], [192, 120]]) a.path(`M${x} ${y}q6 -6 12 0q-2 4 -6 4q-4 0 -6 -4z`, a.lin([GOLD.hi, GOLD.d]));
        a.circle(200, 112, 4, a.rad([AS_MAT[3].hi, AS_MAT[3].m]));
      }
    },
  };

  const STYLES = { CASTLE: castle, PRESIDENTIAL: presidential, MIDDLE_EASTERN: middleEastern, ASIAN: asian };

  return function palaceArt(palace) {
    const kit = STYLES[palace?.style] || castle;
    const t = (part) => Math.max(0, Math.min(4, palace?.[part] ?? 0));
    const a = new Art();
    kit.walls(a, t("walls"));
    kit.windows(a, t("windows"));
    kit.floor(a, t("floor"));
    kit.pillars(a, t("pillars"));
    kit.throne(a, t("throne"));
    kit.treasures(a, t("treasures"));
    return a.svg();
  };
})();
