# SQL Usage

PL/SQL is at its most useful when the embedded SQL is well-formed and
intentional. The rules in this section catch SQL anti-patterns that
slip through code review surprisingly often.

## F-5 · Avoid `SELECT *` {#f-5}

**Severity:** WARNING

`SELECT *` makes your code structurally dependent on the column order
and column count of the underlying tables. When the schema evolves —
even for a column you don't use — your PL/SQL silently breaks or
silently widens its result set. Always list the columns you actually
need.

```{.sql title="Bad"}
CREATE OR REPLACE PROCEDURE p_dirty IS
    l_row employees%ROWTYPE;
BEGIN
    SELECT * INTO l_row FROM employees WHERE rownum = 1;
END;
```

```{.sql title="Good"}
CREATE OR REPLACE PROCEDURE p_clean IS
    l_emp_id NUMBER;
    l_name   VARCHAR2(100);
BEGIN
    SELECT emp_id, name
      INTO l_emp_id, l_name
      FROM employees
     WHERE rownum = 1;
END;
```

The rule also flags `<table>.*` in joined queries.

## F-6 · `INSERT` requires an explicit column list {#f-6}

**Severity:** WARNING

Without a column list, an `INSERT` is positional — your code depends
on the table's exact `CREATE TABLE` order. A future `ALTER TABLE ADD
COLUMN` (or even a column reorder via DDL replay) silently changes
where each value lands.

```{.sql title="Bad"}
INSERT INTO employees VALUES ('Alice', 50000);
```

```{.sql title="Good"}
INSERT INTO employees (name, salary)
VALUES ('Alice', 50000);
```

## F-7 · `UPDATE` and `DELETE` must have a `WHERE` clause {#f-7}

**Severity:** ERROR

`UPDATE` or `DELETE` without `WHERE` is occasionally legitimate (e.g. a
batch reset job) but is far more often the result of forgetting a
condition. The rule fires as **ERROR** because the financial damage of
a missed `WHERE` in production is hard to undo.

```{.sql title="Bad"}
BEGIN
    UPDATE employees SET salary = salary * 1.1;   -- everyone gets 10%?
    DELETE FROM employees;                         -- the entire table?
END;
```

```{.sql title="Good"}
BEGIN
    UPDATE employees
       SET salary = salary * 1.1
     WHERE department_id = :p_dept;

    DELETE FROM employees
     WHERE status = 'TERMINATED'
       AND termination_date < ADD_MONTHS(SYSDATE, -84);
END;
```

If you genuinely need a table-wide update, suppress the rule
locally with a comment annotation (planned for v0.2) or refactor the
operation into a clearly-named procedure that documents the intent.
