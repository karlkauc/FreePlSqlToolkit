# Bind Variables

Hardcoded literals in WHERE clauses, the cause of more shared-pool churn
than any other single PL/SQL anti-pattern.

## F-4 · Don't hardcode literals in `WHERE` clauses {#f-4}

**Severity:** INFO

Every distinct literal value in a SQL statement produces a *different*
SQL_ID in Oracle's library cache. Run the same lookup with 1 000
different IDs and you get 1 000 cache entries — and 1 000 hard parses.
Bind variables collapse all of those into a single shared parsed plan.

```{.sql title="Bad — every dept_id value re-parses"}
SELECT count(emp_id) INTO l_n
  FROM employees
 WHERE dept_id = 42;
```

```{.sql title="Good — one parse, reused"}
SELECT count(emp_id) INTO l_n
  FROM employees
 WHERE dept_id = p_dept_id;        -- PL/SQL implicitly binds p_dept_id
```

Inside PL/SQL, simple references to procedure variables and parameters
are bound automatically by the engine — that's why the rule fires only
on **literal** tokens (`UNSIGNED_INTEGER`, `CHAR_STRING`,
`APPROXIMATE_NUM_LIT`) inside a `where_clause`.

## When a literal is fine

- **Sentinel values** that the optimiser benefits from (e.g. small
  enums where bind peeking would harm plans). Tag the suppression
  inline once we add suppression annotations.
- **DDL or maintenance scripts** run once. They aren't subject to the
  shared-pool argument and are a fine place for literal `WHERE`
  clauses.

The rule is `INFO`-severity for that reason: it surfaces the
opportunity but does not block builds.
