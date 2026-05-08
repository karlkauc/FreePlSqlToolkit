# Formatting

This page collects formatting conventions that the linter does **not** check
yet but that the project nevertheless follows. Treat them as house style
rather than enforced rules.

## Keywords

Use **upper case** for SQL and PL/SQL keywords. This makes the structural
spine of a statement visible at a glance.

```sql
SELECT emp_id, name
  FROM employees
 WHERE department_id = :p_dept;
```

## Indentation

- 4 spaces per level. No tabs.
- `BEGIN`, `EXCEPTION`, and `END` start at the level of their enclosing
  block (no extra indent).
- Inside `IF`/`CASE`/`LOOP`, indent the body one extra level.

## Line length

Soft limit at 100 columns. Break long expressions on operators
(`AND`, `OR`, `||`) at the start of the next line, indented under the
parent expression.

```sql
SELECT emp_id, name, salary
  FROM employees
 WHERE department_id = :p_dept
   AND hire_date >= :p_since
   AND status     = 'ACTIVE';
```

## Argument lists

For procedure/function calls with more than two arguments, use named
notation (`=>`) and one parameter per line:

```sql
pkg_employees.hire(
    p_name      => 'Alice',
    p_email     => 'alice@example.com',
    p_salary    => 50000,
    p_dept_id   => 10
);
```

## Comments

- `--` for single-line comments.
- `/* … */` for block comments and disabled code.
- Keep comments focused on **why**, not **what**: well-named identifiers
  already say what; the comment should explain non-obvious intent.
