# Rule Mapping

Reverse index from each linter rule ID to the section of this site that
documents it. The same IDs appear verbatim in `plsqllint`'s `--format
sarif` and `--format json` output and in the `plsql-toolkit-app` lint
side panel.

| ID    | Name                              | Severity | Documentation                                                  |
|-------|-----------------------------------|----------|----------------------------------------------------------------|
| F-1   | identifier-length                 | WARNING  | [Identifiers § F-1](style/identifiers.md#f-1)                  |
| F-2   | reserved-word-identifier          | WARNING  | [Identifiers § F-2](style/identifiers.md#f-2)                  |
| F-3   | package-global-variable           | WARNING  | [Packages § F-3](design/packages.md#f-3)                       |
| F-4   | literal-in-where-clause           | INFO     | [Bind Variables § F-4](performance/bind-variables.md#f-4)      |
| F-5   | select-star                       | WARNING  | [SQL Usage § F-5](language/sql-usage.md#f-5)                   |
| F-6   | insert-without-column-list        | WARNING  | [SQL Usage § F-6](language/sql-usage.md#f-6)                   |
| F-7   | update-delete-without-where       | ERROR    | [SQL Usage § F-7](language/sql-usage.md#f-7)                   |
| F-8   | if-exit-instead-of-exit-when      | INFO     | [Control Flow § F-8](language/control-flow.md#f-8)             |
| F-9   | when-others-without-raise         | ERROR    | [Exceptions § F-9](language/exceptions.md#f-9)                 |
| F-10  | generic-raise-application-error   | WARNING  | [Exceptions § F-10](language/exceptions.md#f-10)               |
| F-11  | package-naming                    | INFO     | [Naming § F-11](style/naming.md#f-11)                          |
| F-12  | local-var-prefix                  | WARNING  | [Naming § F-12](style/naming.md#f-12)                          |
| F-13  | in-parameter-prefix               | WARNING  | [Naming § F-13](style/naming.md#f-13)                          |
| F-14  | commit-in-trigger                 | ERROR    | [Transactions § F-14](design/transactions.md#f-14)             |
| F-15  | explicit-cursor                   | INFO     | [Control Flow § F-15](language/control-flow.md#f-15)           |

## Custom rules (YAML / SPI)

Rules supplied via `plsqllint --rules custom.yaml` or via Java SPI
plug-ins keep their declared `id` and `name` in all reporters and in
the `plsql-toolkit-app` UI. They appear in lint output but **not** in
this mapping table — by convention, document them in your own
project's docs and link out to them in the linter's `--rules` config.
