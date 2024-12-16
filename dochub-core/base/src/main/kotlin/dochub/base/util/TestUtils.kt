/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.base.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dochub.base.settings.AppSettings
import io.ktor.server.config.*
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import java.io.File
import kotlin.uuid.Uuid

/**
 * Common utilities for unit testing.
 */
public object TestUtils {

    /**
     * Loads the application settings for testing.
     */
    public fun loadSettings() {
        // Load the default configuration.
        val defaultConfig: Config = ConfigFactory.parseResources("application.conf")

        // Extract the current workingDir value and append the unique suffix
        val originalWorkingDir: String = defaultConfig.getString("runtime.workingDir")
        val updatedWorkingDir = "$originalWorkingDir-${Uuid.random()}"

        // Create an override configuration.
        val overrideConfig: Config = ConfigFactory.parseString(
            """
                runtime.workingDir = "$updatedWorkingDir"
            """
        )

        // Merge the override with the default.
        val mergedConfig: Config = overrideConfig.withFallback(defaultConfig).resolve()

        // Create a new ApplicationConfig.
        val applicationConfig = HoconApplicationConfig(config = mergedConfig)

        AppSettings.load(applicationConfig = applicationConfig)
    }

    /**
     * Sets up Koin for testing.
     *
     * @param modules The modules to load.
     */
    public fun setupKoin(modules: List<Module> = emptyList()) {
        startKoin {
            modules(modules)
        }
    }

    /**
     * Tears down the testing environment.
     */
    public fun tearDown() {
        stopKoin()

        val tempRuntime = File(AppSettings.runtime.workingDir)
        if (tempRuntime.exists()) {
            tempRuntime.deleteRecursively()
        }
    }
}
