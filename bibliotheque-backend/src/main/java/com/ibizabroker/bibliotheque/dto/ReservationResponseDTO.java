package com.ibizabroker.bibliotheque.dto;

import com.ibizabroker.bibliotheque.entity.StatutReservation;
import lombok.Data;

import java.util.Date;

@Data
public class ReservationResponseDTO {
    private Integer id;
    private Integer livreId;
    private String livreNom;
    private Integer adherentId;
    private String adherentNom;
    private Date dateReservation;
    private Date dateExpiration;
    private StatutReservation statut;
}