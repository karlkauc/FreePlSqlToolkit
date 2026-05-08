# plsql-guidelines

Markdown source for the FreePlSqlToolkit PL/SQL coding guidelines site.
Built with [MkDocs Material](https://squidfunk.github.io/mkdocs-material/).

Each of the 15 built-in linter rules (`F-1` … `F-15`) has a section here;
the [Rule Mapping](docs/rules-mapping.md) page is the reverse index.

## Setup

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

## Build

```bash
.venv/bin/mkdocs build --strict          # static HTML to ./site/
.venv/bin/mkdocs serve                   # live preview at http://127.0.0.1:8000/
```

`--strict` fails the build on any broken internal link or missing
navigation entry. Treat it as the default for CI.

## Layout

```
docs/
├── index.md                    landing page
├── rules-mapping.md            ID → section table
├── style/
│   ├── identifiers.md          F-1, F-2
│   ├── naming.md               F-11, F-12, F-13
│   └── formatting.md           house style (no specific rules yet)
├── language/
│   ├── sql-usage.md            F-5, F-6, F-7
│   ├── exceptions.md           F-9, F-10
│   └── control-flow.md         F-8, F-15
├── design/
│   ├── packages.md             F-3
│   └── transactions.md         F-14
└── performance/
    └── bind-variables.md       F-4
```

## Deployment

Intended for GitHub Pages via a `mkdocs gh-deploy` step in CI (see
the planned `.github/workflows/ci.yml`, step 12 of the plan).
