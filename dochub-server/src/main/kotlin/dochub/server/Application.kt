/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.server

import dochub.access.plugins.configureBasicAuthentication
import dochub.access.plugins.configureJwtAuthentication
import dochub.access.plugins.configureRbac
import dochub.access.plugins.configureSessions
import dochub.base.plugins.*
import dochub.base.settings.AppSettings
import dochub.database.plugins.configureDatabase
import dochub.server.plugins.configureKoin
import dochub.server.plugins.configureRoutes
import dochub.server.util.ApplicationsUtils
import io.ktor.server.application.*
import io.ktor.server.netty.*

/**
 * Application main entry point.
 * Launches the Ktor server using Netty as the application engine.
 *
 * #### Continuous Compilation
 * - Command: `./gradlew -t build -x test -i`
 * - [Auto-Reload](https://ktor.io/docs/server-auto-reload.html)
 *
 * #### References
 * - [Choosing an engine](https://ktor.io/docs/server-engines.html)
 * - [Configure an engine](https://ktor.io/docs/server-engines.html#configure-engine)
 * - [Application Monitoring With Server Events](https://ktor.io/docs/server-events.html)
 *
 * @param args Command line arguments passed to the application.
 */
public fun main(args: Array<String>) {
    EngineMain.main(args)
}

/**
 * Application configuration module, responsible for setting up the server with various plugins.
 *
 * #### Important
 * The order of execution is vital, as certain configurations depend on the initialization
 * of previous plugins. Incorrect ordering can lead to runtime errors or configuration issues.
 *
 * #### References
 * - [Modules](https://ktor.io/docs/server-modules.html)
 * - [Plugins](https://ktor.io/docs/server-plugins.html)
 */
internal fun Application.dochubModule() {

    AppSettings.load(applicationConfig = environment.config)

    configureKoin()

    configureDatabase()

    configureCors()

    configureSecureConnection()

    configureHeaders()

    configureHttp()

    configureCallLogging()

    configureSerialization()

    configureRateLimit()

    configureRbac()

    configureBasicAuthentication()

    configureJwtAuthentication()

    configureSessions()

    configureApiSchema()

    configureRoutes()

    configureMicroMeterMetrics()

    configureStatusPages()

    configureDoubleReceive()

    configureThymeleaf()

    ApplicationsUtils.completeServerConfiguration(application = this)
}
