/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.server.util

import io.ktor.server.application.*
import kdoc.access.domain.actor.service.DefaultActorFactory
import kdoc.core.env.Tracer
import kdoc.core.security.snowflake.SnowflakeFactory
import kdoc.core.settings.AppSettings
import kdoc.core.util.NetworkUtils
import kotlinx.coroutines.launch

/**
 * Utility functions for the application server.
 */
internal object ApplicationsUtils {
    private val tracer: Tracer = Tracer<ApplicationsUtils>()

    /**
     * Perform any additional server configuration that is required for the application to run.
     *
     * @param application The Ktor application instance.
     */
    fun completeServerConfiguration(application: Application) {
        // Add a hook to refresh the Credentials and RBAC services when the application starts.
        application.monitor.subscribe(definition = ApplicationStarted) {
            application.launch {
                DefaultActorFactory.refresh()
            }
        }

        // Watch the server for readiness.
        application.monitor.subscribe(definition = ServerReady) {
            watchServer(application = application)
        }
    }

    /**
     * Watches the server for readiness and logs the server's endpoints to the console.
     */
    private fun watchServer(application: Application) {
        // Dumps the server's endpoints to the console for easy access and testing.
        // This does not include the actual API routes endpoints.
        NetworkUtils.logEndpoints(reason = "Healthcheck", endpoints = listOf("/admin/health"))
        NetworkUtils.logEndpoints(reason = "Snowflake", endpoints = listOf("admin/snowflake/${SnowflakeFactory.nextId()}"))
        NetworkUtils.logEndpoints(reason = "Micrometer Metrics", endpoints = listOf("admin/metrics"))

        if (AppSettings.security.rbac.isEnabled) {
            NetworkUtils.logEndpoints(reason = "RBAC", endpoints = listOf("rbac/login"))
        }

        // Log the server readiness.
        tracer.withSeverity("Development Mode Enabled: ${application.developmentMode}.")
        tracer.info("Server configured. Environment: ${AppSettings.runtime.environment}.")
    }
}
