# Package Design

Packages are the natural unit of modularisation in PL/SQL. The rule
below addresses the most common design mistake: package globals.

## F-3 · Avoid package-level variables (globals) {#f-3}

**Severity:** WARNING

A non-`CONSTANT` variable declared in a package spec or body becomes
a session-level global. That sounds convenient, but it has costly
side effects:

- It survives across calls, so the package's behaviour depends on
  state set by an earlier (possibly unrelated) call in the same
  session.
- It interacts in surprising ways with `SERIALLY_REUSABLE` packages
  and connection pools.
- Concurrent calls within the same session must coordinate access.

Constants are exempt — they are read-only and safe.

```{.sql title="Bad"}
CREATE OR REPLACE PACKAGE pkg_employees AS
    g_counter NUMBER;                              -- session global
END pkg_employees;
```

```{.sql title="Good"}
CREATE OR REPLACE PACKAGE pkg_employees AS
    c_max_salary CONSTANT NUMBER := 200000;        -- safe: constant
END pkg_employees;
```

If you genuinely need shared state, model it explicitly:

- **Pass it through arguments.** Most "globals" are really inputs in
  disguise.
- **Persist it in a table.** If the state has to outlive a call, it
  almost always has to outlive a session too.
- **Use `dbms_session.set_context` / `sys_context`.** Designed for
  per-session settings, with predictable semantics under reuse.

The rule does not look inside procedure or function bodies — local
variables are encouraged and have a different rule (see
[F-12](../style/naming.md#f-12)).
