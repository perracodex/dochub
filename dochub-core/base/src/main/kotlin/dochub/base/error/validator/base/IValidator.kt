/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package dochub.base.error.validator.base

import dochub.base.error.validator.EmailValidator
import dochub.base.error.validator.PhoneValidator

/**
 * Interface representing a generic value validator.
 *
 * Classes implementing this interface are responsible for validating
 * specific types of input values, such as email addresses, phone numbers,
 * or any type of data. The validation logic should ensure that
 * the input value adheres to the required format or business rules.
 *
 * @see [EmailValidator]
 * @see [PhoneValidator]
 * @see [ValidationException]
 */
public interface IValidator<T> {
    /**
     * Validates the provided [value].
     *
     * @param value The target value to be validated.
     * @return A [Result] object containing original [value] if the validation is successful,
     * or a failure with a relevant [ValidationException] if the validation fails.
     */
    public fun verify(value: T): Result<T>
}
