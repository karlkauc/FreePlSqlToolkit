# FreePlSqlToolkit

A free, open-source toolkit for PL/SQL development.

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## What's inside

| Module                | Purpose                                                                |
| --------------------- | ---------------------------------------------------------------------- |
| `plsql-parser`        | ANTLR-based PL/SQL parser library                                      |
| `plsql-linter-core`   | Rule engine, built-in rules, custom rules via YAML and Java SPI        |
| `plsql-linter-cli`    | Command-line linter with `text`, `json`, and `sarif` output formats    |
| `plsql-db-sync`       | (planned) Periodic Oracle DB → Git sync of PL/SQL source               |
| `plsql-guidelines`    | (planned) PL/SQL coding guidelines as a static HTML site (MkDocs)      |
| `plsql-toolkit-app`   | (planned) JavaFX desktop app: editor + live lint + guidelines          |

## Quick start

Build everything:

```bash
./gradlew build
```

Run the linter against a file or directory:

```bash
java -jar plsql-linter-cli/build/libs/plsql-linter-cli-all.jar \
    check --format sarif --output report.sarif path/to/your.sql
```

## Status

`v0.1` is in active development. The parser, rule engine, ~3 pilot rules, and the
CLI with SARIF output form the first demo milestone. The remaining modules
(`plsql-db-sync`, `plsql-guidelines`, `plsql-toolkit-app`) follow in subsequent
iterations.

## License

Apache License 2.0 — see [LICENSE](LICENSE).

The PL/SQL ANTLR grammar is derived from
[antlr/grammars-v4](https://github.com/antlr/grammars-v4), licensed under
the BSD 3-Clause License.
