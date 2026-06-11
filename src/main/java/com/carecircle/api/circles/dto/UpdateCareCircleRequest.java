package com.carecircle.api.circles.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

/**
 * Request body for partially updating care circle basics.
 *
 * <p>Only circle-level fields are updated here. Elder profile updates should
 * have their own endpoint because they belong to a different domain concept.</p>
 *
 * @param name optional new circle name.
 * @param description optional new circle description. Blank clears it.
 */
public record UpdateCareCircleRequest(
        @Size(max = 160)
        String name,

        @Size(max = 500)
        String description
) {

    /**
     * Ensures the PATCH request actually contains something to update.
     *
     * @return true when at least one supported field was provided.
     */
    @AssertTrue(message = "At least one updatable field must be provided.")
    public boolean isAtLeastOneFieldProvided() {
        return name != null || description != null;
    }

    /**
     * Ensures a provided name is not blank.
     *
     * @return true when name is absent or contains text.
     */
    @AssertTrue(message = "name must not be blank.")
    public boolean isNameValidWhenProvided() {
        return name == null || StringUtils.hasText(name);
    }
}
