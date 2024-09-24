/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.core.settings.config.sections.security.sections

import kdoc.core.settings.config.parser.IConfigSection
import kotlinx.serialization.Serializable

/**
 * RBAC settings.
 *
 * @property isEnabled Flag to enable/disable RBAC authentication.
 */
@Serializable
public data class RbacSettings(
    val isEnabled: Boolean
) : IConfigSection
