/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.database.test

import dochub.base.settings.AppSettings
import dochub.database.schema.document.DocumentAuditTable
import dochub.database.schema.document.DocumentTable
import dochub.database.service.DatabaseService

/**
 * Common utilities for unit testing.
 */
public object DatabaseTestUtils {

    /**
     * Sets up the database for testing.
     */
    public fun setupDatabase() {
        DatabaseService.init(
            settings = AppSettings.database,
            environment = AppSettings.runtime.environment
        ) {
            addTable(table = DocumentAuditTable)
            addTable(table = DocumentTable)
        }
    }

    /**
     * Closes the database connection.
     */
    public fun closeDatabase() {
        DatabaseService.close()
    }
}
