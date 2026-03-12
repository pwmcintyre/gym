# ADR-002: Cloud Backup Storage Uses Google Drive appDataFolder

**Status:** Accepted
**Date:** 2026-03-12

---

# Context

Milestone 6 needs a durable cloud backup for local workout state without introducing a custom backend.

Two candidate stores were considered:

1. Google Drive `appDataFolder`
2. Google Sheets

The app’s backup data is structured application state, not collaborative spreadsheet content. The storage choice needs to support:

- a single durable backup blob,
- simple restore semantics,
- minimal user-facing clutter,
- low-maintenance implementation,
- future schema versioning.

---

# Decision

Cloud backups will use **Google Drive `appDataFolder`** with a **versioned JSON snapshot** of workout state.

The initial backup scope is:

- exercise templates,
- workout sessions,
- exercise entries,
- set entries.

The initial backup scope explicitly excludes:

- OpenAI API keys,
- transient UI state,
- any future secrets unless there is a separate product decision.

Restore semantics are:

- download the latest backup snapshot,
- validate its schema version,
- replace local workout tables atomically.

---

# Why Not Google Sheets

Google Sheets is a poor fit for durable application state in this app because:

- the data is hierarchical and relational, not tabular-first,
- restore would require multi-sheet coordination and custom reconciliation logic,
- schema evolution would be more brittle,
- backups would live as user-visible spreadsheet files rather than hidden app data,
- Sheets introduces quotas and request patterns that are better suited to document editing than snapshot storage.

Sheets can still be useful later for exports or reports, but not as the primary backup store.

---

# Consequences

Positive:

- one snapshot object is easy to upload, download, and version,
- backup files stay out of the user’s normal Drive file list,
- restore logic stays simple and testable,
- the local-first Room database remains the source of truth.

Tradeoffs:

- Drive auth still needs to be implemented in the UI layer,
- restore is replace-based rather than merge-based,
- future schema changes need explicit snapshot version handling.

---

# Implementation Notes

The current codebase now includes:

- a `BackupSnapshot` payload with `schemaVersion`,
- JSON encode/decode support,
- a Room-backed export/import store for workout state,
- manual Google sign-in plus Settings actions for `Back Up Now` and `Restore Backup`.

The current remaining step is end-to-end verification against a configured Android OAuth client and enabled Drive API.
