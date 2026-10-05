# Frontend Implementation Guide

A guide for building the phone-first web client with plain **HTML + CSS + JavaScript** (no framework, no build step). The whole app is one `index.html`, one `styles.css`, one `app.js`.

The backend is a set of REST endpoints (OpenAPI-generated). The client is a thin layer: authenticate, join a game, then **poll** the server on the game tick and render the returned state.

---

## 1. Architecture

- **Single page, screen-switching.** Three screens toggled by CSS: `#auth`, `#lobby`, `#game`. Only one visible at a time.
- **Server is the source of truth.** The client holds almost no logic — it renders whatever the API returns and posts actions. All rules (costs, timers, combat, popularity) live server-side.
- **Polling, not push.** There is no websocket. Poll `GET /players/{id}` and the news feed every few seconds (roughly the tick length). The game clock advances on the server via Quartz.
- **Everything authenticated goes through one header:** `X-Session-Token`.

Client state to keep in memory (and mirror to `localStorage` so a refresh doesn't log you out):

```js
const state = {
  baseUrl: "",          // same origin: the UI is served by game-app from /static
  sessionToken: null,   // from login
  accountId: null,      // from login
  gameId: null,         // the game you created/joined
  playerId: null,       // YOUR player id (from joinGame) — needed to read your own state
  isAdmin: false,       // from your PlayerResponse
  actions: [],          // action catalog (GET /actions), fetched once
};
```

---

## 2. API reference (what the client calls)

Base URL + JSON everywhere. Auth endpoints need **no** token; everything else needs `X-Session-Token` except where noted.

| Call | Method / path | Auth | Body / notes |
|---|---|---|---|
| Register | `POST /account/register` | no | `{username, password}` → `{id, username}` (201) |
| Login | `POST /account/login` | no | `{username, password}` → `{accountId, sessionToken}` |
| List games | `GET /games` | yes | → `GameListing[]` `{id, status, createdAt, adminUsername?, nations, maxNations, joined}` — every lobby, plus started games you're in |
| Create game | `POST /games` | no | → `GameResponse` (then join it: the first joiner is the admin) |
| Game status | `GET /games/{gameId}` | yes | → `GameResponse` — poll in the lobby for `RUNNING`; gives `currentTick` for the election freeze |
| Roster | `GET /games/{gameId}/players` | yes | → `GamePlayerSummary[]` `{id, username, nation?, popularity, eliminated, admin}` — use for the target picker |
| Join game | `POST /games/{gameId}/players` | yes | no body → `PlayerResponse` (**save `id` as your playerId**; first joiner is admin) |
| Start game | `POST /games/{gameId}/start` | yes | admin only → `GameResponse` |
| Pause / Resume | `POST /games/{gameId}/pause` `…/resume` | yes | admin only |
| Player state | `GET /players/{playerId}` | yes | → `PlayerStateResponse` (full for you / satellite intel, limited otherwise) |
| Action catalog | `GET /actions` | no | → `ActionDefinition[]` (fetch once, cache) |
| Submit action | `POST /actions` | yes | `{gameId, actionType, targetPlayerId?, quantity?}` → `ActionResult` (you act as your player in `gameId`) |
| News feed | `GET /players/{playerId}/news?limit=50` | yes | → `NewsEvent[]` filtered for that player; you may only read **your own** (403 otherwise) |

### Response shapes

```jsonc
// GameResponse
{ "id": "uuid", "status": "LOBBY|RUNNING|PAUSED|ENDED",
  "currentTick": 12, "gameTime": "2026-01-01T00:01:00Z", "tickDurationMillis": 5000 }

// PlayerResponse (also nested as PlayerStateResponse.player)
{ "id": "uuid", "gameId": "uuid", "accountId": "uuid", "nation": "USA",
  "cash": 1500000, "popularity": 50, "admin": false, "eliminated": false,
  "popularityBoostCount": 0, "frozenUntilTick": 0 }

// PlayerStateResponse — the sub-objects are ABSENT (undefined) in the limited view
{ "player": { ... },
  "economy":       { "farms":0,"factories":0,"robotFactories":0,"banks":0 },
  "military":      { "infantry":0,"tanks":0,"missileBatteries":0,"airDefenseBatteries":0,"stealthBombers":0,"specialOps":0 },
  "espionage":     { "spies":0,"counterIntel":0 },
  "specialWeapons":{ "nukes":0,"espionageSatellites":0 },
  "protection":    { "doublegangers":0,"bunkers":0,"bodyguards":0,"ninjaBodyguards":0 },
  "research": ["RESEARCH_ADVANCED_FARMING"],
  "relations": [ { "otherPlayerId":"uuid", "status":"WAR|PEACE" } ] }

// ActionDefinition
{ "actionType":"BUILD_FARM", "category":"ECONOMY", "costCash":10000,
  "durationMillis":0, "targeted":false, "publicVisibility":false,
  "description":"Build a farm...", "maxTotal":null, "requiredResearch":null }

// ActionResult
{ "actionType":"BUILD_FARM", "completedImmediately":true,
  "completesAt":"2026-01-01T00:00:05Z", "message":"Completed" }

// NewsEvent
{ "id":"uuid", "gameId":"uuid", "type":"ATTACK|ESPIONAGE|DIPLOMACY|GAME_STARTED",
  "visibility":"PUBLIC|INVOLVED_ONLY|ACTOR_ONLY", "actorPlayerId":"uuid",
  "targetPlayerId":"uuid", "headline":"...", "gameTime":"..." }
```

The news poll must use **your own** `playerId` with your token; requesting another player's feed returns 403.

---

## 3. The fetch helper

One function handles JSON, the token header, and errors (the server returns `{status, message}` on failures).

```js
async function api(method, path, body) {
  const headers = { "Content-Type": "application/json" };
  if (state.sessionToken) headers["X-Session-Token"] = state.sessionToken;
  const res = await fetch(state.baseUrl + path, {
    method, headers, body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    if (res.status === 401) { logout(); }               // token expired → back to login
    throw new Error(data?.message || `HTTP ${res.status}`); // show data.message as a toast
  }
  return data;
}
```

---

## 4. Screen flow

```
AUTH  → register/login → save token+accountId
  ↓
LOBBY → create game (get id)  OR  paste a game id + Join
      → shows game status; admin sees a "Start" button
  ↓ (once status === RUNNING)
GAME  → poll loop renders your dashboard + actions + news
```

```js
function show(screen) {
  for (const id of ["auth","lobby","game"])
    document.getElementById(id).hidden = (id !== screen);
}
```

### Auth

```js
async function login(username, password) {
  const { accountId, sessionToken } = await api("POST","/account/login",{username,password});
  state.accountId = accountId; state.sessionToken = sessionToken;
  persist(); show("lobby");
}
// register is the same call to /account/register first, then login.
```

### Lobby

- **Open lobby**: poll `GET /games` on the menu and list the games; anyone can join any lobby (up to 13 nations + the admin).
- **Create** (admin): `POST /games`, then join it.
- **Join**: `POST /games/{gameId}/players`; store the returned `player.id` as `playerId` and `player.admin` as `isAdmin`. Rejoining a game you're already in returns 400; find yourself in the roster instead.
- Identity is **per game**: an account can be in several games, so every call resolves your player within a game (the path's game, the viewed player's game, or `gameId` in the action body).
- Poll `GET /games/{gameId}` to watch for `status === "RUNNING"`, then `show("game")`.
- Admin gets **Start** (`POST /games/{id}/start`). After that, Pause/Resume live on the game screen.

---

## 5. The poll loop (game screen)

```js
let pollTimer = null;
function startPolling() {
  const tick = async () => {
    try {
      const [me, news] = await Promise.all([
        api("GET", `/players/${state.playerId}`),
        api("GET", `/players/${state.playerId}/news?limit=50`),
      ]);
      renderDashboard(me);
      renderNews(news);
    } catch (e) { toast(e.message); }
  };
  tick();
  pollTimer = setInterval(tick, 4000); // ~ the tick length; tune to tickDurationMillis
}
function stopPolling() { clearInterval(pollTimer); }
```

Fetch the action catalog **once** on entering the game screen (`state.actions = await api("GET","/actions")`), since it never changes during a game.

---

## 6. Rendering the dashboard

`me` is a `PlayerStateResponse`. Render the `player` block always; render each sub-section only if present (they're absent in the limited view of *other* players — but for your own player they're always there).

```js
function renderDashboard(me) {
  const p = me.player;
  el("nation").textContent   = p.nation ?? "(unassigned)";
  el("cash").textContent     = "$" + p.cash.toLocaleString();
  el("popularity").textContent = p.popularity + "%";
  el("eliminated").hidden    = !p.eliminated;   // show a "DEFEATED" banner
  // election freeze: compare p.frozenUntilTick against currentTick from GET /games/{gameId} (poll it alongside)
  section("economy", me.economy, ["farms","factories","robotFactories","banks"]);
  section("military", me.military, ["infantry","tanks","stealthBombers","specialOps","airDefenseBatteries","missileBatteries"]);
  section("espionage", me.espionage, ["spies","counterIntel"]);
  section("specialWeapons", me.specialWeapons, ["nukes","espionageSatellites"]);
  section("protection", me.protection, ["doublegangers","bunkers","bodyguards","ninjaBodyguards"]);
  el("research").textContent = (me.research || []).join(", ") || "none";
  el("relations").textContent = (me.relations || [])
    .map(r => `${r.status} with ${nameOf(r.otherPlayerId)}`).join(", "); // nameOf: look up nation/username in the cached roster
}
```

Popularity is the key stat to surface prominently (it drives income and vulnerability). A colored bar (green ≥ 50, amber 20–49, red < 20) reads well on a phone.

---

## 7. Actions

Group `state.actions` by `category` into collapsible sections (ECONOMY, ARMY, SPECIAL_WEAPONS, ESPIONAGE, PERSONAL_PROTECTION, RESEARCH, DIPLOMACY). For each action render a button showing `description`, `costCash`, and a duration hint.

```js
async function submit(actionType, def) {
  const body = { actionType };
  if (def.targeted) body.targetPlayerId = await pickTarget();  // see §8
  if (needsQuantity(def)) body.quantity = askQuantity();       // e.g. build/recruit N, or $ for SEND_MONEY
  const result = await api("POST", "/actions", body);
  toast(result.completedImmediately
    ? `${actionType}: done`
    : `${actionType}: scheduled (resolves ~${new Date(result.completesAt).toLocaleTimeString()})`);
}
```

Notes that make the UI feel right:
- **Quantity** — most build/recruit actions accept `quantity` (default 1). `SEND_MONEY` uses `quantity` as the **dollar amount**.
- **Timed vs immediate** — if `completedImmediately` is false, the effect appears later (watch the news feed / your next poll). Optionally show a local "pending" chip until then.
- **Disable/annotate** actions the player can't do yet: `requiredResearch` set and not in `me.research` → show "needs {research}". `maxTotal` reached → disable. Insufficient `cash` → dim. (The server enforces all of this too and returns a `message`; the client hints are just UX.)
- **Don't hide leader-kill actions** on popularity — assassinate/decapitation are always accepted and simply fail if the target is too popular. Let players try.

---

## 8. Targeting other players

Targeted actions (`ATTACK`, `SEND_MONEY`, `DECLARE_WAR`, `ASSASSINATE_LEADER`, `GATHER_INTELLIGENCE`, …) require a `targetPlayerId`. Build the target picker from the **roster endpoint**:

```
GET /games/{gameId}/players   →  [ { id, username, nation, popularity, eliminated, admin } ]
```

```js
async function pickTarget() {
  const roster = await api("GET", `/games/${state.gameId}/players`);
  const targets = roster.filter(p => p.id !== state.playerId && !p.admin && !p.eliminated);
  // render targets (show nation + username + popularity) and resolve to the chosen p.id
}
```

It's the *limited public view* only (username, nation, popularity, eliminated, admin) — never force composition or arsenals; those come from recon/satellites. Cache it per game screen and refresh on your poll if you like (nations don't change, but popularity and eliminated do).

Games are found through `GET /games` (an open lobby); there are no join codes.

---

## 9. Errors, security, and edge cases

- **401** on any call → token missing/expired → clear state and return to the auth screen.
- **400** → the body's `message` is human-readable ("Insufficient funds…", "…requires researching…", "The admin does not control a nation…"). Show it as a toast; don't crash. Bean-validation failures (e.g. username < 3 chars, password < 4) come back in Spring's default shape *without* `message` — fall back to a generic text.
- **Admin has no nation** — if `player.nation` is null and `player.admin` is true, render an *admin console* (game status + Start/Pause/Resume) instead of a nation dashboard; the admin can't take nation actions.
- **Eliminated** — `player.eliminated === true` → show a defeated state; the server rejects further actions.
- **Election freeze** — while `frozenUntilTick > currentTick`, only military actions succeed; others return 400. You can pre-dim non-military actions when frozen.
- **403** on the news feed → you requested a feed that isn't yours; always poll with your own `playerId`.

---

## 10. Suggested files

The UI lives in `game-app/src/main/resources/static/` and is served by Spring on the same origin as the API (no CORS). Under the `develop` profile, static files are served straight from that source folder (run from the repo root), so edits show on refresh without a rebuild.

```
game-app/src/main/resources/static/
  index.html    // the three screens (#auth, #lobby, #game) + toast container
  styles.css    // phone-first: single column, big tap targets, sticky action bar
  app.js        // state, api(), screen flow, poll loop, render + submit
```

`index.html` skeleton:

```html
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="styles.css">
</head>
<body>
  <section id="auth">...login/register form...</section>
  <section id="lobby" hidden>...create / join...</section>
  <section id="game" hidden>
    <header id="dashboard">...nation, cash, popularity bar...</header>
    <main id="actions">...category sections...</main>
    <aside id="news">...feed...</aside>
    <div id="adminBar" hidden>...start/pause/resume...</div>
  </section>
  <div id="toasts"></div>
  <script src="app.js"></script>
</body>
</html>
```

### CSS tips for phone

- One column, `max-width: 520px; margin: 0 auto;` so it also looks fine on desktop.
- Big buttons (`min-height: 44px`), generous spacing, `position: sticky` action/nav bars.
- Use `@media (prefers-color-scheme: dark)` — leaders playing at night will thank you.
- A `<progress>` or a div-with-width for the popularity bar; color by threshold.

---

## 11. Build order (suggested)

1. `api()` helper + auth screen (register → login → token stored).
2. Lobby: create game (admin) / join by id; detect `RUNNING`.
3. Game screen: poll loop → render the `player` block only.
4. Action catalog fetch + render economy actions + submit (immediate ones first).
5. News feed rendering (it's your feedback channel for timed actions and combat).
6. Full dashboard sections + popularity bar.
7. Targeting (roster endpoint → target picker) → army/diplomacy/espionage actions.
8. Admin console (start/pause/resume) + eliminated/frozen states.

Keep it dumb and declarative: **poll → render everything from the response → post actions → repeat.** The server already does the hard part.
