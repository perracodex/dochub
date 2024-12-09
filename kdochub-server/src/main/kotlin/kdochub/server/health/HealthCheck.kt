/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.server.health

import io.ktor.server.application.*
import kdochub.core.env.HealthCheckApi
import kdochub.core.settings.AppSettings
import kdochub.core.util.RouteInfo
import kdochub.core.util.collectRoutes
import kdochub.database.service.DatabaseHealth
import kdochub.server.health.checks.*
import kotlinx.serialization.Serializable

/**
 * Data class representing the overall health check for the system.
 *
 * @property health List of errors found during any of the health checks.
 * @property application The [ApplicationHealth] check.
 * @property deployment The [DeploymentHealth] check.
 * @property runtime The [RuntimeHealth] check.
 * @property security The [SecurityHealth] check.
 * @property snowflake The [SnowflakeHealth] check.
 * @property database The [DatabaseHealth] check.
 * @property endpoints The list of endpoints registered by the application.
 */
@OptIn(HealthCheckApi::class)
@Serializable
public data class HealthCheck private constructor(
    val health: MutableList<String>,
    val application: ApplicationHealth,
    val deployment: DeploymentHealth,
    val runtime: RuntimeHealth,
    val security: SecurityHealth,
    val snowflake: SnowflakeHealth,
    val database: DatabaseHealth,
    val endpoints: List<RouteInfo>
) {
    init {
        health.addAll(application.errors)
        health.addAll(deployment.errors)
        health.addAll(runtime.errors)
        health.addAll(security.errors)
        health.addAll(snowflake.errors)
        health.addAll(database.errors)

        if (endpoints.isEmpty()) {
            health.add("No Endpoints Detected.")
        }
        if (health.isEmpty()) {
            health.add("No Errors Detected.")
        }
    }

    internal companion object {
        /**
         * Creates a new [HealthCheck] instance.
         */
        suspend fun create(call: ApplicationCall): HealthCheck {
            return HealthCheck(
                health = mutableListOf(),
                application = ApplicationHealth(),
                deployment = DeploymentHealth.create(call = call),
                runtime = RuntimeHealth(call = call, settings = AppSettings.runtime),
                security = SecurityHealth(),
                snowflake = SnowflakeHealth(),
                database = DatabaseHealth.create(settings = AppSettings.database),
                endpoints = call.application.collectRoutes(),
            )
        }
    }
}
