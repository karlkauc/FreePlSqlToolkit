# FreePlSqlToolkit

A free, open-source toolkit for PL/SQL development.

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## What's inside

| Module                | Purpose                                                                           |
| --------------------- | --------------------------------------------------------------------------------- |
| `plsql-parser`        | ANTLR-based PL/SQL parser library                                                 |
| `plsql-linter-core`   | Rule engine, 15 built-in rules (`F-1`…`F-15`), custom rules via YAML and Java SPI |
| `plsql-linter-cli`    | Command-line linter (`plsqllint`) with `text`, `json`, and `sarif` output         |
| `plsql-db-sync`       | Periodic Oracle DB → Git sync of PL/SQL source (`plsqlsync`)                      |
| `plsql-app-services`  | Pure-Java service layer for the desktop app (connections, metadata, lint, search, …) |
| `plsql-guidelines`    | PL/SQL coding guidelines as a static site (MkDocs Material)                       |
| `plsql-toolkit-app`   | JavaFX desktop app: multi-DB analysis workbench (editor, live lint, schema navigator, batch lint, search, metrics, schema diff, snapshot) |

Library-first architecture — every module is usable on its own; the CLIs and
the desktop app are thin frontends.

## Prerequisites

- **Java 21** (the Gradle toolchain will fetch one if it is missing)
- **Python 3** with `venv` — only needed to build the guidelines site
- An **Oracle database** reachable over JDBC — only needed for `plsql-db-sync`

## Build everything

```bash
./gradlew build
```

This compiles all modules, runs the unit tests, and produces the fat JARs
for the two CLIs.

## Run the linter (`plsqllint`)

Build the fat JAR:

```bash
./gradlew :plsql-linter-cli:shadowJar
```

Lint a single file or a whole directory tree (recurses into `.sql`, `.pks`,
`.pkb`, and `.plsql` files):

```bash
java -jar plsql-linter-cli/build/libs/plsqllint-0.1.0-SNAPSHOT-all.jar \
    check --format sarif --output report.sarif path/to/your.sql
```

Useful flags:

- `-f`, `--format` — `text` (default), `json`, or `sarif`
- `-o`, `--output FILE` — write the report to a file instead of stdout
- `--fail-on INFO|WARNING|ERROR` — minimum severity that triggers a non-zero
  exit code (default: `ERROR`)
- `-r`, `--rules my-rules.yml` — load additional rules from a YAML file

## Run the DB → Git sync (`plsqlsync`)

Build the fat JAR:

```bash
./gradlew :plsql-db-sync:shadowJar
```

Generate a starter config, fill in the connection details, then run a single
pass or a long-running loop:

```bash
SYNC=plsql-db-sync/build/libs/plsqlsync-0.1.0-SNAPSHOT-all.jar

java -jar "$SYNC" init
# edit plsqlsync.yaml: connection URL, user, password, schemas, repo path

java -jar "$SYNC" once --config plsqlsync.yaml   # single pass, then exit
java -jar "$SYNC" run  --config plsqlsync.yaml   # loop on schedule.intervalMinutes
```

The `run` subcommand keeps polling the database and committing changes to the
configured Git repository on every interval until you interrupt it.

## Run the JavaFX desktop app

```bash
./gradlew :plsql-toolkit-app:run
```

The desktop app is a **multi-database PL/SQL analysis workbench**. On first
launch it asks for a master password to encrypt the connection-profile file
(`~/.fpltoolkit/profiles.enc`, AES-256/GCM, PBKDF2-SHA256 with 600 000
iterations); subsequent starts unlock it.

Highlights (v0.2):

- **Multi-connection** — keep several Oracle profiles connected at once
  (Easy Connect, TNS-Names, Oracle Wallet, Kerberos)
- **Schema Navigator** — lazy tree of every schema's packages, procedures,
  functions, triggers, views, and types
- **Editor tabs** for local files (`.sql`/`.pks`/`.pkb`) *and* read-only DDL
  tabs for any DB object — both with syntax highlighting and live lint
- **Batch lint** — right-click any schema → "Lint Schema…", export the
  report as Markdown, HTML, or SARIF 2.1.0
- **DB-wide search** — regex or literal pattern over `ALL_SOURCE` across
  every active connection
- **Code metrics** — LOC, SLOC, cyclomatic complexity (CCN), and lint-issue
  count per object, sortable
- **Cross-references** — "calls" and "called by" tables for any object
  via `ALL_DEPENDENCIES`
- **Schema Diff** — pick two connections + schemas, see added / removed /
  modified objects with unified-diff DDL
- **Invalid-objects dashboard** — `DBA_ERRORS` (falls back to `USER_ERRORS`)
  with line, position, and message
- **DB → Git snapshot** — one-click DDL dump using the existing
  `plsql-db-sync` engine
- **Workspace persistence** — re-opens the same local-file tabs and window
  geometry on the next start (DB tabs reopen after you reconnect)
- **AtlantaFX Primer** light / dark theme

## Build and serve the guidelines site

```bash
cd plsql-guidelines
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

.venv/bin/mkdocs build --strict   # static HTML to ./site/
.venv/bin/mkdocs serve            # live preview at http://127.0.0.1:8000/
```

The `--strict` flag fails the build on any broken internal link or missing
nav entry. A deployed copy lives at
<https://karlkauc.github.io/FreePlSqlToolkit/>.

## License

Apache License 2.0 — see [LICENSE](LICENSE).

The PL/SQL ANTLR grammar is derived from
[antlr/grammars-v4](https://github.com/antlr/grammars-v4), licensed under
the BSD 3-Clause License.
