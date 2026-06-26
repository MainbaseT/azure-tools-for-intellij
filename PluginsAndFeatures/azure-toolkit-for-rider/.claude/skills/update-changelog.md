# Update CHANGELOG.md with the latest GitHub release

Update the `PluginsAndFeatures/azure-toolkit-for-rider/CHANGELOG.md` file by adding a new release section based on the latest GitHub release.

## Steps

### 1. Fetch the latest release from GitHub

Use WebFetch to retrieve the latest release from the GitHub API:

```
https://api.github.com/repos/JetBrains/azure-tools-for-intellij/releases/latest
```

From the JSON response, extract:
- `tag_name` — the Git tag (e.g., `4.7.4` or `v4.5.3`). This is **NEW_TAG** — use it as-is in comparison URLs.
- **NEW_VERSION** — the version for display in section headers. If `tag_name` starts with `v`, strip the `v` prefix; otherwise use `tag_name` as-is.
- `published_at` — format as `YYYY-MM-DD`. This is **RELEASE_DATE**.

### 2. Read the current CHANGELOG.md

Read the file at `PluginsAndFeatures/azure-toolkit-for-rider/CHANGELOG.md`.

### 3. Check for duplicates

Search for `## [NEW_VERSION]` in the file. If it already exists, stop and inform the user that this version is already in the changelog.

### 4. Determine the previous tag

Find the `[Unreleased]` comparison link at the bottom of the file:

```
[Unreleased]: https://github.com/JetBrains/azure-tools-for-intellij/compare/PREVIOUS_TAG...HEAD
```

Extract **PREVIOUS_TAG** from this URL (the part between `compare/` and `...HEAD`). Keep it exactly as-is (with or without `v` prefix).

### 5. Insert the new version section header

Find the line `## [Unreleased]` in the file. Immediately after it, insert a blank line followed by the new section header:

```markdown
## [Unreleased]

## [NEW_VERSION] - RELEASE_DATE
```

Do NOT add any subsections (### Added, ### Changed, etc.) — only the header line.

### 6. Update the `[Unreleased]` comparison link

At the bottom of the file, change:

```
[Unreleased]: https://github.com/JetBrains/azure-tools-for-intellij/compare/PREVIOUS_TAG...HEAD
```

to:

```
[Unreleased]: https://github.com/JetBrains/azure-tools-for-intellij/compare/NEW_TAG...HEAD
```

### 7. Add the new version comparison link

Immediately after the `[Unreleased]` link, insert:

```
[NEW_VERSION]: https://github.com/JetBrains/azure-tools-for-intellij/compare/PREVIOUS_TAG...NEW_TAG
```

### 8. Verify

Read the modified file and confirm:
- `## [Unreleased]` is still at the top
- `## [NEW_VERSION] - RELEASE_DATE` appears right after it
- The `[Unreleased]` link points to `compare/NEW_TAG...HEAD`
- The `[NEW_VERSION]` link points to `compare/PREVIOUS_TAG...NEW_TAG`
- No other parts of the file were changed
