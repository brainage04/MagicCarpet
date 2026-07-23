# Modrinth publishing

This repository includes optional Modrinth publishing in `.github/workflows/release.yml`. The job is skipped unless the repository has a `MODRINTH_TOKEN` secret.

## Token

Create a Modrinth personal access token with these scopes and save it as the `MODRINTH_TOKEN` repository secret:

- `PROJECT_CREATE`
- `PROJECT_READ`
- `PROJECT_WRITE`
- `VERSION_READ`
- `VERSION_CREATE`

## Project metadata

The shared release workflow reads:

- `src/main/resources/fabric.mod.json` for the slug, title, description fallback, contact links, licence, dependencies, and side support;
- `README.md` for the long project description;
- the GitHub repository description for the project summary;
- `.modrinth/project.json` for Modrinth-specific category overrides;
- `gradle.properties` for the mod and Minecraft versions.

The project is created as a draft if it does not already exist. Existing project metadata is synchronized on every release. Fabric dependencies declared in `fabric.mod.json` are translated to Modrinth dependencies; Minecraft, Java, and Fabric Loader constraints are omitted from that dependency list.

MagicCarpet uses `magic` and `transportation` as its primary categories, with `equipment`, `game-mechanics`, `technology`, and `utility` as additional searchable categories.

## Releases

The Modrinth changelog uses the same annotated tag text as the GitHub Release. See [RELEASE.md](RELEASE.md).

The workflow uploads the main JAR from `build/libs`, excluding development and sources JARs. If the current mod version already exists on Modrinth, that upload is skipped.
