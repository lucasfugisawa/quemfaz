This is a Kotlin Multiplatform project targeting Android, iOS, Web, Server.

* [/composeApp](./composeApp/src) is the shared UI and platform logic for all targets.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/androidApp](./androidApp/src) is the Android application module.
  It depends on `composeApp` and `shared` to provide the final Android app experience.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is the pure business logic and models shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin).

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :androidApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :androidApp:assembleDebug
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:

- for the Wasm target (faster, modern browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
      ```
- for the JS target (slower, supports older browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :composeApp:jsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
      ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Integration Tests (Server)

The project includes a robust integration test infrastructure using **Testcontainers (PostgreSQL)** and **Ktor `testApplication`**.

#### Prerequisites
- Docker must be running on your machine.
- For Windows users, ensure the Docker engine is exposed via named pipe (default for Docker Desktop).

#### Running tests
- Run all server tests:
  ```shell
  .\gradlew.bat :server:test
  ```
- Run integration tests specifically:
  ```shell
  .\gradlew.bat :server:test --tests "com.fugisawa.quemfaz.integration.*"
  ```

#### Infrastructure Features
- **CI/CD Compatible**: Configurations can be overridden via environment variables:
  - `TEST_JWT_SECRET`, `TEST_JWT_ISSUER`, `TEST_JWT_AUDIENCE`, `TEST_JWT_EXPIRES_IN`
  - `TEST_SMS_PROVIDER` (defaults to `FAKE`)
- **Automatic Cleanup**: Integration tests can define `tablesToClean` to have specific tables truncated before each test run, ensuring isolation.
- **Authenticated Client**: `createTestClient(token)` helper facilitates testing protected endpoints.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).