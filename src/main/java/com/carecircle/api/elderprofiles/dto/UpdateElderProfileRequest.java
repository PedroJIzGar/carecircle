package com.carecircle.api.elderprofiles.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * Request body for partially updating a basic elder profile.
 *
 * <p>This DTO intentionally contains only non-clinical profile fields. CareCircle
 * must not store diagnosis, treatment decisions or medical recommendations here.</p>
 *
 * @param fullName optional new full name.
 * @param preferredName optional preferred name. Blank clears it.
 * @param birthDate optional birth date. Future dates are rejected.
 * @param notes optional general family notes. Blank clears it.
 */
public record UpdateElderProfileRequest(
        @Size(max = 160)
        String fullName,

        @Size(max = 100)
        String preferredName,

        @PastOrPresent
        LocalDate birthDate,

        @Size(max = 1000)
        String notes
) {

    /**
     * Ensures the PATCH request actually contains something to update.
     *
     * @return true when at least one supported field was provided.
     */
    @AssertTrue(message = "At least one updatable field must be provided.")
    public boolean isAtLeastOneFieldProvided() {
        return fullName != null || preferredName != null || birthDate != null || notes != null;
    }

    /**
     * Ensures a provided full name is not blank.
     *
     * @return true when fullName is absent or contains text.
     */
    @AssertTrue(message = "fullName must not be blank.")
    public boolean isFullNameValidWhenProvided() {
        return fullName == null || StringUtils.hasText(fullName);
    }
}
