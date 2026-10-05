# xmas-game

Real-time multiplayer strategy game backend. Maven multi-module, Spring Boot 4, Java 24.

## Modules

| Module | Responsibility |
|--------|----------------|
| `game-common` | Shared domain enums (`Nation`), error hierarchy (`ApiException`) |
| `game-account` | Registration, login, session store (Redis prod / in-memory develop) |
| `game-app` | Spring Boot application; imports each module's `@Configuration`, serves all OpenAPI specs, owns Liquibase changelog |

Each business-area module owns its OpenAPI spec under `src/main/resources/openapi` and generates
`@Controller` interfaces at build time (`openapi-generator-maven-plugin`). The app module scans only
`com.xmas.game.app` and `@Import`s each module's `@Configuration`.

## Build & test

```bash
mvn clean test
```

## Run locally (develop profile: H2 + in-memory sessions, no Postgres/Redis needed)

Run from the repo root (the develop profile serves the UI straight from source):

```bash
mvn -pl game-app -am -DskipTests package
SPRING_PROFILES_ACTIVE=develop java -jar game-app/target/game-app-0.0.1-SNAPSHOT.jar
```

`develop` uses the real game timings, so it's what you play a game night on. For quick
testing add the `fast` profile (`SPRING_PROFILES_ACTIVE=develop,fast`): builds and research
become instant, strikes and spy ops take 5 seconds, and election/war-strain timers are short.
Data lives in memory either way and is lost when the server stops.

Then:

```bash
curl -X POST http://localhost:8080/account/register -H 'Content-Type: application/json' -d '{"username":"alice","password":"secret1"}'
curl -X POST http://localhost:8080/account/login    -H 'Content-Type: application/json' -d '{"username":"alice","password":"secret1"}'
```

## Conventions

- API-first: OpenAPI spec per business area → generated controller interfaces → hand-written `@RestController` → `@Service` → repository.
- Data carriers (generated DTO, JPA entity) hold no logic; mapping in static `*Mapper` classes.
- `@Service` uses constructor injection via `@RequiredArgsConstructor`; `@Slf4j` for logging.
- 3rd-party integrations: interface + production impl + `@Profile("develop")` mock.
- UUID identifiers, `Instant` for time, no enum ordinals, no records.
