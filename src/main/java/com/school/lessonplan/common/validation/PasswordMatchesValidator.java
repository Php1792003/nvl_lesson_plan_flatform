package com.school.lessonplan.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext ctx) {
        if (!(obj instanceof PasswordConfirmable form)) {
            return true;
        }
        if (form.getPassword() == null || form.getConfirmPassword() == null) {
            return true; // bắt buộc đã do @NotBlank báo
        }
        boolean ok = form.getPassword().equals(form.getConfirmPassword());
        if (!ok) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("{PasswordMatches}")
               .addPropertyNode("confirmPassword")
               .addConstraintViolation();
        }
        return ok;
    }
}
