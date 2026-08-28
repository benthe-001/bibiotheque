package com.ibizabroker.bibliotheque.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String regle;

    public InvalidRequestException(String message) {
        super(message);
        this.regle = null;
    }
}