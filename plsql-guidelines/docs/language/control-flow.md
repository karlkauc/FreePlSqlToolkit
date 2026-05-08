# Control Flow

Two rules in this section nudge you toward PL/SQL's idiomatic loop
constructs and away from C-style or COBOL-style patterns that tend to
creep in.

## F-8 · Use `EXIT WHEN` instead of `IF … EXIT; END IF` {#f-8}

**Severity:** INFO

`EXIT WHEN <condition>` reads naturally and matches the intent
("leave the loop when the condition is true"). The wrapped `IF … EXIT;
END IF` pattern is three extra lines that say the same thing.

```{.sql title="Bad"}
LOOP
    IF rownum > 10 THEN
        EXIT;
    END IF;
END LOOP;
```

```{.sql title="Good"}
LOOP
    EXIT WHEN rownum > 10;
END LOOP;
```

The rule only fires for the simple shape (single `EXIT` body, no
`ELSE`, no `ELSIF`). Conditional logic with side effects in the same
`IF` block is left alone.

## F-15 · Prefer cursor `FOR` loops over explicit `OPEN`/`FETCH`/`CLOSE` {#f-15}

**Severity:** INFO

A cursor `FOR` loop is fewer lines, cannot leak the cursor (no missed
`CLOSE`), and reads top-to-bottom in the same direction as the data
flow. The explicit form has its place — bulk fetches, conditional
fetches, parameterised cursors used in multiple loops — but the
default should be the `FOR` loop.

```{.sql title="Bad"}
DECLARE
    CURSOR c_emps IS SELECT emp_id, name FROM employees;
    l_id   NUMBER;
    l_name VARCHAR2(100);
BEGIN
    OPEN c_emps;
    LOOP
        FETCH c_emps INTO l_id, l_name;
        EXIT WHEN c_emps%NOTFOUND;
        process(l_id, l_name);
    END LOOP;
    CLOSE c_emps;
END;
```

```{.sql title="Good"}
DECLARE
    CURSOR c_emps IS SELECT emp_id, name FROM employees;
BEGIN
    FOR rec IN c_emps LOOP
        process(rec.emp_id, rec.name);
    END LOOP;
END;
```

For collections, prefer `BULK COLLECT INTO … LIMIT` over a `FOR` loop
when the result set can be large; that is a separate convention not
yet covered by a dedicated rule.
