package com.ibizabroker.bibliotheque.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponseDTO {
    private String regle;
    private String message;
}