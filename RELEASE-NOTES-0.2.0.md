# FreePlSqlToolkit v0.2.0

Released 2026-05-09.

The headline change is a **complete rebuild of the JavaFX desktop app** — from
a single-document editor with live lint into a multi-database PL/SQL
**analysis workbench**. The CLIs (`plsqllint`, `plsqlsync`) are unchanged.

## New module

- `plsql-app-services` — pure-Java service layer (no JavaFX), used by the
  desktop app and reusable from CLIs. Owns connection management, schema
  metadata access, batch lint, code search, dependency analysis, metrics,
  schema diffing, snapshotting, and persistence.

## Desktop app — `plsql-toolkit-app`

- **Multi-database connection manager** with HikariCP pools, supporting
  Easy Connect, TNS-Names (`$TNS_ADMIN`), Oracle Wallet (`?TNS_ADMIN=…`),
  and Kerberos (`oracle.net.authentication_services=(KERBEROS5)`).
- **Encrypted connection profiles** in `~/.fpltoolkit/profiles.enc`:
  AES-256/GCM/NoPadding, PBKDF2-HMAC-SHA256 with 600 000 iterations, fresh
  IV per write, atomic file rewrite.
- **Sidebar accordion** with Connections (CRUD + Connect/Disconnect),
  Schema Navigator (lazy tree), and Local Files (folder browser).
- **Tabs** for local PL/SQL files (debounced live lint) and read-only DB
  object DDL (lint runs once on load).
- **Tools**:
  - DB Search — regex/literal over `ALL_SOURCE`, jumps to source line
  - Metrics — LOC, SLOC, cyclomatic complexity, issue count per object
  - Invalid Objects — `DBA_ERRORS` / `USER_ERRORS`
  - Schema Diff — added/removed/modified objects with unified-diff DDL
    (powered by java-diff-utils)
  - Snapshot to Git — one-click DDL dump to a Git repo (wraps `plsqlsync`)
  - Right-click "Lint Schema…" — exports Markdown / HTML / SARIF 2.1.0
  - Dependency view — calls / called-by tables from `ALL_DEPENDENCIES`
- **Workspace persistence** — window geometry, theme, and open local-file
  tabs survive restarts (`~/.fpltoolkit/{settings,workspace}.json`).
- **Master-password dialog** as the first thing on every launch.

## Read-only by design

The app is positioned as a complement to SQL Developer / TOAD, not a
replacement. There is no DML execute, no `CREATE OR REPLACE` against the
database, no compile-against-DB, and no debugger. The only write path is
`Tools → Snapshot to Git`, which goes through `plsql-db-sync`.

## Stack

- Java 21, Gradle 9.5, JavaFX 22.0.2
- HikariCP 5.1.0, java-diff-utils 4.12, Jackson 2.18.1
- Oracle JDBC `ojdbc11` 23.6
- AtlantaFX 2.1.0 (Primer light/dark), RichTextFX 0.11.7, Ikonli 12.4.0

## Known limitations (v0.3 backlog)

- Kerberos is wired through the strategy interface but not exercised in CI;
  treat as best-effort and report breakage on the issue tracker.
- DB-tab restore on cold start is deferred — only local-file tabs come back
  automatically; DB tabs need a manual reconnect.
- LocalFileNavigator does not yet watch the filesystem for changes; use
  the Refresh button.
- Dependency view is tabular only — the visual force-directed graph from
  the original spec is deferred.
- Snapshot uses `DriverManager` under the hood, so Kerberos profiles do
  not flow their auth properties yet.
- Testcontainers integration tests for `plsql-db-sync` remain skipped
  pending a Docker/Testcontainers compatibility fix.

## Verification

```bash
./gradlew test
```

50+ unit tests across `plsql-app-services` and `plsql-toolkit-app` —
crypto, persistence, OracleAuthStrategy URL building, lint report
rendering, code-metrics calculation, syntax highlighting, app-context
roundtrip.
