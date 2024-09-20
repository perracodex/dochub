/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.base.persistence.pagination

import org.jetbrains.exposed.sql.ResultRow

/**
 * Interface for mapping database query [ResultRow] data to entity instances.
 */
public interface IEntityMapper<T> {
    /**
     * Maps a database [ResultRow] to an entity instance.
     *
     * @param row The database [ResultRow] to map.
     * @return The entity instance mapped from the [ResultRow].
     */
    public fun from(row: ResultRow): T
}
