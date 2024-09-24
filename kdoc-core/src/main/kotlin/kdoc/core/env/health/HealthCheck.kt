/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.env.health

import io.ktor.server.application.*
import kdoc.core.database.service.DatabaseService
import kdoc.core.env.health.annotation.HealthCheckAPI
import kdoc.core.env.health.checks.*
import kdoc.core.env.health.utils.collectRoutes
import kotlinx.serialization.Serializable

/**
 * Data class representing the overall health check for the system.
 *
 * @property application The [ApplicationCheck] health check.
 * @property deployment The [DeploymentCheck] health check.
 * @property health List of errors found during any of the health checks.
 * @property runtime The [RuntimeCheck] health check.
 * @property security The [SecurityCheck] health check.
 * @property snowflake The [SnowflakeCheck] health check.
 * @property database The [DatabaseCheck] health check.
 * @property endpoints The list of endpoints detected by the application.
 */
@OptIn(HealthCheckAPI::class)
@Serializable
@ConsistentCopyVisibility
public data class HealthCheck internal constructor(
    val health: MutableList<String>,
    val application: ApplicationCheck,
    val deployment: DeploymentCheck,
    val runtime: RuntimeCheck,
    val security: SecurityCheck,
    val snowflake: SnowflakeCheck,
    val database: DatabaseCheck,
    val endpoints: List<String>
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
        fun create(call: ApplicationCall): HealthCheck {
            return HealthCheck(
                health = mutableListOf(),
                application = ApplicationCheck(),
                deployment = DeploymentCheck(call = call),
                runtime = RuntimeCheck(call = call),
                security = SecurityCheck(),
                snowflake = SnowflakeCheck(),
                database = DatabaseService.getHealthCheck(),
                endpoints = call.application.collectRoutes(),
            )
        }
    }
}