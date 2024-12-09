/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.server

import io.ktor.server.application.*
import io.ktor.server.netty.*
import kdochub.access.plugins.configureBasicAuthentication
import kdochub.access.plugins.configureJwtAuthentication
import kdochub.access.plugins.configureRbac
import kdochub.access.plugins.configureSessions
import kdochub.core.plugins.*
import kdochub.core.settings.AppSettings
import kdochub.database.plugins.configureDatabase
import kdochub.server.plugins.configureKoin
import kdochub.server.plugins.configureRoutes
import kdochub.server.util.ApplicationsUtils

/**
 * Application main entry point.
 * Launches the Ktor server using Netty as the application engine.
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
internal fun Application.kdochubModule() {

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
