# Naming Conventions

Consistent prefixes turn local code into self-documenting code: a reader
who sees `l_count` knows immediately that it is a local variable, not a
package global, parameter, or column. The rules below enforce three
prefix conventions used widely in the PL/SQL community.

## F-11 · Package names start with `pkg_` {#f-11}

**Severity:** INFO

Packages group related procedures, functions, and types. A `pkg_` prefix
makes them stand out from tables, views, and procedures in object
explorers and Git diffs.

```{.sql title="Bad"}
CREATE OR REPLACE PACKAGE employees_api AS
    PROCEDURE hire(p_name IN VARCHAR2);
END employees_api;
```

```{.sql title="Good"}
CREATE OR REPLACE PACKAGE pkg_employees AS
    PROCEDURE hire(p_name IN VARCHAR2);
END pkg_employees;
```

## F-12 · Local variables use the `l_` prefix {#f-12}

**Severity:** WARNING

Within a procedure or function, the `l_` prefix lets a reader spot
local declarations at a glance and rules out collision with table
columns of the same logical name.

```{.sql title="Bad"}
CREATE OR REPLACE PROCEDURE p_bad_naming IS
    counter NUMBER;        -- column? variable? package global?
BEGIN
    counter := 0;
END;
```

```{.sql title="Good"}
CREATE OR REPLACE PROCEDURE p_good_naming IS
    l_counter NUMBER;
BEGIN
    l_counter := 0;
END;
```

Constants are exempt — see [F-3](../design/packages.md#f-3) for the
related package-global rule and use a `c_` prefix for them by convention.

## F-13 · `IN` parameters use the `p_` prefix {#f-13}

**Severity:** WARNING

Inside the body of a procedure, an unprefixed parameter name is hard to
distinguish from a local variable or a column reference inside SQL
statements. The `p_` prefix removes that ambiguity entirely.

```{.sql title="Bad"}
CREATE OR REPLACE PROCEDURE p_hire(name IN VARCHAR2, salary IN NUMBER) IS
BEGIN
    INSERT INTO employees (name, salary) VALUES (name, salary);  -- ambiguous
END;
```

```{.sql title="Good"}
CREATE OR REPLACE PROCEDURE p_hire(p_name IN VARCHAR2, p_salary IN NUMBER) IS
BEGIN
    INSERT INTO employees (name, salary) VALUES (p_name, p_salary);
END;
```

The rule does not currently flag `OUT`/`IN OUT` parameters; suggested
prefixes for those are `o_` and `io_` respectively, but the linter is
deliberately conservative on them in v0.1.
