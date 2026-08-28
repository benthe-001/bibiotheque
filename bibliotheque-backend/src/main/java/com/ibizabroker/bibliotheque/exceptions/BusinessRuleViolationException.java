package com.ibizabroker.bibliotheque.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(value = HttpStatus.CONFLICT)
public class BusinessRuleViolationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String regle;

    public BusinessRuleViolationException(String regle, String message) {
        super(message);
        this.regle = regle;
    }
}