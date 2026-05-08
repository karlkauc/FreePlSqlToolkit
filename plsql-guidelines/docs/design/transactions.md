# Transactions

PL/SQL inherits its transactional semantics from the calling SQL
session. A trigger or callback that issues `COMMIT` or `ROLLBACK` is
making a unilateral decision on behalf of the caller — almost always
the wrong call.

## F-14 · No `COMMIT` inside a trigger {#f-14}

**Severity:** ERROR

A `COMMIT` inside a row-level or statement-level trigger ends the
*calling* statement's transaction prematurely. The change that
fired the trigger then can no longer be rolled back, and any error
that surfaces later in the same transaction loses its anchor.

```{.sql title="Bad"}
CREATE OR REPLACE TRIGGER trg_audit
AFTER INSERT ON employees
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (event, ts) VALUES ('hire', SYSDATE);
    COMMIT;                                            -- breaks the caller
END;
```

```{.sql title="Good"}
CREATE OR REPLACE TRIGGER trg_audit
AFTER INSERT ON employees
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (event, ts) VALUES ('hire', SYSDATE);
    -- transactional consistency stays with the calling statement
END;
```

If you genuinely need to commit logs *independently* of the caller —
for instance, audit entries that must persist even if the caller rolls
back — use an **autonomous transaction** in a *separate* procedure
called from the trigger:

```{.sql title="Recommended pattern"}
CREATE OR REPLACE PROCEDURE log_audit_event(p_event VARCHAR2) IS
    PRAGMA AUTONOMOUS_TRANSACTION;
BEGIN
    INSERT INTO audit_log (event, ts) VALUES (p_event, SYSDATE);
    COMMIT;
END;

CREATE OR REPLACE TRIGGER trg_audit
AFTER INSERT ON employees
FOR EACH ROW
BEGIN
    log_audit_event('hire');
END;
```

The same caution applies to `ROLLBACK` and `SAVEPOINT` inside triggers,
even though the linter currently flags only `COMMIT`.
