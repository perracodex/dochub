/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.utils

import io.ktor.server.config.*
import kdoc.base.database.schema.document.DocumentTable
import kdoc.base.database.service.DatabaseService
import kdoc.base.settings.AppSettings
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import java.io.File

/**
 * Common utilities for unit testing.
 */
object TestUtils {

    /**
     * Loads the application settings for testing.
     */
    fun loadSettings() {
        val testConfig = ApplicationConfig(configPath = "application.conf")

        runBlocking {
            AppSettings.load(applicationConfig = testConfig)
        }
    }

    /**
     * Sets up the database for testing.
     */
    fun setupDatabase() {
        DatabaseService.init(settings = AppSettings.database) {
            addTable(table = DocumentTable)
        }
    }

    /**
     * Sets up Koin for testing.
     *
     * @param modules The modules to load.
     */
    fun setupKoin(modules: List<Module> = emptyList()) {
        startKoin {
            modules(modules)
        }
    }

    /**
     * Tears down the testing environment.
     */
    fun tearDown() {
        stopKoin()

        DatabaseService.close()

        val tempRuntime = File(AppSettings.runtime.workingDir)
        if (tempRuntime.exists()) {
            tempRuntime.deleteRecursively()
        }
    }
}
