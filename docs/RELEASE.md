# Create a new release

1. Update `mod_version` in `gradle.properties`.
2. Commit and push that change.
3. Create a matching annotated tag in the form `vX.Y.Z`; its annotation becomes the release notes.
4. Push the tag.

For a short release note:

```shell
git tag -a v1.0.3 -m "Summarise the release here"
git push origin v1.0.3
```

For longer notes, put them in a file and use `git tag -a v1.0.3 -F RELEASE_NOTES.md`.

The release workflow validates that the tag and `mod_version` match, builds the release JAR, and publishes a GitHub Release. If the tag has no annotation text, GitHub-generated notes are used as a fallback.

If `MODRINTH_TOKEN` is configured, the same workflow creates or updates the Modrinth project and publishes the release JAR. If both `CURSEFORGE_TOKEN` and `CURSEFORGE_PROJECT_ID` are configured, it also publishes to CurseForge. Missing third-party credentials skip only that destination; the GitHub Release still proceeds.
