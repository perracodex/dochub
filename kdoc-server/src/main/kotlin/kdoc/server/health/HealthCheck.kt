/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.server.health

import io.ktor.server.application.*
import kdoc.core.env.HealthCheckApi
import kdoc.core.settings.AppSettings
import kdoc.core.util.RouteInfo
import kdoc.core.util.collectRoutes
import kdoc.database.service.DatabaseHealth
import kdoc.server.health.checks.*
import kotlinx.serialization.Serializable

/**
 * Data class representing the overall health check for the system.
 *
 * @property health List of errors found during any of the health checks.
 * @property application The [kdoc.server.health.checks.ApplicationHealth] check.
 * @property deployment The [kdoc.server.health.checks.DeploymentHealth] check.
 * @property runtime The [kdoc.server.health.checks.RuntimeHealth] check.
 * @property security The [kdoc.server.health.checks.SecurityHealth] check.
 * @property snowflake The [kdoc.server.health.checks.SnowflakeHealth] check.
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
