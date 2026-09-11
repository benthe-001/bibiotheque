package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de réservation, tous authentifiés (RS-01 : 401 si token absent/invalide).
 * Matrice d'autorisation :
 *  - ADHERENT       : crée pour lui-même (identité du token), consulte/annule
 *                     uniquement SES réservations (RS-03, RS-04, RS-05) ;
 *  - BIBLIOTHECAIRE : accès complet, y compris DELETE (RS-02 : un ADHERENT reçoit 403).
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final String AUTORISER_LECTEURS = "hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')";

    @Autowired
    private ReservationService reservationService;

    @Operation(summary = "Créer une réservation", description = "Crée une réservation pour un livre indisponible. "
            + "Un ADHERENT est créé pour lui-même : son identité vient du token (RS-04), jamais du corps de la requête. "
            + "Un BIBLIOTHECAIRE peut créer au nom de n'importe quel adhérent (adherentId dans le corps).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide : livreId ou adherentId manquant"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "Droits insuffisants (RS-02, RS-04)"),
            @ApiResponse(responseCode = "404", description = "Livre ou adhérent introuvable"),
            @ApiResponse(responseCode = "409", description = "Règle de gestion violée (RG-01, RG-02, RG-03)")
    })
    @PreAuthorize(AUTORISER_LECTEURS)
    @PostMapping
    public ResponseEntity<ReservationResponseDTO> creerReservation(@RequestBody ReservationRequestDTO request) {
        ReservationResponseDTO response = reservationService.creerReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lister les réservations", description = "Un ADHERENT ne reçoit que ses propres réservations (RS-05). "
            + "Un BIBLIOTHECAIRE liste toutes les réservations, filtrables par statut et par adhérent.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des réservations récupérée avec succès"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré")
    })
    @PreAuthorize(AUTORISER_LECTEURS)
    @GetMapping
    public ResponseEntity<List<ReservationResponseDTO>> listerReservations(
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Integer adherentId) {
        List<ReservationResponseDTO> reservations = reservationService.listerReservations(statut, adherentId);
        return ResponseEntity.ok(reservations);
    }

    @Operation(summary = "Consulter une réservation", description = "Consulte une réservation par son identifiant. "
            + "Un ADHERENT ne peut consulter que ses propres réservations (RS-03).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "La réservation n'appartient pas à l'adhérent (RS-03)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    @PreAuthorize(AUTORISER_LECTEURS)
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> consulterReservation(@PathVariable Integer id) {
        ReservationResponseDTO response = reservationService.consulterReservation(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Annuler une réservation", description = "Annule une réservation si son statut est EN_ATTENTE ou DISPONIBLE. "
            + "Un ADHERENT ne peut annuler que ses propres réservations (RS-03).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation annulée avec succès"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "La réservation n'appartient pas à l'adhérent (RS-03)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "Règle de gestion violée (RG-05, RG-06)")
    })
    @PreAuthorize(AUTORISER_LECTEURS)
    @PatchMapping("/{id}/annuler")
    public ResponseEntity<ReservationResponseDTO> annulerReservation(@PathVariable Integer id) {
        ReservationResponseDTO response = reservationService.annulerReservation(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supprimer une réservation", description = "Supprime définitivement une réservation. "
            + "Réservé au BIBLIOTHECAIRE : un ADHERENT reçoit un 403 (RS-02).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Réservation supprimée avec succès"),
            @ApiResponse(responseCode = "401", description = "Token absent, invalide ou expiré"),
            @ApiResponse(responseCode = "403", description = "Réservé au BIBLIOTHECAIRE (RS-02)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerReservation(@PathVariable Integer id) {
        reservationService.supprimerReservation(id);
        return ResponseEntity.noContent().build();
    }
}