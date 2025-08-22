# Valtimo Plugin Starter

The **Valtimo Plugin Starter** provides an empty, ready-to-use Valtimo instance for developing **Valtimo/GZAC plugins**.

## Getting Started

1. Rename your project in `./settings.gradle.kts`:

   ```kotlin
   rootProject.name = "New Plugin Name"
   ```

2. Install frontend dependencies:

   ```bash
   cd ./frontend
   npm install
   ```

3. Start the frontend:

   ```bash
   ng serve
   ```

4. Start the backend:

   ```bash
   ./gradlew :app:backend:bootRun
   ```

This will also spin up a **Docker Compose** environment with the required containers.

## Setting Up a Plugin

A plugin can be set up manually or by using the `EZplugin` Gradle task.

To run the `EZplugin` task:

The only thing you need to do is register the plugin in the ./frontend/src/app/app.module.ts file.

```bash
./gradlew EZplugin --name "your plugin name"
```

This will automatically build a basic working plugin and auto-register the module in both the frontend and backend. After building, it should be visible once you restart the frontend and backend.

### Keycloak users

The example application has a few test users that are preconfigured.

| Name | Role | Username | Password |
|---|---|---|---|
| James Vance | ROLE_USER | user | user |
| Asha Miller | ROLE_ADMIN | admin | admin |
| Morgan Finch | ROLE_DEVELOPER | developer | developer |