/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package kdoc.access.rbac.repository.field

import kdoc.access.rbac.model.field.RbacFieldRuleDto
import kdoc.access.rbac.model.field.RbacFieldRuleRequest
import kdoc.access.rbac.model.scope.RbacScopeRuleRequest
import kotlin.uuid.Uuid

/**
 * Repository for [RbacFieldRuleDto] data.
 *
 * @see RbacFieldRuleRequest
 */
internal interface IRbacFieldRuleRepository {

    /**
     * Updates an existing scope rule with the given set of [RbacFieldRuleRequest] entries.
     *
     * All the existing field rules for the concrete scope rule will be replaced by the new ones.
     *
     * @param scopeRuleId The target [RbacScopeRuleRequest] being updated.
     * @param requestList The new set of [RbacFieldRuleRequest] entries to set.
     * @return The new number of rows.
     */
    fun replace(scopeRuleId: Uuid, requestList: List<RbacFieldRuleRequest>?): Int
}
