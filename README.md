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
| `plsql-guidelines`    | PL/SQL coding guidelines as a static site (MkDocs Material)                       |
| `plsql-toolkit-app`   | JavaFX desktop app: editor + live lint + guidelines viewer                        |

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

Highlights:

- PL/SQL editor with syntax highlighting (RichTextFX)
- Debounced live lint (300 ms after the last keystroke)
- Lint-issues table with double-click-to-jump
- Read-only browser of the 15 built-in rules
- Light / dark theme toggle (AtlantaFX Primer)

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
