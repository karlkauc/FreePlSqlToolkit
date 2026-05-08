# plsql-db-sync

Periodically extracts PL/SQL source from an Oracle database and commits it to a
Git repository, so DB-resident code becomes diffable and reviewable.

## What it does

1. Connect to the Oracle database via JDBC.
2. List all PL/SQL objects (`PACKAGE`, `PACKAGE BODY`, `PROCEDURE`, `FUNCTION`,
   `TRIGGER`, `VIEW`, `TYPE`, `TYPE BODY`) and their `last_ddl_time`.
3. Diff against the local state file (`.plsqlsync-state.json`) to find changed
   objects.
4. Pull the DDL of each changed object via `DBMS_METADATA.GET_DDL`.
5. Write each object to `<repo>/<schema>/<type>/<name>.sql`.
6. Stage and commit via JGit. Optionally push (configurable).

This is the **pull** flavor (v0.1). A push flavor based on a DDL trigger +
audit queue is planned for v0.2.

## CLI

The Fat-JAR is `plsqlsync-0.1.0-SNAPSHOT-all.jar`. Three subcommands:

```
plsqlsync init    [-o plsqlsync.yaml]      # Write a starter config
plsqlsync once    -c plsqlsync.yaml        # Run a single sync pass
plsqlsync run     -c plsqlsync.yaml        # Loop on the configured interval
```

## Configuration (`plsqlsync.yaml`)

```yaml
connection:
  url: jdbc:oracle:thin:@host:1521/XEPDB1
  user: ${DB_USER}
  password: ${DB_PASSWORD}
schemas:
  - HR
output:
  repo: ./plsql-source
  branch: main
  commitAuthor: "PLSQLSync <plsqlsync@example.com>"
  push: false
schedule:
  intervalMinutes: 5
```

Environment variables can be referenced as `${VAR}` in `user` and `password`.

## TODO (post-v0.1)

- **End-to-end integration test** against a live Oracle XE container.
  Initial Testcontainers setup hit an API-version mismatch between the
  Docker server (1.44+) and the docker-java client bundled with
  Testcontainers 1.20.4 (1.32). Re-investigate when bumping Testcontainers
  or once a workaround is verified in CI.
- **Push flavor** based on a DDL trigger + audit queue.
- **Custom commit-message templates** including diff stats per file.
