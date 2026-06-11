package com.carecircle.api.circles.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating the first version of a care circle aggregate.
 *
 * <p>The authenticated user is not accepted from the request body. It is always
 * derived from the validated Supabase Bearer token.</p>
 *
 * @param circle care circle fields.
 * @param elderProfile basic non-clinical elder profile fields.
 */
public record CreateCareCircleRequest(
        @Valid
        @NotNull
        CircleInput circle,

        @Valid
        @NotNull
        ElderProfileInput elderProfile
) {

    /**
     * Input fields for the care circle itself.
     *
     * @param name family-visible circle name.
     * @param description optional short description.
     */
    public record CircleInput(
            @NotBlank
            @Size(max = 160)
            String name,

            @Size(max = 500)
            String description
    ) {
    }

    /**
     * Input fields for the elder profile created with the circle.
     *
     * @param fullName elder person's full name.
     * @param preferredName optional preferred name used by the family.
     * @param birthDate optional birth date.
     * @param notes optional general family notes.
     */
    public record ElderProfileInput(
            @NotBlank
            @Size(max = 160)
            String fullName,

            @Size(max = 100)
            String preferredName,

            @PastOrPresent
            LocalDate birthDate,

            @Size(max = 1000)
            String notes
    ) {
    }
}
