---
name: sync-upstream-release
description: Merge a selected azure-tools-for-java release branch into the Azure Toolkit fork, update its upstream-release metadata and changelog, then open a pull request.
user_invocable: true
---

# Sync an Upstream Release

Use this skill when the user asks to bring a release branch from `microsoft/azure-tools-for-java` into the JetBrains fork. Work from the repository root returned by `git rev-parse --show-toplevel`, not the Rider plugin directory. In this repository, the workflow is at the root (`.github/workflows/build.yml`) and the Rider changelog is at `PluginsAndFeatures/azure-toolkit-for-rider/CHANGELOG.md`.

## Required input and safety checks

1. Ask the user for the exact upstream release branch before changing repository state. Require the form `release-vX.Y.Z`, for example `release-v3.97.1`; do not infer, substitute, or select a release on the user's behalf.

2. Check that the worktree is clean with `git status --porcelain`. If it is not clean, stop and report the paths. Do not stash, discard, overwrite, or include another person's changes.

3. Ensure the `upstream` remote exists. If it is absent, add exactly:

   ```powershell
   git remote add upstream git@github.com:microsoft/azure-tools-for-java.git
   ```

   If `upstream` exists but has a different URL, stop and ask the user to resolve that configuration.

4. Fetch the upstream refs:

   ```powershell
   git fetch upstream
   ```

5. Verify that `upstream/<selected-release>` exists. If it does not, stop and report that the requested release is unavailable. Also stop if either the local branch `upstream/<selected-release>` or the identically named branch on `origin` already exists; do not overwrite an existing integration branch.

## Create the integration branch

Create `upstream/<selected-release>` directly from `upstream/<selected-release>`, then merge the local `develop` branch into it:

```powershell
git switch --create "upstream/<selected-release>" "upstream/<selected-release>"
git merge develop --no-edit
```

If the merge has *any* conflict, run `git merge --abort` immediately. Do not attempt to inspect, edit, stage, resolve, or commit conflicted files. Do not update metadata, push, or create a pull request. Leave the newly created branch available at its upstream base and hand control to the user with the conflict report.

If the merge succeeds, retain its result and continue.

## Update fork metadata

Update only the following files after a successful merge:

1. In `.github/workflows/build.yml`, set the single `UPSTREAM_RELEASE_VERSION` value to the selected release branch exactly, for example:

   ```yaml
   UPSTREAM_RELEASE_VERSION: release-v3.97.1
   ```

2. In `PluginsAndFeatures/azure-toolkit-for-rider/CHANGELOG.md`, add the selected upstream release under the `[Unreleased]` section's `Changed` subsection, creating that subsection if absent:

   ```markdown
   - Sync plugin with upstream `release-v3.97.1`.
   ```

Do not change unrelated workflow settings or changelog entries. Run `git diff --check` after the edits.

## Publish and open the pull request

Commit the two metadata changes in a separate commit with a clear message, such as `chore: sync upstream release-v3.97.1`. Push the integration branch to `origin`, then create a GitHub pull request in `JetBrains/azure-tools-for-intellij` from that branch into `develop`. Use a clear title such as `Sync upstream release-v3.97.1` and mention the selected upstream branch in the PR body.

Report the upstream branch, integration branch, merge result, files changed, commit SHA, and PR URL. If authentication or GitHub CLI access prevents pushing or creating the PR, stop and report the exact failed command and error; do not retry with a different remote or publish destination.
