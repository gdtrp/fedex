package com.fedex.assessment.model.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE_USE;

@Documented
@Constraint(validatedBy = IsoCountryCodeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsoCountryCode {

    String message() default "Invalid country code";

    Class<? extends Payload>[] payload() default {};

    Class<?>[] groups() default {};
}
