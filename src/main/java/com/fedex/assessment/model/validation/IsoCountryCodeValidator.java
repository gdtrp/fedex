package com.fedex.assessment.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class IsoCountryCodeValidator implements
        ConstraintValidator<IsoCountryCode, String> {

    private static final Set<String> ISO_COUNTRIES = new HashSet<>
            (Arrays.asList(Locale.getISOCountries()));

    @Override
    public boolean isValid(String code, ConstraintValidatorContext constraintValidatorContext) {
        return ISO_COUNTRIES.contains(code);
    }
}
