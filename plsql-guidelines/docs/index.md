# FreePlSqlToolkit Guidelines

A focused set of PL/SQL & SQL coding rules used by the
[FreePlSqlToolkit](https://github.com/karlkauc/FreePlSqlToolkit) linter and
JavaFX IDE.

This site is **not** a fork of the Trivadis PL/SQL & SQL Coding Guidelines —
it is an independent, deliberately compact subset structured around the
15 rules currently shipped by the linter. Each rule has a stable ID
(`F-1` … `F-15`) that maps 1:1 to the linter's `--format sarif`/`--format json`
output and to the IDE's lint side panel.

## How to read this site

- **Style** covers identifiers, naming conventions, and formatting.
- **Language** covers SQL usage patterns, exception handling, and control flow.
- **Design** covers package structure and transaction boundaries.
- **Performance** covers bind variables and execution-plan stability.
- **[Rule Mapping](rules-mapping.md)** is the reverse index from rule IDs
  back to the section that explains them.

## Conventions used in examples

```{.sql .copy title="Bad — what the rule flags"}
-- This snippet violates the rule.
SELECT * FROM employees WHERE name = 'Alice';
```

```{.sql .copy title="Good — preferred form"}
SELECT emp_id, name, salary FROM employees WHERE name = :p_name;
```

!!! info "Severity levels"
    - **ERROR** — almost always a bug or correctness risk; build should fail.
    - **WARNING** — strong style/maintainability convention.
    - **INFO** — helpful suggestion; not a build breaker.

## Tooling

The rules in this site are checked automatically by:

- **`plsqllint`** — CLI for CI/CD with SARIF + JSON + text reporters.
- **`plsql-toolkit-app`** — JavaFX desktop IDE with live-lint markers.

Both load the same rule set; companies can extend it via YAML or Java SPI
without changing the linter source.
