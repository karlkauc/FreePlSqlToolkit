# Exceptions

Exception handling is the part of PL/SQL where small mistakes cause the
biggest problems: silent failures, misleading error codes, and lost
stack traces all start with sloppy exception clauses. The rules in
this section enforce two essentials.

## F-9 · `WHEN OTHERS` must re-raise {#f-9}

**Severity:** ERROR

`WHEN OTHERS THEN NULL;` swallows every error — including the ones you
wrote `WHEN OTHERS` to log. The result is silent corruption: a
procedure returns "successfully" while half its work failed.

The rule fires whenever a `WHEN OTHERS` handler does **not** call
`RAISE`, `RAISE_APPLICATION_ERROR`, or contain those keywords in any
re-thrown form.

```{.sql title="Bad"}
BEGIN
    SELECT count(*) INTO l_count FROM employees;
EXCEPTION
    WHEN OTHERS THEN
        NULL;             -- silent failure
END;
```

```{.sql title="Good — log and re-raise"}
BEGIN
    SELECT count(*) INTO l_count FROM employees;
EXCEPTION
    WHEN OTHERS THEN
        pkg_logger.error('count failed: ' || SQLERRM);
        RAISE;
END;
```

```{.sql title="Good — translate to a meaningful application error"}
BEGIN
    SELECT count(*) INTO l_count FROM employees;
EXCEPTION
    WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20101, 'employee count failed: ' || SQLERRM);
END;
```

## F-10 · Don't use the generic error code `-20000` {#f-10}

**Severity:** WARNING

Oracle reserves the range `-20000` to `-20999` for application errors,
and many developers reach for `-20000` as a default. Using a unique
code per error site (or per category) lets you grep, alert, and
translate errors deterministically.

```{.sql title="Bad"}
RAISE_APPLICATION_ERROR(-20000, 'something failed');
```

```{.sql title="Good"}
RAISE_APPLICATION_ERROR(-20101, 'employee not found: ' || p_emp_id);
```

A common pattern is to maintain a shared package of named error codes:

```{.sql title="Recommended"}
CREATE OR REPLACE PACKAGE pkg_errors AS
    c_employee_not_found CONSTANT PLS_INTEGER := -20101;
    c_invalid_salary     CONSTANT PLS_INTEGER := -20102;
    -- …
END pkg_errors;
```
