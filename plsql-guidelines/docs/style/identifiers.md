# Identifiers

Identifiers carry a lot of meaning — column names, variables, package names —
and Oracle's compiler is mostly indifferent to how you choose them. The
rules in this section guard against the few cases where bad identifier
choices break tooling, readability, or future portability.

## F-1 · Identifier length ≤ 30 characters {#f-1}

**Severity:** WARNING

Oracle 12.2+ allows identifiers up to 128 characters, but many third-party
tools, naming conventions, and DBA scripts still assume the legacy 30-char
limit. Long identifiers also tend to bury intent in noise.

```{.sql title="Bad"}
DECLARE
    l_a_very_long_identifier_name_that_exceeds_thirty_chars NUMBER;
BEGIN
    NULL;
END;
```

```{.sql title="Good"}
DECLARE
    l_count NUMBER;
BEGIN
    NULL;
END;
```

If you really need longer names — for instance because they reflect a
business glossary — disable F-1 in your `rules.yaml` rather than fighting
the linter.

## F-2 · Reserved words and built-in types as identifiers {#f-2}

**Severity:** WARNING

Using built-in type names like `NUMBER`, `DATE`, `VARCHAR2`, `BLOB`, or
ambiguous words like `LEVEL`, `USER`, `SESSION`, or `ROW` as identifiers
forces readers to mentally disambiguate every reference. It also makes
quoted-identifier hacks (`"NUMBER"`) more likely later on.

```{.sql title="Bad"}
CREATE OR REPLACE PROCEDURE p_x IS
    l_date NUMBER;       -- l_date is a number?
    "NUMBER" NUMBER;     -- quoted identifier just to dodge the keyword
BEGIN
    NULL;
END;
```

```{.sql title="Good"}
CREATE OR REPLACE PROCEDURE p_x IS
    l_hire_date DATE;
    l_amount    NUMBER;
BEGIN
    NULL;
END;
```

The default deny list covers the most common offenders. Extend it for
your project via the `forbidden-keyword` YAML rule type.
