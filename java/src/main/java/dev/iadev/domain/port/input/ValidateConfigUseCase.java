package dev.iadev.domain.port.input;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ValidationResult;

/**
 * Use case contract for validating a project configuration.
 *
 * <p>Accepts a {@link ProjectConfig} and returns a {@link ValidationResult} indicating whether the
 * configuration is valid and listing any validation errors found.
 *
 * <p>Validation rules include but are not limited to:
 *
 * <ul>
 *   <li>Required fields are present and non-empty
 *   <li>Language and framework are supported
 *   <li>Architecture style is recognized
 * </ul>
 *
 * @see ProjectConfig
 * @see ValidationResult
 */
public interface ValidateConfigUseCase {

    /**
     * Validates the given project configuration.
     *
     * @param config the project configuration to validate (must not be null)
     * @return the validation result with validity flag and errors
     */
    ValidationResult validate(ProjectConfig config);
}
