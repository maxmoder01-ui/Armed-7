# Sample Armed 7 module

A `.kmod` package is a ZIP archive renamed to `.kmod` and contains a root-level
`manifest.json`.

Alpha 0.1 validates and stores package data only.
It does not load executable code or interact with the Among Us game process.

## Required fields

- `id`: lowercase identifier.
- `name`: module name.
- `version`: module version.
- `apiVersion`: currently `1`.
