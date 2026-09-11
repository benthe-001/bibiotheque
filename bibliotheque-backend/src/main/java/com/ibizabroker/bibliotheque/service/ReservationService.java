package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.dto.ReservationResponseDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BusinessRuleViolationException;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import com.ibizabroker.bibliotheque.exceptions.InvalidRequestException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.security.AppRoles;
import com.ibizabroker.bibliotheque.security.CurrentUserService;
import com.ibizabroker.bibliotheque.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Logique métier du module réservation.
 * <p>
 * Règles métier : RG-01 à RG-06.<br>
 * Règles de sécurité : RS-01 et RS-02 côté Spring Security (config + @PreAuthorize),
 * RS-03, RS-04 et RS-05 appliquées ici — l'identité vient toujours du token
 * (via {@link CurrentUserService}), jamais du corps de la requête.
 */
@Service
public class ReservationService {

    private static final List<StatutReservation> STATUTS_ACTIFS = Arrays.asList(
            StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE);

    private static final List<StatutReservation> STATUTS_TERMINAUX = Arrays.asList(
            StatutReservation.ANNULEE, StatutReservation.EXPIREE, StatutReservation.HONOREE);

    /** RG-03 : nombre maximal de réservations actives simultanées par adhérent. */
    private static final int MAX_RESERVATIONS_ACTIVES = 3;

    private final ReservationRepository reservationRepository;
    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;
    private final CurrentUserService currentUserService;

    /** Injection par constructeur : testable avec des mocks, sans base. */
    public ReservationService(ReservationRepository reservationRepository,
                              BooksRepository booksRepository,
                              UsersRepository usersRepository,
                              CurrentUserService currentUserService) {
        this.reservationRepository = reservationRepository;
        this.booksRepository = booksRepository;
        this.usersRepository = usersRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Crée une réservation.
     * RG-01 : On ne peut réserver qu'un livre indisponible.
     * RG-02 : Un adhérent ne peut avoir qu'une seule réservation active sur un même livre.
     * RG-03 : Un adhérent ne peut pas dépasser 3 réservations actives simultanées.
     * RG-04 : dateExpiration = dateReservation + 7 jours.
     * RS-04 : pour un ADHERENT, l'identité vient du token — l'adherentId du corps est ignoré.
     */
    public ReservationResponseDTO creerReservation(ReservationRequestDTO request) {
        // Validation des champs obligatoires : on liste TOUS les champs manquants
        List<String> manquants = new ArrayList<>();
        if (request.getLivreId() == null) {
            manquants.add("livreId");
        }
        // RS-04 : pour un ADHERENT, l'adherentId du corps est ignoré —
        // l'identité au nom de qui on réserve vient du token.
        Integer adherentId = resoudreAdherentId(request, manquants);
        if (!manquants.isEmpty()) {
            throw new InvalidRequestException("Champ(s) obligatoire(s) manquant(s) : " + String.join(", ", manquants) + ".");
        }

        // Vérification de l'existence du livre
        Books livre = booksRepository.findById(request.getLivreId())
                .orElseThrow(() -> new NotFoundException("Livre avec l'id " + request.getLivreId() + " introuvable."));

        // Vérification de l'existence de l'adhérent
        Users adherent = usersRepository.findById(adherentId)
                .orElseThrow(() -> new NotFoundException("Adherent avec l'id " + adherentId + " introuvable."));

        // RG-01 : On ne peut réserver qu'un livre indisponible
        if (livre.getNoOfCopies() > 0) {
            throw new BusinessRuleViolationException("RG-01",
                    "RG-01 : Le livre \"" + livre.getBookName() + "\" est disponible, il ne peut pas être réservé.");
        }

        // RG-02 : Un adhérent ne peut avoir qu'une seule réservation active sur un même livre
        List<Reservation> reservationsActivesSurLivre = reservationRepository
                .findByLivre_BookIdAndStatutIn(livre.getBookId(), STATUTS_ACTIFS);
        boolean dejaReserve = reservationsActivesSurLivre.stream()
                .anyMatch(r -> r.getAdherent().getUserId().equals(adherent.getUserId()));
        if (dejaReserve) {
            throw new BusinessRuleViolationException("RG-02",
                    "RG-02 : L'adherent \"" + adherent.getName() + "\" a déjà une réservation active sur le livre \"" + livre.getBookName() + "\".");
        }

        // RG-03 : Un adhérent ne peut pas dépasser 3 réservations actives simultanées
        List<Reservation> reservationsActivesAdherent = reservationRepository
                .findByAdherent_UserIdAndStatutIn(adherent.getUserId(), STATUTS_ACTIFS);
        if (reservationsActivesAdherent.size() >= MAX_RESERVATIONS_ACTIVES) {
            throw new BusinessRuleViolationException("RG-03",
                    "RG-03 : L'adherent \"" + adherent.getName() + "\" a déjà " + reservationsActivesAdherent.size()
                            + " réservations actives, le maximum autorisé est de " + MAX_RESERVATIONS_ACTIVES + ".");
        }

        // Création de la réservation
        Reservation reservation = new Reservation();
        reservation.setLivre(livre);
        reservation.setAdherent(adherent);

        // RG-04 : dateReservation = maintenant, dateExpiration = dateReservation + 7 jours
        Date dateReservation = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateReservation);
        calendar.add(Calendar.DATE, 7);
        Date dateExpiration = calendar.getTime();

        reservation.setDateReservation(dateReservation);
        reservation.setDateExpiration(dateExpiration);
        reservation.setStatut(StatutReservation.EN_ATTENTE);

        Reservation saved = reservationRepository.save(reservation);
        return toResponseDTO(saved);
    }

    /**
     * RS-04 : identité de l'adhérent au nom duquel on réserve.
     * <ul>
     *     <li>BIBLIOTHECAIRE : peut créer pour n'importe quel adhérent (adherentId du corps, requis).</li>
     *     <li>ADHERENT : toujours lui-même — l'identité vient du token, le corps est ignoré.</li>
     * </ul>
     */
    private Integer resoudreAdherentId(ReservationRequestDTO request, List<String> manquants) {
        if (currentUserService.isBibliothecaire()) {
            if (request.getAdherentId() == null) {
                manquants.add("adherentId");
            }
            return request.getAdherentId();
        }
        Integer currentUserId = currentUserService.getCurrentUserId();
        if (currentUserId == null) {
            throw new ForbiddenException("RS-04",
                    "L'identité de l'utilisateur n'a pas pu être déterminée à partir du token.");
        }
        return currentUserId;
    }

    /**
     * Liste les réservations, filtrable par statut et par adhérent.
     * RS-05 : un ADHERENT ne reçoit que ses propres réservations, quel que soit le filtre demandé.
     */
    public List<ReservationResponseDTO> listerReservations(StatutReservation statut, Integer adherentId) {
        adherentId = restreindreFiltreAdherent(adherentId);

        List<Reservation> reservations;

        if (statut != null && adherentId != null) {
            reservations = reservationRepository.findByAdherent_UserId(adherentId).stream()
                    .filter(r -> r.getStatut() == statut)
                    .collect(Collectors.toList());
        } else if (statut != null) {
            reservations = reservationRepository.findByStatut(statut);
        } else if (adherentId != null) {
            reservations = reservationRepository.findByAdherent_UserId(adherentId);
        } else {
            reservations = reservationRepository.findAll();
        }

        return reservations.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * RS-05 : force le filtre sur l'identité du token pour un ADHERENT.
     */
    private Integer restreindreFiltreAdherent(Integer adherentId) {
        if (currentUserService.isBibliothecaire()) {
            return adherentId; // BIBLIOTHECAIRE : filtre libre
        }
        if (currentUserService.isAdherent()) {
            return currentUserService.getCurrentUserId();
        }
        return adherentId;
    }

    /**
     * Consulte une réservation par son id.
     * RS-03 : un ADHERENT ne peut consulter que ses propres réservations.
     */
    public ReservationResponseDTO consulterReservation(Integer id) {
        Reservation reservation = chargerReservation(id);
        verifierAccesProprietaire(reservation);
        return toResponseDTO(reservation);
    }

    /**
     * Annule une réservation.
     * RS-03 : un ADHERENT ne peut annuler que ses propres réservations (vérifié avant les règles métier).
     * RG-05 : Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE.
     * RG-06 : Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état.
     */
    public ReservationResponseDTO annulerReservation(Integer id) {
        Reservation reservation = chargerReservation(id);
        verifierAccesProprietaire(reservation);

        // RG-06 : Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état
        if (STATUTS_TERMINAUX.contains(reservation.getStatut())) {
            throw new BusinessRuleViolationException("RG-05",
                    "RG-05 : Une reservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE. "
                            + "RG-06 : Le statut " + reservation.getStatut() + " est terminal, il ne peut plus changer d'état.");
        }

        // RG-05 : Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE
        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new BusinessRuleViolationException("RG-05",
                    "RG-05 : Une reservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE.");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        Reservation saved = reservationRepository.save(reservation);
        return toResponseDTO(saved);
    }

    /**
     * Supprime une réservation (réservée au BIBLIOTHECAIRE via @PreAuthorize — RS-02).
     */
    public void supprimerReservation(Integer id) {
        Reservation reservation = chargerReservation(id);
        reservationRepository.delete(reservation);
    }

    /**
     * Charge une réservation ou lève une 404.
     */
    private Reservation chargerReservation(Integer id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation avec l'id " + id + " introuvable."));
    }

    /**
     * RS-03 : contrôle d'appartenance.
     * BIBLIOTHECAIRE : accès à tout. ADHERENT : 403 si la réservation n'est pas la sienne.
     */
    private void verifierAccesProprietaire(Reservation reservation) {
        UserPrincipal utilisateur = currentUserService.getCurrentUser();
        if (utilisateur == null) {
            throw new ForbiddenException("RS-03",
                    "L'identité de l'utilisateur n'a pas pu être déterminée à partir du token.");
        }
        if (utilisateur.hasRole(AppRoles.BIBLIOTHECAIRE)) {
            return;
        }
        if (utilisateur.hasRole(AppRoles.ADHERENT)
                && !utilisateur.getUserId().equals(reservation.getAdherent().getUserId())) {
            throw new ForbiddenException("RS-03",
                    "403 : La réservation n°" + reservation.getId() + " n'appartient pas à l'adhérent \""
                            + utilisateur.getUsername() + "\".");
        }
    }

    /**
     * Convertit une entité Reservation en DTO de sortie.
     */
    private ReservationResponseDTO toResponseDTO(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setLivreId(reservation.getLivre().getBookId());
        dto.setLivreTitre(reservation.getLivre().getBookName());
        dto.setAdherentId(reservation.getAdherent().getUserId());
        dto.setAdherentNom(reservation.getAdherent().getName());
        dto.setDateReservation(reservation.getDateReservation());
        dto.setDateExpiration(reservation.getDateExpiration());
        dto.setStatut(reservation.getStatut());
        return dto;
    }
}