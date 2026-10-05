"use strict";

/* =====================================================================
   XMAS-GAME web client. Plain JS, no build step.
   Server is the source of truth: poll → render → post actions.
   ===================================================================== */

const $ = (id) => document.getElementById(id);

// ---------------------------------------------------------------------
// State (mirrored to localStorage so a refresh keeps you logged in)
// ---------------------------------------------------------------------

const STORAGE_KEY = "xmas.state.v1";
const PERSISTED = ["sessionToken", "accountId", "username", "gameId", "playerId", "isAdmin"];

const state = {
  baseUrl: "",          // same origin: the UI is served by game-app
  sessionToken: null,
  accountId: null,
  username: null,       // kept after logout to prefill the login form
  gameId: null,
  playerId: null,
  isAdmin: false,
  actions: [],          // action catalog, fetched once per game screen
  clockSkew: 0,         // server clock minus ours, in ms
};

function persist() {
  const saved = Object.fromEntries(PERSISTED.map((k) => [k, state[k]]));
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(saved)); } catch { /* private mode: ignore */ }
}

function restore() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY)) || {};
    for (const k of PERSISTED) if (k in saved) state[k] = saved[k];
  } catch { /* corrupt or blocked storage: start fresh */ }
}

function clearSession() {
  Object.assign(state, {
    sessionToken: null, accountId: null, gameId: null, playerId: null, isAdmin: false, actions: [],
  });
  persist();
}

// ---------------------------------------------------------------------
// API
// ---------------------------------------------------------------------

class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function api(method, path, body) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (state.sessionToken) headers["X-Session-Token"] = state.sessionToken;

  let res;
  try {
    res = await fetch(state.baseUrl + path, {
      method, headers, body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "Cannot reach the server. Check your connection.");
  }

  // Timers from the server (completesAt) are on its clock; remember how far ours is off.
  const serverDate = Date.parse(res.headers.get("Date") || "");
  if (serverDate) {
    const skew = serverDate - Date.now();
    state.clockSkew = Math.abs(skew) > 1500 ? skew : 0;   // Date has 1s resolution: ignore jitter
  }

  const text = await res.text();
  let data = null;
  if (text) { try { data = JSON.parse(text); } catch { /* non-JSON body */ } }

  if (!res.ok) {
    // A 401 while holding a token means the session is gone: back to login.
    if (res.status === 401 && state.sessionToken) logout();
    throw new ApiError(res.status, errorMessage(res.status, data));
  }
  return data;
}

function errorMessage(status, data) {
  if (data && typeof data.message === "string" && data.message) return data.message;
  // Bean-validation failures come back in Spring's default shape without `message`.
  if (status === 400) return "The server rejected that request. Check your input.";
  if (status === 403) return "You are not allowed to do that.";
  if (status === 404) return "Not found.";
  return `Server error (HTTP ${status}).`;
}

// ---------------------------------------------------------------------
// Toasts
// ---------------------------------------------------------------------

const MAX_TOASTS = 3;

function toast(message, kind = "info") {
  const el = document.createElement("div");
  el.className = `toast ${kind}`;
  el.setAttribute("role", kind === "error" ? "alert" : "status");
  if (kind === "error") {
    const head = document.createElement("span");
    head.className = "hd";
    head.textContent = "GURU MEDITATION";
    el.append(head);
  }
  el.append(document.createTextNode(message));
  el.addEventListener("click", () => el.remove());

  const box = $("toasts");
  box.append(el);
  while (box.children.length > MAX_TOASTS) box.firstElementChild.remove();
  setTimeout(() => el.remove(), kind === "info" ? 3500 : 6000);   // errors and alerts linger
}

const toastError = (e) => toast(e.message || String(e), "error");

// ---------------------------------------------------------------------
// Theme (Workbench 1.3 / 2.0 / Copper) — a per-device preference
// ---------------------------------------------------------------------

const THEME_KEY = "xmas.theme";
const THEMES = { wb: "Workbench 1.3", wb2: "Workbench 2.0", cu: "Copper" };

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  const title = getComputedStyle(document.documentElement).getPropertyValue("--title").trim();
  document.querySelector('meta[name="theme-color"]').content = title;
}

function cycleTheme() {
  const keys = Object.keys(THEMES);
  const next = keys[(keys.indexOf(document.documentElement.dataset.theme) + 1) % keys.length];
  try { localStorage.setItem(THEME_KEY, next); } catch { /* ignore */ }
  applyTheme(next);
  toast(`Theme: ${THEMES[next]}`);
}

function initTheme() {
  applyTheme(document.documentElement.dataset.theme || "wb");
  // Without a saved choice, keep following the system light/dark setting.
  matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (e) => {
    let saved = null;
    try { saved = localStorage.getItem(THEME_KEY); } catch { /* ignore */ }
    if (!saved) applyTheme(e.matches ? "cu" : "wb");
  });
  $("themeBtn").addEventListener("click", cycleTheme);
}

// ---------------------------------------------------------------------
// Screens
// ---------------------------------------------------------------------

const SCREEN_TITLES = {
  auth: "XMAS-GAME WORKBENCH",
  lobby: "XMAS-GAME · LOBBY",
  game: "XMAS-GAME",
};

function show(screen) {
  for (const id of Object.keys(SCREEN_TITLES)) $(id).hidden = id !== screen;
  $("screenTitle").textContent = SCREEN_TITLES[screen];
  if (screen !== "game") document.body.classList.remove("tabbed");
  window.scrollTo(0, 0);
}

/** Disables the buttons while `fn` runs, so a double tap can't submit twice. */
async function busy(buttons, fn) {
  buttons.forEach((b) => { b.disabled = true; });
  try { return await fn(); } finally { buttons.forEach((b) => { b.disabled = false; }); }
}

// ---------------------------------------------------------------------
// Polling
// ---------------------------------------------------------------------

/**
 * Calls `fn` every `intervalMs` (a number, or a function returning one) until stopped. Never overlaps calls, skips
 * rounds while the tab is hidden, and toasts a failure only once per streak
 * so a flaky connection doesn't flood the screen.
 */
function makePoller(fn, intervalMs) {
  let running = false;
  let timer = null;
  let failing = false;

  async function loop() {
    if (!running) return;
    if (!document.hidden) {
      try {
        await fn();
        failing = false;
      } catch (e) {
        if (!failing && running && e.status !== 401) toastError(e);
        failing = true;
      }
    }
    if (running) timer = setTimeout(loop, typeof intervalMs === "function" ? intervalMs() : intervalMs);
  }

  return {
    start() { if (!running) { running = true; loop(); } },
    stop() { running = false; clearTimeout(timer); },
    now() { clearTimeout(timer); loop(); },
  };
}

const pollers = [];
function stopAllPolling() { pollers.forEach((p) => p.stop()); }

// Catch up immediately when the phone wakes the tab again.
document.addEventListener("visibilitychange", () => {
  if (!document.hidden) pollers.forEach((p) => p.now());
});

// ---------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------

const UUID_RE = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;

/** "NORTH_KOREA" → "NORTH KOREA" */
const formatNation = (nation) => (nation ? nation.replaceAll("_", " ") : "");

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

/** Clipboard API needs a secure origin; phones on the LAN use plain http, so fall back. */
async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch { /* fall through */ }
  const ta = el("textarea");
  ta.value = text;
  ta.setAttribute("readonly", "");
  ta.style.cssText = "position:fixed;top:0;left:0;opacity:0";
  document.body.append(ta);
  ta.select();
  let ok = false;
  try { ok = document.execCommand("copy"); } catch { /* unsupported */ }
  ta.remove();
  return ok;
}

// ---------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------

async function login(username, password) {
  const { accountId, sessionToken } = await api("POST", "/account/login", { username, password });
  Object.assign(state, { accountId, sessionToken, username });
  persist();
}

async function register(username, password) {
  await api("POST", "/account/register", { username, password });
  await login(username, password);
}

function logout() {
  stopAllPolling();
  if (!$("sheet").hidden) closeSheet();
  clearSession();
  showAuth();
}

function showAuth() {
  $("username").value = state.username || "";
  $("password").value = "";
  show("auth");
  scheduleNuke();
}

// A bit of fun: a few seconds after the title screen shows, the city gets nuked.
// Once per page load, and never for players who ask for reduced motion.
const NUKE_DELAY_MS = 2500;
let nukeTimer = null;

function scheduleNuke() {
  if (nukeTimer || matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  nukeTimer = setTimeout(() => {
    if (!$("auth").hidden) $("auth").classList.add("nuked");
  }, NUKE_DELAY_MS);
}

function initAuth() {
  const form = $("authForm");
  const buttons = [$("loginBtn"), $("registerBtn")];
  const credentials = () => [$("username").value.trim(), $("password").value];

  const run = (fn, welcome) => busy(buttons, async () => {
    try {
      await fn(...credentials());
      $("password").value = "";
      toast(welcome());
      enterLobby();
    } catch (e) {
      toastError(e);
    }
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    run(login, () => `Welcome back, ${state.username}.`);
  });
  $("registerBtn").addEventListener("click", () => {
    if (!form.reportValidity()) return;
    run(register, () => `Account created. Welcome, ${state.username}.`);
  });
}

// ---------------------------------------------------------------------
// Lobby
// ---------------------------------------------------------------------

const MAX_NATIONS = 13;
const lobbyPoller = makePoller(pollLobby, 2000);
pollers.push(lobbyPoller);

/** After login: back into the current game if we have one, else the menu. */
function enterLobby() {
  $("whoami").textContent = state.username || "?";
  if (state.gameId && state.playerId) openRoom();
  else openMenu();
}

function openMenu() {
  lobbyPoller.stop();
  $("lobbyMenu").hidden = false;
  $("lobbyRoom").hidden = true;
  show("lobby");
}

function openRoom() {
  $("joinCodeBox").textContent = state.gameId;
  $("adminStart").hidden = !state.isAdmin;
  $("waitHint").hidden = state.isAdmin;
  $("roster").replaceChildren();
  $("lobbyMenu").hidden = true;
  $("lobbyRoom").hidden = false;
  show("lobby");
  lobbyPoller.start();
}

function leaveGame() {
  Object.assign(state, { gameId: null, playerId: null, isAdmin: false, actions: [] });
  persist();
  openMenu();
}

async function createGame() {
  const game = await api("POST", "/games");
  await joinGame(game.id);
}

async function joinGame(gameId) {
  let player;
  try {
    player = await api("POST", `/games/${gameId}/players`);
  } catch (e) {
    // Rejoining (new phone, cleared storage): the server refuses a second join
    // ("already joined", or "no longer accepting" once started), so find ourselves in the roster.
    if (e.status !== 400) throw e;
    const roster = await api("GET", `/games/${gameId}/players`);
    player = roster.find((p) => p.username === state.username);
    if (!player) throw e;
  }
  Object.assign(state, { gameId, playerId: player.id, isAdmin: player.admin });
  persist();
  openRoom();
}

async function pollLobby() {
  let game, roster;
  try {
    [game, roster] = await Promise.all([
      api("GET", `/games/${state.gameId}`),
      api("GET", `/games/${state.gameId}/players`),
    ]);
  } catch (e) {
    if (e.status === 404) {         // game vanished (e.g. dev server restarted)
      toast("That game no longer exists.", "error");
      leaveGame();
      return;
    }
    throw e;
  }
  if (game.status !== "LOBBY") {
    lobbyPoller.stop();
    enterGame(game);
    return;
  }
  renderRoster(roster);
}

function renderRoster(roster) {
  const sorted = [...roster].sort((a, b) => Number(b.admin) - Number(a.admin));
  $("roster").replaceChildren(...sorted.map((p) => {
    const li = el("li");
    li.append(el("span", "name", p.username));
    if (p.nation) li.append(el("span", "nation", formatNation(p.nation)));
    if (p.admin) li.append(el("span", "tag accent", "ADMIN"));
    if (p.id === state.playerId) li.append(el("span", "tag", "YOU"));
    return li;
  }));

  const nations = roster.filter((p) => !p.admin).length;
  $("rosterTitle").textContent = `PLAYERS ${roster.length}`;

  if (state.isAdmin) {
    let hint;
    if (nations === 0) hint = "Waiting for at least one player to join.";
    else if (nations > MAX_NATIONS) hint = `Too many players: at most ${MAX_NATIONS} nations.`;
    else hint = `${nations} ${nations === 1 ? "nation" : "nations"} ready. Nations are dealt at random.`;
    $("startHint").textContent = hint;
    $("startBtn").disabled = nations === 0 || nations > MAX_NATIONS;
  }
}

function joinLink() {
  return `${location.origin}${location.pathname}?join=${state.gameId}`;
}

async function shareLink() {
  const link = joinLink();
  if (navigator.share) {
    try {
      await navigator.share({ title: "XMAS-GAME", text: "Join my XMAS-GAME", url: link });
      return;
    } catch (e) {
      if (e.name === "AbortError") return;   // user closed the share sheet
    }
  }
  toast((await copyText(link)) ? "Join link copied." : link);
}

/** A ?join=<code> link pre-fills the join form, then disappears from the address bar. */
function takeJoinCodeFromUrl() {
  const params = new URLSearchParams(location.search);
  const code = params.get("join");
  if (!code) return;
  $("joinCode").value = code;
  params.delete("join");
  const rest = params.toString();
  history.replaceState(null, "", location.pathname + (rest ? `?${rest}` : "") + location.hash);
}

function initLobby() {
  $("logoutBtn").addEventListener("click", logout);

  $("createBtn").addEventListener("click", () => busy([$("createBtn"), $("joinBtn")], async () => {
    try {
      await createGame();
      toast("Game created. Share the join code.");
    } catch (e) { toastError(e); }
  }));

  $("joinForm").addEventListener("submit", (e) => {
    e.preventDefault();
    const match = $("joinCode").value.match(UUID_RE);   // accepts a bare code or a whole link
    if (!match) {
      toast("That doesn't look like a join code.", "error");
      return;
    }
    busy([$("createBtn"), $("joinBtn")], async () => {
      try {
        await joinGame(match[0].toLowerCase());
        $("joinCode").value = "";
        toast("Joined the game.");
      } catch (err) { toastError(err); }
    });
  });

  $("copyBtn").addEventListener("click", async () => {
    toast((await copyText(state.gameId)) ? "Join code copied." : "Copy failed. Long-press the code to copy it.");
  });
  $("shareBtn").addEventListener("click", shareLink);

  $("startBtn").addEventListener("click", () => busy([$("startBtn")], async () => {
    try {
      const game = await api("POST", `/games/${state.gameId}/start`);
      lobbyPoller.stop();
      enterGame(game);
    } catch (e) { toastError(e); }
  }));

  $("leaveBtn").addEventListener("click", () => {
    const warning = state.isAdmin
      ? "Back to the menu? You stay the admin of this game, and you can rejoin with the same code."
      : "Back to the menu? You stay on this game's player list, and you can rejoin with the same code.";
    if (confirm(warning)) leaveGame();
  });

  takeJoinCodeFromUrl();
}

// ---------------------------------------------------------------------
// Game screen
// ---------------------------------------------------------------------

const VULNERABLE_BELOW = 20;
const TAB_KEY = "xmas.tab";

/** Latest server data for the game screen. */
const game = {
  info: null,         // GameResponse
  me: null,           // PlayerStateResponse
  lastCash: null,     // to show how much the treasury just moved
};

// Poll roughly twice per tick so changes show up promptly.
const gamePoller = makePoller(pollGame, () => {
  const tick = game.info?.tickDurationMillis || 5000;
  return Math.min(5000, Math.max(2000, tick / 2));
});
pollers.push(gamePoller);

const fmtCash = (n) => "$" + Math.round(n).toLocaleString("en-US");
const popLevel = (p) => (p >= 50 ? "good" : p >= VULNERABLE_BELOW ? "warn" : "bad");
/** "RESEARCH_ADVANCED_FARMING" → "ADVANCED FARMING" */
const researchName = (type) => type.replace(/^RESEARCH_/, "").replaceAll("_", " ");

async function enterGame(info) {
  game.info = info;
  game.me = null;
  game.lastCash = null;
  $("cash").textContent = "–";
  $("cashDelta").textContent = "";
  $("adminRoster").replaceChildren();
  resetNews();
  resetWorld();
  show("game");
  selectTab(savedTab());
  // Admin view until we know better; the first poll sorts it out.
  applyRole(state.isAdmin);
  loadPending();
  loadCatalog();
  gamePoller.start();
  pendingTicker.start();
}

function leaveGameScreen() {
  gamePoller.stop();
  pendingTicker.stop();
  if (!$("sheet").hidden) closeSheet();
}

/** The action catalog never changes during a game: fetch it once. */
async function loadCatalog() {
  if (state.actions.length) return;
  try {
    state.actions = await api("GET", "/actions");
    if (game.me && game.info) renderPlayer(game.me, game.info);
  } catch (e) { toastError(e); }
}

async function pollGame() {
  let info, me, roster, feed;
  try {
    [info, me, roster, feed] = await Promise.all([
      api("GET", `/games/${state.gameId}`),
      api("GET", `/players/${state.playerId}`),
      api("GET", `/games/${state.gameId}/players`),
      state.isAdmin ? null : api("GET", `/players/${state.playerId}/news?limit=${NEWS_LIMIT}`),
    ]);
  } catch (e) {
    if (e.status === 404) {
      toast("That game no longer exists.", "error");
      leaveGameScreen();
      leaveGame();
      return;
    }
    throw e;
  }
  game.info = info;
  game.me = me;

  const adminView = me.player.admin && !me.player.nation;
  applyRole(adminView);
  if (adminView) renderAdmin(info, roster);
  else renderPlayer(me, info);
  if (feed) receiveNews(feed);
  if (!adminView) receiveRoster(roster);
  renderBanners(info, me.player, adminView);
}

function renderPlayer(me, info) {
  renderNation(me);
  renderActions(me, info);
  renderPending();
  if (sheet.def) updateSheet();   // cash moved while the dialog is open
}

/** The admin gets a control console instead of a nation. */
function applyRole(adminView) {
  $("hud").hidden = adminView;
  $("tabbar").hidden = adminView;
  $("adminPanel").hidden = !adminView;
  document.body.classList.toggle("tabbed", !adminView);
  if (adminView) {
    for (const t of ["nation", "act", "world", "news"]) $(`tab-${t}`).hidden = true;
    $("screenTitle").textContent = "XMAS-GAME · ADMIN";
  } else if ($("tab-nation").hidden && $("tab-act").hidden && $("tab-world").hidden && $("tab-news").hidden) {
    selectTab(savedTab());
  }
}

// ---------- Tabs ----------

function savedTab() {
  try { return localStorage.getItem(TAB_KEY) || "nation"; } catch { return "nation"; }
}

function selectTab(name) {
  for (const btn of document.querySelectorAll("#tabbar [data-tab]")) {
    const on = btn.dataset.tab === name;
    $(`tab-${btn.dataset.tab}`).hidden = !on;
    if (on) btn.setAttribute("aria-current", "page"); else btn.removeAttribute("aria-current");
  }
  try { localStorage.setItem(TAB_KEY, name); } catch { /* ignore */ }
  if (name === "news") onNewsTabOpened();
  else news.openedMark = null;
  window.scrollTo(0, 0);
}

// ---------- Nation header + tab ----------

const SECTIONS = [
  { key: "economy", title: "ECONOMY", fields: [["farms", "Farms"], ["factories", "Factories"], ["robotFactories", "Robot fac."], ["banks", "Banks"], ["fed", "Fed ind."]] },
  { key: "military", title: "MILITARY", fields: [["infantry", "Infantry"], ["tanks", "Tanks"], ["stealthBombers", "Bombers"], ["specialOps", "Spec. ops"], ["airDefenseBatteries", "Air def."], ["missileBatteries", "Missiles"]] },
  { key: "espionage", title: "ESPIONAGE", half: true, fields: [["spies", "Spies"], ["counterIntel", "Counter"]] },
  { key: "specialWeapons", title: "WEAPONS", half: true, fields: [["nukes", "Nukes"], ["espionageSatellites", "Satellites"]] },
  { key: "protection", title: "PROTECTION", fields: [["doublegangers", "Doubles"], ["bunkers", "Bunkers"], ["bodyguards", "Guards"], ["ninjaBodyguards", "Ninjas"]] },
];

function windowFrame(title, id) {
  const win = el("section", "win");
  const bar = el("div", "tbar");
  const heading = el("h3", "ttl hd", title);
  if (id) heading.id = id;
  bar.append(el("span", "gad"), heading, el("span", "grow"), el("span", "gad"));
  win.append(bar);
  return win;
}

/** Land: each economy building takes a plot; buildings under construction count as taken. */
const usesLand = (def) => def.category === "ECONOMY" && def.actionType.startsWith("BUILD_");
const freeLand = (me) => {
  const e = me.economy;
  return e?.landCapacity == null ? Infinity : Math.max(0, e.landCapacity - (e.landInUse ?? 0));
};

/** Builds the stat windows once; later polls only update the numbers. */
function buildNationSections() {
  const root = $("nationSections");
  let pair = null;
  for (const section of SECTIONS) {
    const win = windowFrame(section.title, `title-${section.key}`);
    const dl = el("dl", "stats");
    dl.style.setProperty("--cols", section.half ? 1 : 2);
    for (const [field, label] of section.fields) {
      const row = el("div", "stat");
      const dd = el("dd", "", "–");
      dd.id = `stat-${section.key}-${field}`;
      row.append(el("dt", "", label), dd);
      dl.append(row);
    }
    win.append(dl);
    if (section.half) {
      if (!pair) { pair = el("div", "half-pair"); root.append(pair); }
      pair.append(win);
    } else {
      pair = null;
      root.append(win);
    }
  }
}

/** Sets text and flashes it when a known value changes. */
function setValue(node, text) {
  if (node.textContent === text) return;
  const changed = node.textContent !== "–" && node.textContent !== "";
  node.textContent = text;
  if (changed) {
    node.classList.remove("bump");
    void node.offsetWidth;          // restart the animation
    node.classList.add("bump");
  }
}

function renderNation(me) {
  const p = me.player;
  const nation = formatNation(p.nation);
  $("screenTitle").textContent = `XMAS-GAME · ${nation}`;
  $("flag").dataset.nation = p.nation || "";
  $("flag").title = nation;
  $("nationName").textContent = nation;
  $("tickLabel").textContent = `TICK ${game.info?.currentTick ?? 0}`;

  // Treasury, with how much it moved since the last poll.
  $("cash").textContent = fmtCash(p.cash);   // no flash: it moves every tick; the delta says enough
  if (game.lastCash !== null && p.cash !== game.lastCash) {
    const diff = p.cash - game.lastCash;
    $("cashDelta").textContent = `${diff > 0 ? "+" : "−"}${fmtCash(Math.abs(diff))}`;
    $("cashDelta").classList.toggle("neg", diff < 0);
  }
  game.lastCash = p.cash;

  // Popularity
  const pop = p.popularity;
  $("popValue").textContent = `${pop}%`;
  $("popFill").style.width = `${Math.max(0, Math.min(100, pop))}%`;
  $("popFill").className = `popfill ${popLevel(pop)}`;
  $("popBar").setAttribute("aria-valuenow", pop);

  // Stat windows
  if (!$("nationSections").children.length) buildNationSections();
  if (me.economy?.landCapacity != null) {
    $("title-economy").textContent = `ECONOMY · LAND ${me.economy.landInUse}/${me.economy.landCapacity}`;
  }
  if (me.military?.upkeepPerTick != null) {
    $("title-military").textContent = `MILITARY · UPKEEP ${fmtCash(me.military.upkeepPerTick)}/TICK`;
  }
  const dgMax = state.actions.find((a) => a.actionType === "HIRE_DOUBLEGANGER")?.maxTotal;
  for (const section of SECTIONS) {
    const values = me[section.key] || {};
    for (const [field] of section.fields) {
      let text = String(values[field] ?? "–");
      if (field === "doublegangers" && dgMax) text += `/${dgMax}`;
      if (field === "fed") text = fedText(values);
      setValue($(`stat-${section.key}-${field}`), text);
    }
  }

  // Research
  const done = me.research || [];
  const total = state.actions.filter((a) => a.category === "RESEARCH").length;
  $("researchTitle").textContent = total ? `RESEARCH ${done.length}/${total}` : `RESEARCH ${done.length}`;
  const chips = done.length
    ? done.map((r) => el("span", "chip", researchName(r)))
    : [el("span", "dim", "Nothing researched yet.")];
  $("researchList").replaceChildren(...chips);
}

function renderBanners(info, p, adminView) {
  const banners = [];
  if (info.status === "PAUSED") banners.push(["info", "GAME PAUSED BY THE ADMIN"]);
  if (!adminView && p.eliminated) {
    banners.push(["bad", "YOUR GOVERNMENT HAS FALLEN. YOU CAN NO LONGER ACT."]);
  } else if (!adminView && p.frozenUntilTick > info.currentTick) {
    const left = p.frozenUntilTick - info.currentTick;
    banners.push(["warn", `ELECTION: ONLY MILITARY ACTIONS FOR ${left} ${left === 1 ? "TICK" : "TICKS"}`]);
  }
  $("banners").replaceChildren(...banners.map(([kind, text]) => {
    const b = el("div", `banner ${kind}`, text);
    b.setAttribute("role", "status");
    return b;
  }));
}

// ---------- Admin console ----------

function renderAdmin(info, roster) {
  const paused = info.status === "PAUSED";
  $("adminStatus").textContent = info.status;
  $("adminStatus").className = `tag ${paused ? "warn" : info.status === "RUNNING" ? "good" : ""}`;
  $("adminTick").textContent = `Tick ${info.currentTick}`;
  $("pauseBtn").hidden = paused;
  $("resumeBtn").hidden = !paused;

  const nations = roster.filter((p) => !p.admin);
  $("adminRosterTitle").textContent = `NATIONS ${nations.length}`;
  $("adminRoster").replaceChildren(...nations.map((p) => {
    const li = el("li", p.eliminated ? "out" : "");
    const who = el("span", "name");
    who.append(el("span", "", p.username), el("span", "nation", ` ${formatNation(p.nation)}`));
    const bar = el("span", "minibar");
    const fill = el("span", popLevel(p.popularity));
    fill.style.width = `${p.popularity}%`;
    bar.append(fill);
    li.append(who);
    if (p.eliminated) li.append(el("span", "tag bad", "FALLEN"));
    else li.append(bar, el("span", "pct", `${p.popularity}%`));
    return li;
  }));
}

async function setPaused(pause) {
  const btns = [$("pauseBtn"), $("resumeBtn")];
  await busy(btns, async () => {
    try {
      const info = await api("POST", `/games/${state.gameId}/${pause ? "pause" : "resume"}`);
      game.info = info;
      toast(pause ? "Game paused." : "Game resumed.");
      gamePoller.now();
    } catch (e) { toastError(e); }
  });
}

// ---------------------------------------------------------------------
// Actions: Act tab
// ---------------------------------------------------------------------

/** Display groups, in order. ATTACK and SPECIAL_WEAPON_ATTACK are DIPLOMACY in the
 *  catalog but players look for them under Army / Special weapons. */
const GROUPS = [
  ["ECONOMY", "ECONOMY"], ["DEMOLITION", "DEMOLITION"], ["ARMY", "ARMY"], ["SPECIAL_WEAPONS", "SPECIAL WEAPONS"],
  ["ESPIONAGE", "ESPIONAGE"], ["PERSONAL_PROTECTION", "PROTECTION"], ["RESEARCH", "RESEARCH"],
  ["DIPLOMACY", "DIPLOMACY"],
];
const GROUPS_KEY = "xmas.groups";

const LABELS = {
  HIRE_DOUBLEGANGER: "HIRE BODY DOUBLE",
  BUILD_ESPIONAGE_SATELLITE: "BUILD SPY SATELLITE",
  PERFORM_CYBER_ATTACK: "CYBER ATTACK",
  PERFORM_SABOTAGE: "SABOTAGE",
  PERFORM_HEIST: "HEIST",
  GATHER_INTELLIGENCE: "GATHER INTEL",
  SPECIAL_WEAPON_ATTACK: "NUCLEAR STRIKE",
  ATTACK: "ATTACK",
};

/** What each build/recruit/hire action adds to: [state section, field]. These take a quantity. */
const OWNED = {
  BUILD_FARM: ["economy", "farms"], BUILD_FACTORY: ["economy", "factories"],
  BUILD_ROBOT_FACTORY: ["economy", "robotFactories"], BUILD_BANK: ["economy", "banks"],
  RECRUIT_INFANTRY: ["military", "infantry"], RECRUIT_TANK: ["military", "tanks"],
  BUILD_MISSILE_BATTERY: ["military", "missileBatteries"], BUILD_AIR_DEFENSE_BATTERY: ["military", "airDefenseBatteries"],
  BUILD_STEALTH_BOMBER: ["military", "stealthBombers"], RECRUIT_SPECIAL_OPS: ["military", "specialOps"],
  BUILD_ESPIONAGE_SATELLITE: ["specialWeapons", "espionageSatellites"], BUILD_NUKE: ["specialWeapons", "nukes"],
  HIRE_SPIES: ["espionage", "spies"], HIRE_COUNTER_INTEL: ["espionage", "counterIntel"],
  HIRE_DOUBLEGANGER: ["protection", "doublegangers"], BUILD_SECRET_BUNKER: ["protection", "bunkers"],
  HIRE_BODYGUARDS: ["protection", "bodyguards"], HIRE_NINJA_BODYGUARDS: ["protection", "ninjaBodyguards"],
  DEMOLISH_FARM: ["economy", "farms"], DEMOLISH_FACTORY: ["economy", "factories"],
  DEMOLISH_ROBOT_FACTORY: ["economy", "robotFactories"], DEMOLISH_BANK: ["economy", "banks"],
};

/** Industry the farms feed, e.g. "13/36"; over capacity means the extra runs at half output. */
function fedText(e) {
  if (e.industryCapacity == null) return "–";
  const industry = (e.factories ?? 0) + (e.robotFactories ?? 0);
  return `${industry}/${e.industryCapacity}${industry > e.industryCapacity ? " !" : ""}`;
}

const UPKEEP_RULE = "Troops cost upkeep every tick. If your treasury runs dry, some of them desert.";

const FOOD_RULE = "Each farm feeds 3 factories or robot factories. Any beyond that earn only half.";

/** Extra warnings shown in the dialog, from the game rules. */
const WARNINGS = {
  ASSASSINATE_LEADER: "Only succeeds if the target's popularity is below 20%. You pay either way.",
  SPECIAL_WEAPON_ATTACK: "Every nation loses popularity when a nuke flies, you most of all.",
  ATTACK: "Everyone sees it coming: the attack lands after a delay. One attack per nation at a time.",
  DECAPITATION_STRIKE: "Only succeeds if the target's popularity is below 20%. You pay either way. One strike per nation at a time.",
  DECLARE_WAR: "Declaring war costs you popularity.",
  DECLARE_PEACE: "Only possible with a nation you are at war with. Making peace raises your popularity.",
  SEND_MONEY: "Gifts of $50,000 or more raise your popularity, less each time (like parades).",
  HOLD_ELECTION: "Resets your parade bonus, but freezes all non-military actions for a while.",
  DEMOLISH_FARM: "Torn down at once, with no refund. The land is free to build on. " + FOOD_RULE,
  BUILD_FACTORY: FOOD_RULE,
  BUILD_ROBOT_FACTORY: FOOD_RULE,
  BUILD_FARM: FOOD_RULE,
  RECRUIT_INFANTRY: UPKEEP_RULE,
  RECRUIT_TANK: UPKEEP_RULE,
  BUILD_STEALTH_BOMBER: UPKEEP_RULE,
  RECRUIT_SPECIAL_OPS: UPKEEP_RULE,
  DEMOLISH_FACTORY: "Torn down at once, with no refund. The land is free to build on.",
  DEMOLISH_ROBOT_FACTORY: "Torn down at once, with no refund. The land is free to build on.",
  DEMOLISH_BANK: "Torn down at once, with no refund. The land is free to build on.",
};

/** Spy operations: they need agents, and a failure can cost spies. */
const COVERT = new Set([
  "PERFORM_CYBER_ATTACK", "PERFORM_SABOTAGE", "PERFORM_HEIST", "STEAL_TECHNOLOGY",
  "GATHER_INTELLIGENCE", "ASSASSINATE_LEADER",
]);
const SPY_RISK = "If it fails, some of your spies may be caught, and the target may learn who sent them. "
  + "Your spies run one operation per nation at a time.";
const spyCount = (me) => me.espionage?.spies ?? 0;

const actionLabel = (type) =>
  LABELS[type] || (type.startsWith("RESEARCH_") ? researchName(type) : type.replaceAll("_", " "));
const titleCase = (s) => s.toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
const isDemolish = (def) => def.actionType.startsWith("DEMOLISH_");
const uiGroup = (def) =>
  def.actionType === "ATTACK" ? "ARMY"
    : def.actionType === "SPECIAL_WEAPON_ATTACK" ? "SPECIAL_WEAPONS"
      : isDemolish(def) ? "DEMOLITION" : def.category;
/** The server lets military actions through an election freeze. */
const isMilitary = (def) =>
  def.category === "ARMY" || def.actionType === "ATTACK" || def.actionType === "SPECIAL_WEAPON_ATTACK";
const takesQuantity = (def) =>
  def.actionType in OWNED || def.actionType === "SEND_MONEY" || def.actionType === "SPECIAL_WEAPON_ATTACK";
const ownedCount = (me, type) => (OWNED[type] ? me[OWNED[type][0]]?.[OWNED[type][1]] ?? 0 : null);

/** "$15,000", "Free", or "any sum" for sending money. */
const costLabel = (def) =>
  def.actionType === "SEND_MONEY" ? "any sum" : def.costCash > 0 ? fmtCash(def.costCash) : "Free";

function fmtDuration(ms) {
  if (ms <= 0) return "Instant";
  const s = Math.ceil(ms / 1000);
  return s < 60 ? `${s}s` : `${Math.floor(s / 60)}m ${String(s % 60).padStart(2, "0")}s`;
}

/** Why an action can't be taken right now, or null. The server enforces all of this too. */
function blockReason(def, me, info) {
  const p = me.player;
  const research = me.research || [];
  if (p.eliminated) return "Your government has fallen";
  if (def.category === "RESEARCH" && research.includes(def.actionType)) return "Researched";
  if (def.category === "RESEARCH" && pendingOf(def.actionType).length) return "Research under way";
  if (def.requiredResearch && !research.includes(def.requiredResearch)) {
    return `Needs ${titleCase(researchName(def.requiredResearch))}`;
  }
  if (def.maxTotal != null && ownedCount(me, def.actionType) >= def.maxTotal) return `Maximum of ${def.maxTotal} reached`;
  if (def.actionType === "SPECIAL_WEAPON_ATTACK" && !(me.specialWeapons?.nukes > 0)) return "No warheads built";
  if (def.actionType === "DECLARE_PEACE" && !(me.relations || []).some((r) => r.status === "WAR")) return "Not at war";
  if (COVERT.has(def.actionType) && spyCount(me) === 0) {
    // Special ops can carry out an assassination on their own.
    if (def.actionType !== "ASSASSINATE_LEADER") return "Needs spies";
    if (!(me.military?.specialOps > 0)) return "Needs spies or special ops";
  }
  if (p.frozenUntilTick > info.currentTick && !isMilitary(def)) return "Frozen by the election";
  if (usesLand(def) && freeLand(me) === 0) return "No free land";
  if (isDemolish(def) && ownedCount(me, def.actionType) === 0) return "Nothing to demolish";
  if (def.actionType !== "SEND_MONEY" && p.cash < def.costCash) return "Not enough cash";
  return null;
}

const actionRows = new Map();   // actionType → { btn, meta }

function savedGroups() {
  try { return JSON.parse(localStorage.getItem(GROUPS_KEY)) || ["ECONOMY"]; } catch { return ["ECONOMY"]; }
}

/** Builds the grouped action list once; later polls only update each row. */
function buildActionGroups() {
  const open = new Set(savedGroups());
  const chevron = '<svg viewBox="0 0 8 8" aria-hidden="true"><path d="M2 1h1v1H2zM3 2h1v1H3zM4 3h1v2H4zM3 5h1v1H3zM2 6h1v1H2z"/></svg>';
  const groups = GROUPS.map(([key, title]) => {
    const defs = state.actions.filter((d) => uiGroup(d) === key);
    if (!defs.length) return null;
    const details = el("details", "group");
    details.open = open.has(key);
    details.addEventListener("toggle", () => {
      const now = new Set(savedGroups());
      if (details.open) now.add(key); else now.delete(key);
      try { localStorage.setItem(GROUPS_KEY, JSON.stringify([...now])); } catch { /* ignore */ }
    });
    const summary = el("summary", "btn");
    summary.innerHTML = chevron;
    summary.append(el("span", "grow", title), el("span", "count", String(defs.length)));
    const rows = el("div", "group-rows");
    for (const def of defs) {
      const btn = el("button", "action");
      btn.type = "button";
      const top = el("span", "top");
      top.append(el("span", "name", actionLabel(def.actionType)), el("span", "cost", costLabel(def)));
      const meta = el("span", "meta");
      btn.append(top, meta);
      btn.addEventListener("click", () => openSheet(def.actionType, btn));
      rows.append(btn);
      actionRows.set(def.actionType, { btn, meta });
    }
    details.append(summary, rows);
    return details;
  }).filter(Boolean);
  $("actionGroups").replaceChildren(...groups);
}

function renderActions(me, info) {
  if (!state.actions.length) return;
  if (!actionRows.size) buildActionGroups();
  for (const def of state.actions) {
    const row = actionRows.get(def.actionType);
    if (!row) continue;
    const reason = blockReason(def, me, info);
    const done = reason === "Researched";
    row.btn.disabled = !!reason;
    row.btn.classList.toggle("done", done);
    const busyCount = pendingOf(def.actionType).reduce((n, p) => n + p.quantity, 0);
    let text = reason || `${fmtDuration(def.durationMillis)} · ${def.description || ""}`;
    if (busyCount && !reason) text = `${busyCount} under way · ${text}`;
    row.meta.textContent = text;
  }
}

// ---------------------------------------------------------------------
// Actions: under-way markers (kept per device; the server has no list of them)
// ---------------------------------------------------------------------

const PENDING_KEY = "xmas.pending";
let pending = [];   // { actionType, quantity, targetId, targetName, completesAt (ms, server clock), baseline }

const pendingTicker = makePoller(renderPending, 1000);
pollers.push(pendingTicker);

const serverNow = () => Date.now() + (state.clockSkew || 0);
const pendingOf = (type) => pending.filter((p) => p.actionType === type);

/** Actions the server allows only one of per target at a time; all spy operations share one slot. */
const slotOf = (type) => (COVERT.has(type) ? "SPY" : type === "ATTACK" || type === "DECAPITATION_STRIKE" ? type : null);
const underWayAgainst = (type, targetId) => {
  const slot = slotOf(type);
  return slot != null && pending.some((p) =>
    slotOf(p.actionType) === slot && p.targetId === targetId && serverNow() < p.completesAt);
};

function loadPending() {
  try {
    const saved = JSON.parse(localStorage.getItem(PENDING_KEY));
    pending = saved && saved.playerId === state.playerId ? saved.items : [];
  } catch { pending = []; }
}

function savePending() {
  try { localStorage.setItem(PENDING_KEY, JSON.stringify({ playerId: state.playerId, items: pending })); } catch { /* ignore */ }
}

function addPending(def, body, completesAt, targetName) {
  // Builds: the count this batch will be on top of (including batches still in flight).
  const inFlight = pendingOf(def.actionType).reduce((n, p) => n + p.quantity, 0);
  const owned = ownedCount(game.me, def.actionType);
  pending.push({
    actionType: def.actionType,
    quantity: takesQuantity(def) && def.actionType !== "SEND_MONEY" ? body.quantity : 1,
    targetId: body.targetPlayerId,
    targetName,
    completesAt: Date.parse(completesAt),
    baseline: owned == null ? null : owned + inFlight,
  });
  savePending();
  renderPending();
}

/** Short chip text: "FACTORY ×2 · 4S". */
function pendingText(p, remaining) {
  const label = actionLabel(p.actionType);
  const short = p.actionType in OWNED ? label.replace(/^\S+\s/, "") : label;
  const qty = p.quantity > 1 ? ` ×${p.quantity}` : "";
  const target = p.targetName ? ` → ${p.targetName}` : "";
  const when = remaining > 0 ? fmtDuration(remaining).toUpperCase() : "DUE";
  return `${short}${qty}${target} · ${when}`;
}

/** True once the latest state shows the action took effect (builds and research only). */
function landed(p) {
  const me = game.me;
  if (!me) return false;
  if (p.baseline != null) return ownedCount(me, p.actionType) >= p.baseline + p.quantity;
  if (p.actionType.startsWith("RESEARCH_")) return (me.research || []).includes(p.actionType);
  return false;
}

function renderPending() {
  // The server resolves due actions on its next tick. Builds and research are retired
  // as soon as the state shows them; anything else after a grace period (the news tells).
  const grace = (game.info?.tickDurationMillis || 5000) + 3000;
  const now = serverNow();
  const before = pending.length;
  pending = pending.filter((p) => {
    if (landed(p)) {
      toast(`${pendingText(p, 0).replace(/ · DUE$/, "")}: ready.`);
      return false;
    }
    return now < p.completesAt + grace;
  });
  if (pending.length !== before) {
    savePending();
    if (game.me && game.info) renderActions(game.me, game.info);
  }
  $("pendingStrip").replaceChildren(...pending
    .slice().sort((a, b) => a.completesAt - b.completesAt)
    .map((p) => el("span", "chip", pendingText(p, p.completesAt - now))));
}

// ---------------------------------------------------------------------
// Actions: dialog (quantity, target, confirm)
// ---------------------------------------------------------------------

const sheet = { def: null, qty: 1, targetId: null, roster: [], opener: null };

function openSheet(type, opener, targetId = null) {
  const def = state.actions.find((a) => a.actionType === type);
  const me = game.me;
  if (!def || !me) return;
  Object.assign(sheet, { def, qty: type === "SEND_MONEY" ? 10000 : 1, targetId, roster: [], opener });

  $("sheetTitle").textContent = actionLabel(type);
  $("sheetDesc").textContent = def.description || "";

  const facts = [];
  if (type !== "SEND_MONEY") facts.push([takesQuantity(def) && type !== "SPECIAL_WEAPON_ATTACK" ? "EACH" : "COST", costLabel(def)]);
  facts.push(["TIME", fmtDuration(def.durationMillis)]);
  if (type in OWNED) {
    const n = ownedCount(me, type);
    facts.push(["OWNED", def.maxTotal != null ? `${n}/${def.maxTotal}` : String(n)]);
  }
  if (type === "SPECIAL_WEAPON_ATTACK") facts.push(["WARHEADS", String(me.specialWeapons?.nukes ?? 0)]);
  if (COVERT.has(type)) facts.push(["YOUR SPIES", String(spyCount(me))]);
  if (usesLand(def) && freeLand(me) !== Infinity) facts.push(["FREE LAND", String(freeLand(me))]);
  $("sheetFacts").replaceChildren(...facts.map(([k, v]) => {
    const f = el("div", "fact");
    f.append(el("span", "k", k), el("span", "v", v));
    return f;
  }));

  const warning = [WARNINGS[type], COVERT.has(type) ? SPY_RISK : null].filter(Boolean).join(" ");
  $("sheetWarn").hidden = !warning;
  $("sheetWarn").textContent = warning;

  // Quantity / amount
  $("qtyBlock").hidden = !takesQuantity(def);
  $("qtyLabel").textContent = type === "SEND_MONEY" ? "AMOUNT ($)" : type === "SPECIAL_WEAPON_ATTACK" ? "WARHEADS TO LAUNCH" : "QUANTITY";
  const presets = type === "SEND_MONEY"
    ? [["$10K", 10000], ["$50K", 50000], ["$100K", 100000], ["$250K", 250000]]
    : [["1", 1], ["5", 5], ["10", 10], ["MAX", "max"]];
  $("qtyPresets").replaceChildren(...presets.map(([label, value]) => {
    const b = el("button", "btn", label);
    b.type = "button";
    b.dataset.value = value;
    b.addEventListener("click", () => { sheet.qty = value === "max" ? maxQty() : value; updateSheet(); });
    return b;
  }));

  // Targets
  $("targetBlock").hidden = !def.targeted;
  $("targetList").replaceChildren(el("p", "dim", "Loading nations…"));
  if (def.targeted) loadTargets();

  $("sheet").hidden = false;
  document.body.classList.add("sheet-open");
  updateSheet();
  $("sheet").querySelector(".gad-btn").focus();
}

function closeSheet() {
  $("sheet").hidden = true;
  document.body.classList.remove("sheet-open");
  sheet.def = null;
  if (sheet.opener && document.contains(sheet.opener)) sheet.opener.focus();
}

/** The most you can do in one go: limited by cash, warheads and caps. */
function maxQty() {
  const { def } = sheet;
  const me = game.me;
  const cash = me.player.cash;
  if (def.actionType === "SEND_MONEY") return Math.max(0, cash);
  let max = def.costCash > 0 ? Math.floor(cash / def.costCash) : 999;
  if (def.actionType === "SPECIAL_WEAPON_ATTACK") max = Math.min(max, me.specialWeapons?.nukes ?? 0);
  if (def.maxTotal != null) max = Math.min(max, def.maxTotal - ownedCount(me, def.actionType));
  if (usesLand(def)) max = Math.min(max, freeLand(me));
  if (isDemolish(def)) max = Math.min(max, ownedCount(me, def.actionType));
  return Math.max(0, max);
}

function sheetTotal() {
  const { def, qty } = sheet;
  if (def.actionType === "SEND_MONEY") return qty;
  return def.costCash * (takesQuantity(def) ? qty : 1);
}

function updateSheet() {
  const { def } = sheet;
  if (!def) return;
  const cash = game.me.player.cash;
  const quantity = takesQuantity(def);
  if (quantity) {
    sheet.qty = Math.max(1, Math.floor(Number(sheet.qty) || 1));
    if (document.activeElement !== $("qtyInput")) $("qtyInput").value = sheet.qty;
    for (const b of $("qtyPresets").children) {
      const v = b.dataset.value === "max" ? maxQty() : Number(b.dataset.value);
      b.classList.toggle("on", v === sheet.qty);
    }
  }
  const total = sheetTotal();
  $("sheetTotal").textContent = fmtCash(total);
  const left = cash - total;
  $("sheetLeft").textContent = left >= 0 ? `${fmtCash(left)} left after` : "Not enough cash";
  $("sheetLeft").classList.toggle("bad", left < 0);

  let go;
  if (def.actionType === "SEND_MONEY") go = `SEND ${fmtCash(sheet.qty)}`;
  else if (quantity) go = `${actionLabel(def.actionType).split(" ")[0]} ×${sheet.qty}`;
  else go = actionLabel(def.actionType).length <= 16 ? actionLabel(def.actionType) : "CONFIRM";

  const busyTarget = def.targeted && underWayAgainst(def.actionType, sheet.targetId);
  if (busyTarget) go = "UNDER WAY";
  $("sheetGo").textContent = go;

  const overCap = quantity && def.actionType !== "SEND_MONEY" && sheet.qty > maxQty();
  $("sheetGo").disabled = left < 0 || overCap || busyTarget || (def.targeted && !sheet.targetId);
}

async function loadTargets() {
  const forType = sheet.def.actionType;
  try {
    const roster = await api("GET", `/games/${state.gameId}/players`);
    if (!sheet.def || sheet.def.actionType !== forType) return;   // dialog changed meanwhile
    sheet.roster = roster;
    renderTargets();
  } catch (e) {
    $("targetList").replaceChildren(el("p", "dim", "Could not load nations."));
    toastError(e);
  }
}

function renderTargets() {
  const relations = new Map((game.me.relations || []).map((r) => [r.otherPlayerId, r.status]));
  const targets = sheet.roster
    .filter((p) => p.id !== state.playerId && !p.admin && !p.eliminated)
    // War and peace only make sense against the opposite relation.
    .filter((p) => sheet.def.actionType !== "DECLARE_PEACE" || relations.get(p.id) === "WAR")
    .filter((p) => sheet.def.actionType !== "DECLARE_WAR" || relations.get(p.id) !== "WAR")
    .sort((a, b) => formatNation(a.nation).localeCompare(formatNation(b.nation)));
  if (!targets.length) {
    $("targetList").replaceChildren(el("p", "dim", "No other nations to target."));
    return;
  }
  $("targetList").replaceChildren(...targets.map((p) => {
    const row = el("label", "target");
    const radio = el("input");
    radio.type = "radio";
    radio.name = "target";
    radio.value = p.id;
    radio.checked = p.id === sheet.targetId;
    radio.addEventListener("change", () => { sheet.targetId = p.id; updateSheet(); });

    const flag = el("span", "flag");
    flag.dataset.nation = p.nation;
    flag.setAttribute("aria-hidden", "true");
    const who = el("span", "who");
    who.append(el("span", "hd", formatNation(p.nation)), el("span", "user", p.username));
    row.append(radio, flag, who);

    const rel = relations.get(p.id);
    if (rel) row.append(el("span", `tag ${rel === "WAR" ? "bad" : "good"}`, rel));
    if (p.popularity < VULNERABLE_BELOW) row.append(el("span", "tag bad", "WEAK"));
    const bar = el("span", "minibar");
    const fill = el("span", popLevel(p.popularity));
    fill.style.width = `${p.popularity}%`;
    bar.append(fill);
    row.append(bar, el("span", "pct", `${p.popularity}%`));
    return row;
  }));
}

async function submitSheet() {
  const { def } = sheet;
  if (!def) return;
  const body = { actionType: def.actionType };
  if (def.targeted) body.targetPlayerId = sheet.targetId;
  if (takesQuantity(def)) body.quantity = sheet.qty;
  const target = sheet.roster.find((p) => p.id === sheet.targetId);
  const targetName = target ? formatNation(target.nation) : null;

  await busy([$("sheetGo")], async () => {
    try {
      const result = await api("POST", "/actions", body);
      const label = actionLabel(def.actionType);
      closeSheet();
      if (result.completedImmediately) {
        toast(def.targeted ? `${label} → ${targetName}: done. Watch the news.` : `${label}: done.`);
      } else {
        addPending(def, body, result.completesAt, targetName);
        const wait = Date.parse(result.completesAt) - serverNow();
        toast(`${label}: under way, ready in ${fmtDuration(wait)}.`);
      }
      gamePoller.now();
    } catch (e) {
      toastError(e);
    }
  });
}

function initSheet() {
  for (const node of document.querySelectorAll("#sheet [data-close]")) node.addEventListener("click", closeSheet);
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && !$("sheet").hidden) closeSheet();
  });
  $("qtyInput").addEventListener("input", () => {
    const digits = $("qtyInput").value.replace(/\D/g, "");
    $("qtyInput").value = digits;
    if (digits) { sheet.qty = Number(digits); updateSheet(); }
  });
  $("qtyInput").addEventListener("blur", updateSheet);
  const step = () => (sheet.def?.actionType === "SEND_MONEY" ? 10000 : 1);
  $("qtyMinus").addEventListener("click", () => { sheet.qty = Math.max(1, sheet.qty - step()); updateSheet(); });
  $("qtyPlus").addEventListener("click", () => { sheet.qty += step(); updateSheet(); });
  $("sheetGo").addEventListener("click", submitSheet);
}

// ---------------------------------------------------------------------
// News
// ---------------------------------------------------------------------

const NEWS_SEEN_KEY = "xmas.newsSeen";
const NEWS_LIMIT = 50;

const news = {
  events: [],
  filter: "all",
  seen: 0,            // newest gameTime (ms) the player has looked at
  openedMark: null,   // `seen` as it was when the tab was opened: newer items get NEW
  loaded: false,
  signature: "",
};

const NEWS_TAGS = { ATTACK: ["ATTACK", "bad"], ESPIONAGE: ["SPY", "warn"], DIPLOMACY: ["DIPLO", ""], GAME_STARTED: ["GAME", "accent"] };
const INTEL_RE = /^INTEL on (.+?) \((\d+)% confidence\) — Army: (.+?)\. Economy: (.+?)\. Tech detected: (.+?)\.$/s;
const evTime = (ev) => Date.parse(ev.gameTime);
/** Unread = newer than the last look, not counting echoes of your own public moves. */
const isUnreadAfter = (ev, mark) => evTime(ev) > mark
  && !(ev.actorPlayerId === state.playerId && ev.visibility !== "ACTOR_ONLY");
/** Short labels for the intel report, matching the Nation tab. */
const INTEL_LABELS = {
  "stealth bombers": "Bombers", "special ops": "Spec. ops", "air defense": "Air def.",
  "missile batteries": "Missiles", "robot factories": "Robot fac.",
};
const involvesMe = (ev) => ev.actorPlayerId === state.playerId || ev.targetPlayerId === state.playerId;

function newsTag(ev) {
  if (INTEL_RE.test(ev.headline)) return ["INTEL", "good"];
  if (/nuclear|warhead/i.test(ev.headline)) return ["NUKE", "bad"];
  return NEWS_TAGS[ev.type] || [ev.type, ""];
}

const NEWS_FILTERS = {
  all: () => true,
  me: involvesMe,
  war: (ev) => ev.type === "ATTACK" || /nuclear|warhead/i.test(ev.headline),
  spy: (ev) => ev.type === "ESPIONAGE",
  diplo: (ev) => ev.type === "DIPLOMACY",
};

/** Makes server text read nicely: nation and research codes, dollar amounts. */
function tidy(text) {
  return text
    .replace(/\bRESEARCH_[A-Z_]+\b/g, (m) => titleCase(researchName(m)))
    .replace(/\b(NORTH_KOREA|SOUTH_AFRICA|SAUDI_ARABIA)\b/g, (m) => m.replace("_", " "))
    .replace(/\$(\d{4,})/g, (_, n) => fmtCash(Number(n)));
}

/** The tick an event happened on, from its game time. */
function eventTick(ev) {
  const info = game.info;
  if (!info) return null;
  const ticksAgo = Math.round((Date.parse(info.gameTime) - evTime(ev)) / info.tickDurationMillis);
  return Math.max(0, info.currentTick - ticksAgo);
}

/** Text with the player's own nation picked out. */
function withMyNation(text) {
  const mine = formatNation(game.me?.player.nation);
  const span = el("span");
  if (!mine) { span.textContent = text; return span; }
  text.split(mine).forEach((part, i) => {
    if (i) span.append(el("strong", "", mine));
    span.append(part);
  });
  return span;
}

function intelBlock(match) {
  const [, nation, confidence, army, economy, tech] = match;
  const box = el("div", "intel");
  const rows = (list) => list.split(", ").map((item) => {
    const m = item.match(/^(.*) (\d+)-(\d+)$/);
    const row = el("div", "row");
    const value = m ? (m[2] === m[3] ? m[2] : `${m[2]}–${m[3]}`) : "";
    const label = m ? INTEL_LABELS[m[1]] || titleCase(m[1]) : item;
    row.append(el("span", "k", label), el("span", "", value));
    return row;
  });
  box.append(el("span", "sub", "ARMY"), ...rows(army), el("span", "sub", "ECONOMY"), ...rows(economy));
  const techLine = el("div", "full");
  techLine.append(el("span", "k dim", "Tech: "), tech === "none" ? "none detected" : tech);
  box.append(el("span", "sub", "TECHNOLOGY"), techLine);
  const intro = el("p", "news-text", `Recon on ${nation}: ${confidence}% confidence.`);
  return [intro, box];
}

function newsItem(ev) {
  const item = el("article", "news-item");
  const head = el("div", "news-head");
  const [tagText, tagKind] = newsTag(ev);
  head.append(el("span", `tag ${tagKind}`, tagText));
  const tick = eventTick(ev);
  if (tick !== null) head.append(el("span", "when", `TICK ${tick}`));
  if (ev.visibility === "ACTOR_ONLY") head.append(el("span", "who", "· ONLY YOU"));
  else if (ev.visibility === "INVOLVED_ONLY") head.append(el("span", "who", "· SECRET"));
  head.append(el("span", "grow"));
  if (news.openedMark !== null && isUnreadAfter(ev, news.openedMark)) head.append(el("span", "tag accent", "NEW"));
  item.append(head);

  const text = tidy(ev.headline);
  const intel = text.match(INTEL_RE);
  if (intel) {
    item.append(...intelBlock(intel));
    return item;
  }
  // First sentence is the news; the rest (losses etc.) is detail.
  const split = text.match(/^(.+?[.!?])\s+(.+)$/s);
  const main = el("p", "news-text");
  main.append(withMyNation(split ? split[1] : text));
  item.append(main);
  if (split) item.append(el("p", "news-more", split[2]));
  return item;
}

function loadNewsSeen() {
  try {
    const saved = JSON.parse(localStorage.getItem(NEWS_SEEN_KEY));
    news.seen = saved && saved.playerId === state.playerId ? saved.at : 0;
  } catch { news.seen = 0; }
}

function markNewsSeen() {
  const newest = news.events.reduce((t, ev) => Math.max(t, evTime(ev)), news.seen);
  if (newest === news.seen) return;
  news.seen = newest;
  try { localStorage.setItem(NEWS_SEEN_KEY, JSON.stringify({ playerId: state.playerId, at: newest })); } catch { /* ignore */ }
}

const newsTabOpen = () => !$("tab-news").hidden;

function onNewsTabOpened() {
  news.openedMark = news.seen;
  markNewsSeen();
  news.signature = "";          // force a redraw with NEW tags
  renderNews();
}

function receiveNews(feed) {
  const known = new Set(news.events.map((ev) => ev.id));
  const fresh = feed.filter((ev) => !known.has(ev.id));
  news.events = feed;

  // Heads-up when someone acts against you and you're looking elsewhere.
  if (news.loaded && !newsTabOpen()) {
    for (const ev of fresh) {
      if (ev.targetPlayerId === state.playerId && ev.actorPlayerId !== state.playerId) {
        toast(tidy(ev.headline).match(/^(.+?[.!?])(\s|$)/)?.[1] || tidy(ev.headline), "alert");
      }
    }
  }
  if (!news.loaded) {
    news.loaded = true;
    if (newsTabOpen()) news.openedMark = news.seen;
  }
  if (newsTabOpen()) markNewsSeen();
  renderNews();
}

function renderNews() {
  const unread = news.events.filter((ev) => isUnreadAfter(ev, news.seen)).length;
  $("newsBadge").hidden = unread === 0 || newsTabOpen();
  $("newsBadge").textContent = unread > 9 ? "9+" : String(unread);

  if (!news.loaded) return;
  const shown = news.events.filter(NEWS_FILTERS[news.filter]);
  const signature = `${news.filter}|${news.openedMark}|${shown.map((ev) => ev.id).join(",")}`;
  if (signature === news.signature) return;
  news.signature = signature;

  if (!shown.length) {
    const msg = news.events.length ? "Nothing here with this filter." : "No news yet. Make some.";
    $("newsList").replaceChildren(el("p", "dim news-empty", msg));
    return;
  }
  $("newsList").replaceChildren(...shown.map(newsItem));
}

function resetNews() {
  Object.assign(news, { events: [], filter: news.filter, openedMark: null, loaded: false, signature: "" });
  loadNewsSeen();
  $("newsList").replaceChildren(el("p", "dim news-empty", "Loading news…"));
  $("newsBadge").hidden = true;
}

function initNews() {
  for (const btn of $("newsFilters").querySelectorAll("[data-filter]")) {
    btn.addEventListener("click", () => {
      news.filter = btn.dataset.filter;
      for (const b of $("newsFilters").querySelectorAll("[data-filter]")) {
        b.setAttribute("aria-pressed", String(b === btn));
      }
      renderNews();
    });
  }
}

// ---------------------------------------------------------------------
// World
// ---------------------------------------------------------------------

/** Shown up front on a nation card ("DIPLOMACY" = war or peace, whichever applies). */
const QUICK_ACTIONS = ["ATTACK", "GATHER_INTELLIGENCE", "DIPLOMACY", "SEND_MONEY"];
const DETAIL_REFRESH_MS = 10000;

const world = {
  roster: [],
  openId: null,           // the expanded nation
  moreOpen: false,        // "More actions" expanded on it
  detail: new Map(),      // playerId → { at, state }: their (limited or satellite) state
  buttons: [],            // quick-action buttons on the open card, to update per poll
  signature: "",
};

const relationWith = (id) => (game.me?.relations || []).find((r) => r.otherPlayerId === id)?.status || null;
const rosterEntry = (id) => world.roster.find((p) => p.id === id);
const latestIntel = (id) => news.events.find((ev) =>
  ev.targetPlayerId === id && ev.actorPlayerId === state.playerId && INTEL_RE.test(ev.headline));

function receiveRoster(roster) {
  world.roster = roster;
  renderWorld();
}

/** Their state as we may see it: relations always, full stats only with satellite intel. */
async function loadDetail(id, force = false) {
  const cached = world.detail.get(id);
  if (!force && cached && Date.now() - cached.at < DETAIL_REFRESH_MS) return;
  world.detail.set(id, { ...cached, at: Date.now() });   // no duplicate fetches meanwhile
  try {
    const theirs = await api("GET", `/players/${id}`);
    world.detail.set(id, { at: Date.now(), state: theirs });
    if (world.openId === id) { world.signature = ""; renderWorld(); }
  } catch { /* keep what we had; the game poll reports connection trouble */ }
}

function toggleNation(id) {
  world.openId = world.openId === id ? null : id;
  world.moreOpen = false;
  world.signature = "";
  renderWorld();
  if (world.openId) {
    loadDetail(id, true);
    document.querySelector(`[data-nation-id="${id}"]`)?.scrollIntoView({ block: "nearest" });
  }
}

function quickButton(def, targetId) {
  const btn = el("button", "btn qa");
  btn.type = "button";
  btn.append(el("span", "qn", actionLabel(def.actionType)), el("span", "qm"));
  btn.addEventListener("click", () => openSheet(def.actionType, btn, targetId));
  world.buttons.push({ btn, def });
  return btn;
}

function updateQuickButtons() {
  if (!game.me || !game.info) return;
  for (const { btn, def } of world.buttons) {
    const reason = blockReason(def, game.me, game.info);
    btn.disabled = !!reason;
    btn.querySelector(".qm").textContent = reason || costLabel(def);
  }
}

function nationHead(p) {
  const mine = p.id === state.playerId;
  const open = world.openId === p.id;
  const interactive = !mine && !p.eliminated;
  const head = el(interactive ? "button" : "div", `nation-head${mine ? " me" : ""}${p.eliminated ? " out" : ""}`);
  head.dataset.nationId = p.id;
  if (interactive) {
    head.type = "button";
    head.setAttribute("aria-expanded", String(open));
    head.setAttribute("aria-controls", `nation-body-${p.id}`);
    head.addEventListener("click", () => toggleNation(p.id));
  }
  const flag = el("span", "flag");
  flag.dataset.nation = p.nation || "";
  flag.setAttribute("aria-hidden", "true");
  const who = el("span", "who");
  who.append(el("span", "hd", formatNation(p.nation)), el("span", "user", p.username));
  head.append(flag, who);

  if (mine) head.append(el("span", "tag", "YOU"));
  if (p.eliminated) {
    head.append(el("span", "tag bad", "FALLEN"));
    return head;
  }
  const rel = relationWith(p.id);
  if (rel) head.append(el("span", `tag ${rel === "WAR" ? "bad" : "good"}`, rel));
  if (p.popularity < VULNERABLE_BELOW) head.append(el("span", "tag bad", "WEAK"));
  const bar = el("span", "minibar");
  const fill = el("span", popLevel(p.popularity));
  fill.style.width = `${p.popularity}%`;
  bar.append(fill);
  head.append(bar, el("span", "pct", `${p.popularity}%`));
  return head;
}

/** Who they're at war with: public, from their relations. */
function warsLine(theirs) {
  const line = el("p", "wars");
  if (!theirs) { line.append(el("span", "k", "Checking their wars…")); return line; }
  const enemies = (theirs.relations || []).filter((r) => r.status === "WAR")
    .map((r) => (r.otherPlayerId === state.playerId ? "YOU" : formatNation(rosterEntry(r.otherPlayerId)?.nation) || "?"));
  line.append(el("span", "k", "At war with: "), enemies.length ? enemies.join(", ") : "nobody");
  return line;
}

/** Full stats, when satellite intel lets us see them. */
function satelliteBlock(theirs) {
  const box = el("div", "intel");
  for (const section of SECTIONS) {
    box.append(el("span", "sub", section.title));
    for (const [field, label] of section.fields) {
      const row = el("div", "row");
      row.append(el("span", "k", label), el("span", "", String(theirs[section.key]?.[field] ?? "?")));
      box.append(row);
    }
  }
  const tech = (theirs.research || []).map((r) => titleCase(researchName(r)));
  const techLine = el("div", "full");
  techLine.append(el("span", "k dim", "Tech: "), tech.length ? tech.join(", ") : "none");
  box.append(el("span", "sub", "TECHNOLOGY"), techLine);
  return box;
}

function intelSection(p) {
  const wrap = el("div", "stack6");
  const theirs = world.detail.get(p.id)?.state;
  wrap.append(warsLine(theirs));

  if (theirs?.military) {
    const title = el("div", "intel-title");
    title.append(el("span", "tag good", "SATELLITE"), el("span", "hd", "LIVE VIEW"));
    wrap.append(title, satelliteBlock(theirs));
    return wrap;
  }
  const report = latestIntel(p.id);
  if (report) {
    const title = el("div", "intel-title");
    title.append(el("span", "tag good", "INTEL"), el("span", "hd", `LAST RECON · TICK ${eventTick(report) ?? "?"}`));
    wrap.append(title, ...intelBlock(tidy(report.headline).match(INTEL_RE)).slice(1));
    return wrap;
  }
  wrap.append(el("p", "dim small", "No intel on their army yet. Gather Intel needs spies; a satellite network shows everything."));
  return wrap;
}

function nationBody(p) {
  const body = el("div", "nation-body");
  body.id = `nation-body-${p.id}`;
  const targeted = state.actions.filter((d) => d.targeted);
  const rel = relationWith(p.id);
  const quickTypes = QUICK_ACTIONS.map((t) => (t === "DIPLOMACY" ? (rel === "WAR" ? "DECLARE_PEACE" : "DECLARE_WAR") : t));
  const quick = el("div", "quick");
  for (const type of quickTypes) {
    const def = targeted.find((d) => d.actionType === type);
    if (def) quick.append(quickButton(def, p.id));
  }
  const rest = targeted.filter((d) => !quickTypes.includes(d.actionType)
    && !(d.actionType === "DECLARE_WAR" || d.actionType === "DECLARE_PEACE"));
  const more = el("details", "more-actions");
  more.open = world.moreOpen;
  more.addEventListener("toggle", () => { world.moreOpen = more.open; });
  more.append(el("summary", "hd", `MORE ACTIONS (${rest.length})`));
  const moreGrid = el("div", "quick");
  for (const def of rest) moreGrid.append(quickButton(def, p.id));
  more.append(moreGrid);
  body.append(quick, more, intelSection(p));
  return body;
}

function renderWorld() {
  if (!game.me || !game.info || !world.roster.length) return;
  const rank = (p) => (p.id === state.playerId ? 0 : p.eliminated ? 2 : 1);
  const nations = world.roster.filter((p) => !p.admin)
    .sort((a, b) => rank(a) - rank(b) || formatNation(a.nation).localeCompare(formatNation(b.nation)));

  const alive = nations.filter((p) => !p.eliminated).length;
  const wars = (game.me.relations || []).filter((r) => r.status === "WAR").length;
  $("worldTitle").textContent = wars ? `NATIONS ${alive} · AT WAR ${wars}` : `NATIONS ${alive}`;

  // Rebuild only when something visible changed; cash-dependent button states update in place.
  const signature = JSON.stringify([
    nations.map((p) => [p.id, p.popularity, p.eliminated, relationWith(p.id)]),
    world.openId, state.actions.length,
    world.openId && latestIntel(world.openId)?.id,
  ]);
  if (signature !== world.signature) {
    world.signature = signature;
    world.buttons = [];
    $("worldList").replaceChildren(...nations.flatMap((p) =>
      (world.openId === p.id ? [nationHead(p), nationBody(p)] : [nationHead(p)])));
  }
  updateQuickButtons();
  if (world.openId) loadDetail(world.openId);   // throttled refresh while a card is open
}

function resetWorld() {
  Object.assign(world, { roster: [], openId: null, moreOpen: false, detail: new Map(), buttons: [], signature: "" });
  $("worldList").replaceChildren(el("p", "dim news-empty", "Loading nations…"));
}

function initGame() {
  initSheet();
  initNews();
  for (const btn of document.querySelectorAll("#tabbar [data-tab]")) {
    btn.addEventListener("click", () => selectTab(btn.dataset.tab));
  }
  for (const btn of document.querySelectorAll("[data-logout]")) {
    btn.addEventListener("click", logout);
  }
  $("pauseBtn").addEventListener("click", () => setPaused(true));
  $("resumeBtn").addEventListener("click", () => setPaused(false));
}

// ---------------------------------------------------------------------
// Boot
// ---------------------------------------------------------------------

function init() {
  restore();
  initTheme();
  initAuth();
  initLobby();
  initGame();
  if (state.sessionToken) enterLobby();
  else showAuth();
}

init();
