// Mars mission art: a 256x160 launch pad at night with the spaceship stacked up part by part.
// Built parts are drawn solid; parts still to build are dashed blueprint outlines.
// spaceshipArt(built, {tall}) returns an <svg> string; built = list of SPACE_* action types.
// tall doubles the height with more night sky above the pad (the launch screen uses it).

const SPACE_PARTS = [
  "SPACE_LAUNCH_TOWER", "SPACE_ENGINES", "SPACE_FIRST_STAGE", "SPACE_SECOND_STAGE", "SPACE_THIRD_STAGE",
  "SPACE_PRIMARY_BOOSTERS", "SPACE_SECONDARY_BOOSTERS", "SPACE_COMMAND_MODULE", "SPACE_LANDING_MODULE",
  "SPACE_SUPPORT_MODULE",
];

const spaceshipArt = (() => {
  let seq = 0;
  const GROUND = 140;
  const BLUEPRINT = "#7fb8ff";

  function spaceshipArt(built = [], { tall = false } = {}) {
    const top = tall ? -160 : 0;                 // the tall view extends the sky upward
    const id = `sa${++seq}`;
    let n = 0;
    const defs = [];
    const body = [];
    const has = new Set(built);

    const lin = (stops, x2 = 1, y2 = 0) => {
      const gid = `${id}g${++n}`;
      defs.push(`<linearGradient id="${gid}" x1="0" y1="0" x2="${x2}" y2="${y2}">${stops.map((c, i) =>
        `<stop offset="${(i / (stops.length - 1)).toFixed(2)}" stop-color="${c}"/>`).join("")}</linearGradient>`);
      return `url(#${gid})`;
    };
    const shape = (tag, attrs) => body.push(`<${tag} ${Object.entries(attrs).map(([k, v]) => `${k}="${v}"`).join(" ")}/>`);

    /** Draws a part: solid shapes when built, a dashed blueprint of the same outline when not. */
    const part = (type, shapes) => {
      const done = has.has(type);
      for (const s of shapes) {
        if (!done && s.detail) continue;
        const attrs = { ...s.attrs };
        if (done) {
          attrs.fill = s.fill;
          if (s.stroke) { attrs.stroke = s.stroke; attrs["stroke-width"] = s.sw ?? 0.8; }
        } else {
          Object.assign(attrs, { fill: "none", stroke: BLUEPRINT, "stroke-width": 0.8, "stroke-dasharray": "3 2", opacity: 0.6 });
        }
        shape(s.tag, attrs);
      }
    };
    const rect = (x, y, w, h, fill, extra = {}) => ({ tag: "rect", attrs: { x, y, width: w, height: h, ...(extra.rx ? { rx: extra.rx } : {}) }, fill, ...extra });
    const path = (d, fill, extra = {}) => ({ tag: "path", attrs: { d }, fill, ...extra });
    const circle = (cx, cy, r, fill, extra = {}) => ({ tag: "circle", attrs: { cx, cy, r }, fill, ...extra });

    // ---- scene ---------------------------------------------------------------------
    shape("rect", { x: 0, y: top, width: 256, height: 160 - top,
      fill: lin(tall ? ["#02030e", "#070a22", "#1a1a48", "#4a2a52"] : ["#070a22", "#1a1a48", "#4a2a52"], 0, 1) });
    for (let k = 0; k < (tall ? 90 : 40); k++) {
      const x = (k * 97) % 256, y = top + ((k * 53) % (110 - top));
      shape("circle", { cx: x, cy: y, r: k % 5 ? 0.5 : 0.9, fill: "#ffffff", opacity: 0.4 + (k % 3) * 0.2 });
    }
    const [mx, my, mr] = tall ? [196, -112, 18] : [224, 26, 13];                                        // Mars
    if (tall) shape("circle", { cx: mx, cy: my, r: mr + 6, fill: "#ff7a50", opacity: 0.12 });
    shape("circle", { cx: mx, cy: my, r: mr, fill: lin(["#f08050", "#b83a1c", "#6a1a0c"], 1, 1) });
    shape("circle", { cx: mx - mr * 0.38, cy: my - mr * 0.3, r: mr * 0.19, fill: "#8a2a12", opacity: 0.6 });
    shape("circle", { cx: mx + mr * 0.38, cy: my + mr * 0.38, r: mr * 0.14, fill: "#8a2a12", opacity: 0.6 });
    shape("ellipse", { cx: 128, cy: GROUND + 10, rx: 160, ry: 18, fill: lin(["#3a3a46", "#1c1c24"], 0, 1) });
    shape("rect", { x: 0, y: GROUND, width: 256, height: 20, fill: lin(["#2c2c36", "#16161c"], 0, 1) });
    shape("rect", { x: 72, y: GROUND - 4, width: 112, height: 6, fill: lin(["#6a6a76", "#3a3a44"], 0, 1) });   // pad
    shape("rect", { x: 112, y: GROUND - 2, width: 32, height: 4, fill: "#0e0e14" });                         // flame trench

    const white = lin(["#9aa0ac", "#f4f6fa", "#ffffff", "#c8ccd6", "#7a808c"]);
    const metal = lin(["#3a3a44", "#9a9aa8", "#55555f"]);
    const dark = lin(["#1a1a22", "#4a4a56", "#22222a"]);
    const gold = lin(["#8a5c00", "#f6c840", "#fff2a8", "#c8930a"]);
    const red = lin(["#6a0d1c", "#d0303a", "#8a1222"]);

    // ---- launch tower (right of the rocket) --------------------------------------------
    const tower = [rect(186, 18, 3, GROUND - 22, red), rect(199, 18, 3, GROUND - 22, red)];
    for (let y = 22; y < GROUND - 8; y += 12) {
      tower.push(path(`M187 ${y}L201 ${y + 12}M201 ${y}L187 ${y + 12}`, "none", { stroke: "#c8303a", sw: 1 }));
      tower.push(rect(186, y, 16, 1.5, "#e8e8ec", { detail: true }));
    }
    for (const y of [36, 70, 100]) tower.push(rect(146, y, 40, 3, metal), rect(146, y + 3, 2, 4, dark, { detail: true }));
    tower.push(rect(182, 14, 24, 5, red), circle(194, 11, 2, "#ff4040", { detail: true }));
    part("SPACE_LAUNCH_TOWER", tower);

    // Everything from here to the capsule lifts off together (class "ship"); flames and smoke
    // only show while a launch plays (class "launching" on a parent, see styles.css).
    body.push('<g class="ship">');
    const flame = (cx, w, len) => shape("path", {
      class: "ship-flame", fill: lin(["#fff6c0", "#ffb020", "#ff5a10", "rgba(255,60,0,0)"], 0, 1),
      // Positioned in the path itself: the flicker animation owns the transform.
      d: `M${cx - w / 2} 136Q${cx} ${136 + len * 1.2} ${cx + w / 2} 136Z`,
    });
    for (const cx of [118, 128, 138]) flame(cx, 7, 26);
    for (const cx of [103.5, 152.5]) flame(cx, 9, 22);
    for (const cx of [89, 167]) flame(cx, 7, 16);

    // ---- secondary boosters (outer pair) -----------------------------------------------
    const booster = (x, w, top, bottom) => [
      rect(x, top, w, bottom - top, white, { stroke: "#5a606c", sw: 0.5 }),
      path(`M${x} ${top}Q${x + w / 2} ${top - w * 1.1} ${x + w} ${top}Z`, white, { stroke: "#5a606c", sw: 0.5 }),
      path(`M${x + 1} ${bottom}h${w - 2}l1.5 5h${-(w + 1)}z`, dark),
      rect(x, top + 6, w, 2, "#2a2a34", { detail: true }), rect(x, bottom - 8, w, 2, "#2a2a34", { detail: true }),
    ];
    part("SPACE_SECONDARY_BOOSTERS", [...booster(84, 10, 98, 130), ...booster(162, 10, 98, 130)]);
    // ---- primary boosters --------------------------------------------------------------
    part("SPACE_PRIMARY_BOOSTERS", [...booster(97, 13, 86, 131), ...booster(146, 13, 86, 131)]);

    // ---- engines: three bell nozzles under the first stage --------------------------------
    const nozzle = (cx) => path(`M${cx - 3} 126h6l3 10h-12z`, dark, { stroke: "#1a1a22", sw: 0.5 });
    part("SPACE_ENGINES", [nozzle(118), nozzle(128), nozzle(138), rect(112, 124, 32, 3, metal)]);

    // ---- first stage with fins -----------------------------------------------------------
    part("SPACE_FIRST_STAGE", [
      rect(112, 80, 32, 44, white, { stroke: "#5a606c", sw: 0.6 }),
      path("M112 108L104 126h8z", red), path("M144 108L152 126h-8z", red),
      rect(112, 86, 32, 4, "#23232c", { detail: true }), rect(112, 112, 16, 6, "#23232c", { detail: true }),
      rect(128, 112, 16, 6, "#e8e8ec", { detail: true }),
      path("M118 96h20", "none", { stroke: "#2f5aa6", sw: 2, detail: true }),
    ]);
    // ---- second stage ----------------------------------------------------------------------
    part("SPACE_SECOND_STAGE", [
      rect(115, 56, 26, 24, white, { stroke: "#5a606c", sw: 0.6 }),
      rect(115, 76, 26, 4, dark, { detail: true }), rect(115, 62, 13, 5, "#23232c", { detail: true }),
    ]);
    // ---- third stage -----------------------------------------------------------------------
    part("SPACE_THIRD_STAGE", [
      rect(118, 40, 20, 16, white, { stroke: "#5a606c", sw: 0.6 }),
      rect(118, 52, 20, 4, dark, { detail: true }), circle(128, 46, 2.5, "#2f5aa6", { detail: true }),
    ]);
    // ---- support module: supplies, with folded solar panels --------------------------------
    part("SPACE_SUPPORT_MODULE", [
      rect(119, 32, 18, 8, metal, { stroke: "#2a2a34", sw: 0.5 }),
      rect(111, 33, 7, 6, lin(["#1a2a6a", "#3a5ac8"]), { stroke: "#9ab0ff", sw: 0.4 }),
      rect(138, 33, 7, 6, lin(["#3a5ac8", "#1a2a6a"]), { stroke: "#9ab0ff", sw: 0.4 }),
    ]);
    // ---- landing module: gold foil and folded legs -----------------------------------------
    part("SPACE_LANDING_MODULE", [
      path("M120 32L122 22h12l2 10z", gold, { stroke: "#8a5c00", sw: 0.5 }),
      path("M120 32l-4 4M136 32l4 4", "none", { stroke: "#c8c8d0", sw: 1, detail: true }),
    ]);
    // ---- command module: the crew capsule with its escape tower -----------------------------
    part("SPACE_COMMAND_MODULE", [
      path("M122 22L128 8L134 22Z", white, { stroke: "#5a606c", sw: 0.6 }),
      path("M128 8V1", "none", { stroke: "#c8303a", sw: 1.2 }),
      circle(128, 16, 1.6, "#6fb2e0", { detail: true }),
    ]);

    body.push("</g>");
    for (const [x, r] of [[96, 10], [112, 13], [128, 15], [144, 13], [160, 10], [80, 8], [176, 8]]) {
      shape("circle", { class: "ship-smoke", cx: x, cy: GROUND - 2, r, fill: "#d8dce4" });
    }

    if (SPACE_PARTS.every((p) => has.has(p))) {                                                    // ready: venting steam
      for (const [x, r] of [[110, 7], [146, 8], [100, 5], [156, 6]]) shape("circle", { cx: x, cy: GROUND - 4, r, fill: "#e8ecf2", opacity: 0.5 });
    }
    return `<svg viewBox="0 ${top} 256 ${160 - top}"${tall ? ' class="tall"' : ""} aria-hidden="true"><defs>${defs.join("")}</defs>${body.join("")}</svg>`;
  }

  return spaceshipArt;
})();
