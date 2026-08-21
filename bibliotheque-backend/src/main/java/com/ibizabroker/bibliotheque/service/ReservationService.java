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
import com.ibizabroker.bibliotheque.exceptions.InvalidRequestException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private UsersRepository usersRepository;

    private static final List<StatutReservation> STATUTS_ACTIFS = Arrays.asList(
            StatutReservation.EN_ATTENTE, StatutReservation.DISPONIBLE);

    private static final List<StatutReservation> STATUTS_TERMINAUX = Arrays.asList(
            StatutReservation.ANNULEE, StatutReservation.EXPIREE, StatutReservation.HONOREE);

    /**
     * Crée une réservation.
     * RG-01 : On ne peut réserver qu'un livre indisponible.
     * RG-02 : Un adhérent ne peut avoir qu'une seule réservation active sur un même livre.
     * RG-03 : Un adhérent ne peut pas dépasser 3 réservations actives simultanées.
     * RG-04 : dateExpiration = dateReservation + 7 jours.
     */
    public ReservationResponseDTO creerReservation(ReservationRequestDTO request) {
        // Validation des champs obligatoires
        if (request.getLivreId() == null) {
            throw new InvalidRequestException("Le champ 'livreId' est obligatoire.");
        }
        if (request.getAdherentId() == null) {
            throw new InvalidRequestException("Le champ 'adherentId' est obligatoire.");
        }

        // Vérification de l'existence du livre
        Books livre = booksRepository.findById(request.getLivreId())
                .orElseThrow(() -> new NotFoundException("Livre avec l'id " + request.getLivreId() + " introuvable."));

        // Vérification de l'existence de l'adhérent
        Users adherent = usersRepository.findById(request.getAdherentId())
                .orElseThrow(() -> new NotFoundException("Adhérent avec l'id " + request.getAdherentId() + " introuvable."));

        // RG-01 : On ne peut réserver qu'un livre indisponible
        if (livre.getNoOfCopies() > 0) {
            throw new BusinessRuleViolationException(
                    "RG-01 : Le livre \"" + livre.getBookName() + "\" est disponible, il ne peut pas être réservé.");
        }

        // RG-02 : Un adhérent ne peut avoir qu'une seule réservation active sur un même livre
        List<Reservation> reservationsActivesSurLivre = reservationRepository
                .findByLivre_BookIdAndStatutIn(livre.getBookId(), STATUTS_ACTIFS);
        boolean dejaReserve = reservationsActivesSurLivre.stream()
                .anyMatch(r -> r.getAdherent().getUserId().equals(adherent.getUserId()));
        if (dejaReserve) {
            throw new BusinessRuleViolationException(
                    "RG-02 : L'adhérent \"" + adherent.getName() + "\" a déjà une réservation active sur le livre \"" + livre.getBookName() + "\".");
        }

        // RG-03 : Un adhérent ne peut pas dépasser 3 réservations actives simultanées
        List<Reservation> reservationsActivesAdherent = reservationRepository
                .findByAdherent_UserIdAndStatutIn(adherent.getUserId(), STATUTS_ACTIFS);
        if (reservationsActivesAdherent.size() >= 3) {
            throw new BusinessRuleViolationException(
                    "RG-03 : L'adhérent \"" + adherent.getName() + "\" a déjà atteint la limite de 3 réservations actives.");
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
     * Liste les réservations, filtrable par statut et par adhérent.
     */
    public List<ReservationResponseDTO> listerReservations(StatutReservation statut, Integer adherentId) {
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
     * Consulte une réservation par son id.
     */
    public ReservationResponseDTO consulterReservation(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Réservation avec l'id " + id + " introuvable."));
        return toResponseDTO(reservation);
    }

    /**
     * Annule une réservation.
     * RG-05 : Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE.
     * RG-06 : Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état.
     */
    public ReservationResponseDTO annulerReservation(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Réservation avec l'id " + id + " introuvable."));

        // RG-06 : Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état
        if (STATUTS_TERMINAUX.contains(reservation.getStatut())) {
            throw new BusinessRuleViolationException(
                    "RG-06 : Une réservation avec le statut " + reservation.getStatut() + " ne peut plus changer d'état.");
        }

        // RG-05 : Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE
        if (!STATUTS_ACTIFS.contains(reservation.getStatut())) {
            throw new BusinessRuleViolationException(
                    "RG-05 : Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE.");
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        Reservation saved = reservationRepository.save(reservation);
        return toResponseDTO(saved);
    }

    /**
     * Supprime une réservation.
     */
    public void supprimerReservation(Integer id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Réservation avec l'id " + id + " introuvable."));
        reservationRepository.delete(reservation);
    }

    /**
     * Convertit une entité Reservation en DTO de sortie.
     */
    private ReservationResponseDTO toResponseDTO(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setLivreId(reservation.getLivre().getBookId());
        dto.setLivreNom(reservation.getLivre().getBookName());
        dto.setAdherentId(reservation.getAdherent().getUserId());
        dto.setAdherentNom(reservation.getAdherent().getName());
        dto.setDateReservation(reservation.getDateReservation());
        dto.setDateExpiration(reservation.getDateExpiration());
        dto.setStatut(reservation.getStatut());
        return dto;
    }
}