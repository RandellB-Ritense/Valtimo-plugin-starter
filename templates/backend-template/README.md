# Example Plugin Template (Backend)

This module provides a minimal boilerplate for creating a new Valtimo backend plugin, modeled after the
kvk-handelsregister structure.

How to use:

- Copy this directory and rename it to your plugin name.
- Update package names under `src/main/kotlin`.
- Update `plugin.properties` (pluginArtifactId), `build.gradle.kts` (projectName/publishing), and the `@Plugin`
  annotation key/title/description in `ExamplePlugin.kt`.
- Create your own services/clients as needed.
- Wire the module into the build by adding it to `settings.gradle.kts` and reference it from
  `backend/app/build.gradle.kts` if you want the app to include it.

Notes:

- The plugin exposes a sample action `sample-action` that logs a message.
- Keep the plugin key in sync with the frontend plugin specification.
