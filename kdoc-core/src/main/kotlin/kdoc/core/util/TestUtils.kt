/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.util

import io.ktor.server.config.*
import kdoc.core.database.schema.document.DocumentTable
import kdoc.core.database.service.DatabaseService
import kdoc.core.settings.AppSettings
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import java.io.File

/**
 * Common utilities for unit testing.
 */
public object TestUtils {

    /**
     * Loads the application settings for testing.
     */
    public fun loadSettings() {
        val testConfig = ApplicationConfig(configPath = "application.conf")

        AppSettings.load(applicationConfig = testConfig)
    }

    /**
     * Sets up the database for testing.
     */
    public fun setupDatabase() {
        DatabaseService.init(settings = AppSettings.database) {
            addTable(table = DocumentTable)
        }
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

        DatabaseService.close()

        val tempRuntime = File(AppSettings.runtime.workingDir)
        if (tempRuntime.exists()) {
            tempRuntime.deleteRecursively()
        }
    }
}
