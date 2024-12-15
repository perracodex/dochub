/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdochub.base.plugins

import io.github.perracodex.kopapi.plugin.Kopapi
import io.github.perracodex.kopapi.type.*
import io.ktor.server.application.*
import kdochub.base.settings.AppSettings

/**
 * Configures OpenAPI, Swagger-UI and Redoc.
 *
 * #### References
 * - [Kopapi Documentation](https://github.com/perracodex/kopapi)
 */
public fun Application.configureApiSchema() {

    if (!AppSettings.apiSchema.environments.contains(AppSettings.runtime.environment)) {
        return
    }

    install(plugin = Kopapi) {
        enabled = true
        onDemand = false
        logPluginRoutes = true

        apiDocs {
            openApiUrl = AppSettings.apiSchema.openApiEndpoint
            openApiFormat = OpenApiFormat.YAML
            redocUrl = AppSettings.apiSchema.redocEndpoint

            swagger {
                url = AppSettings.apiSchema.swaggerEndpoint
                persistAuthorization = true
                withCredentials = false
                docExpansion = SwaggerDocExpansion.NONE
                displayRequestDuration = true
                displayOperationId = true
                operationsSorter = SwaggerOperationsSorter.UNSORTED
                uiTheme = SwaggerUiTheme.DARK
                syntaxTheme = SwaggerSyntaxTheme.NORD
                includeErrors = true
            }
        }

        info {
            title = "KDOCHUB API"
            version = "1.0.0"
            description = "KDOCHUB API Documentation"
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        servers {
            add(urlString = "{protocol}://{host}:{port}") {
                description = "KDOCHUB API Server"
                variable(name = "protocol", defaultValue = "http") {
                    choices = setOf("http", "https")
                }
                variable(name = "host", defaultValue = "localhost") {
                    choices = setOf(AppSettings.deployment.host, "localhost")
                }
                variable(name = "port", defaultValue = "8080") {
                    choices = setOf(
                        AppSettings.deployment.port.toString(),
                        AppSettings.deployment.sslPort.toString(),
                    )
                }
            }
        }
    }
}
