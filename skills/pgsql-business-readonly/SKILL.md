---
name: pgsql-business-readonly
description: Connect to the canteen-smile PostgreSQL instance with the local psql client and pgpass file to inspect schemas or answer business-data questions using strictly read-only queries. Use when Codex needs to read, compare, summarize, validate, or troubleshoot data in the smile_auth or smile_iam databases, including listing tables, inspecting columns/comments, checking counts, and correlating authentication and IAM business data.
---

# PostgreSQL Business Read-Only

Use the repository's known local connection configuration to answer business questions from `smile_auth` and `smile_iam` without changing database state.

## Connection configuration

- Client: `E:\soft\pgsql\bin\psql.exe`
- Credential file: `E:\soft\pgsql\pgpass.conf`
- Host: `192.168.0.228`
- Port: `5432`
- User: `postgres`
- Allowed databases: `smile_auth`, `smile_iam`
- Default schema: `public`

Never print, parse into output, copy, edit, or commit the password from `pgpass.conf`. Set `PGPASSFILE` only for the spawned process.

## Read-only workflow

1. Identify which allowed database owns the needed business data.
2. Inspect table and column metadata before writing a business query. Do not guess table names, fields, enum meanings, joins, or tenant semantics.
3. Run only read-only SQL. Force `default_transaction_read_only=on`, set a statement timeout, stop on the first error, and disable the pager.
4. Select only fields needed for the user's question. Avoid `SELECT *`, bound broad results with `LIMIT`, and aggregate in PostgreSQL where appropriate.
5. Treat credentials, mobile numbers, identity data, tokens, recovery codes, and audit payloads as sensitive. Do not retrieve or display them unless explicitly necessary and authorized; minimize or mask them in the response.
6. Report the database, query scope, and material assumptions. State that no data was modified.

Do not execute `INSERT`, `UPDATE`, `DELETE`, `MERGE`, DDL, `COPY ... TO PROGRAM`, procedure calls, maintenance commands, or any SQL that may mutate state. If the user requests a write, do not use this skill for that action.

## Command template

In PowerShell, set process-local connection variables and invoke the absolute client path:

```powershell
$env:PGPASSFILE = 'E:\soft\pgsql\pgpass.conf'
$env:PGCONNECT_TIMEOUT = '8'
$env:PGOPTIONS = '-c default_transaction_read_only=on -c statement_timeout=15000'
& 'E:\soft\pgsql\bin\psql.exe' -X -w -v ON_ERROR_STOP=1 -P pager=off -h 192.168.0.228 -p 5432 -U postgres -d smile_auth -c '<READ_ONLY_SQL>'
```

Replace only the database with `smile_iam` when needed. Keep `-X`, `-w`, `ON_ERROR_STOP`, `pager=off`, and `PGOPTIONS` intact. Request sandbox escalation when the local execution environment blocks access to the private network.

## Schema discovery

List tables:

```powershell
& 'E:\soft\pgsql\bin\psql.exe' -X -w -v ON_ERROR_STOP=1 -P pager=off -h 192.168.0.228 -p 5432 -U postgres -d smile_auth -c '\dt public.*'
```

Inspect a confirmed table:

```powershell
& 'E:\soft\pgsql\bin\psql.exe' -X -w -v ON_ERROR_STOP=1 -P pager=off -h 192.168.0.228 -p 5432 -U postgres -d smile_auth -c '\d+ public.<table_name>'
```

Read table and column comments when business meanings are unclear:

```sql
SELECT
    c.table_name,
    c.column_name,
    c.data_type,
    c.is_nullable,
    pg_catalog.col_description(
        format('%I.%I', c.table_schema, c.table_name)::regclass::oid,
        c.ordinal_position
    ) AS column_comment
FROM information_schema.columns AS c
WHERE c.table_schema = 'public'
  AND c.table_name = '<table_name>'
ORDER BY c.ordinal_position;
```

Inspect keys and relationships from catalog metadata before joining tables. Confirm business semantics from repository SQL or code when database comments and constraints are insufficient.

## Cross-database business analysis

PostgreSQL does not directly join ordinary tables across separate databases. Query `smile_auth` and `smile_iam` separately with narrow result sets, then correlate the returned rows locally using confirmed identifiers. Do not install or invoke `dblink`, foreign data wrappers, extensions, temporary tables, or other server-side changes.

Prefer aggregate or identifier-only intermediate results. Never persist extracted production data into the repository. If temporary local output is essential, use the task temporary directory, minimize sensitive values, and remove it after analysis.

## Failure handling

- `fe_sendauth: no password supplied`: confirm `PGPASSFILE` is exactly `E:\soft\pgsql\pgpass.conf` and that the selected database has a matching entry. Do not display the file contents.
- `Permission denied (10013)`: rerun the same command with sandbox network escalation; do not change connection settings.
- Timeout: narrow the query or inspect indexes with catalog metadata. Do not remove the statement timeout for an unbounded query.
- Missing or ambiguous schema evidence: stop the affected analysis and mark it `TODO(待确认)` rather than inventing a contract.
