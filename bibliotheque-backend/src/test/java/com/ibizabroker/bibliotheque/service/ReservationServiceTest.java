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
import com.ibizabroker.bibliotheque.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RG-03 : limite de 3 réservations actives simultanées — test unitaire de la couche service.
 * Les repositories sont simulés (Mockito) : ce test passe sans qu'aucune base ne tourne.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Integer ADHERENT_ID = 7;
    private static final Integer LIVRE_ID = 1;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ReservationService reservationService;

    private Users adherent;
    private Books livreIndisponible;

    @BeforeEach
    void setUp() {
        adherent = new Users();
        adherent.setUserId(ADHERENT_ID);
        adherent.setName("Alice Adherent");

        // RG-01 : le livre doit être indisponible pour être réservable
        livreIndisponible = new Books();
        livreIndisponible.setBookId(LIVRE_ID);
        livreIndisponible.setBookName("Livre indisponible");
        livreIndisponible.setNoOfCopies(0);
    }

    private Reservation reservationActive(Integer userId, Integer bookId) {
        Reservation reservation = new Reservation();
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        Users utilisateur = new Users();
        utilisateur.setUserId(userId);
        Books livre = new Books();
        livre.setBookId(bookId);
        reservation.setAdherent(utilisateur);
        reservation.setLivre(livre);
        return reservation;
    }

    /** RS-04 : l'adherentId est absent du corps — l'identité vient du token. */
    private ReservationRequestDTO requeteDepuisToken() {
        ReservationRequestDTO request = new ReservationRequestDTO();
        request.setLivreId(LIVRE_ID);
        return request;
    }

    /** Prépare le contexte : adhérent authentifié (identité du token) avec N réservations actives. */
    private void arrangeContexteAdherentAvec(int nombreReservationsActives) {
        when(currentUserService.isBibliothecaire()).thenReturn(false);
        when(currentUserService.getCurrentUserId()).thenReturn(ADHERENT_ID);
        when(booksRepository.findById(LIVRE_ID)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(ADHERENT_ID)).thenReturn(Optional.of(adherent));
        when(reservationRepository.findByLivre_BookIdAndStatutIn(eq(LIVRE_ID), anyList()))
                .thenReturn(List.of());
        List<Reservation> actives = new ArrayList<>();
        for (int i = 0; i < nombreReservationsActives; i++) {
            actives.add(reservationActive(ADHERENT_ID, 10 + i));
        }
        when(reservationRepository.findByAdherent_UserIdAndStatutIn(eq(ADHERENT_ID), anyList()))
                .thenReturn(actives);
    }

    @Test
    void creerReservation_adherentAvecDeuxReservationsActives_autoriseLaTroisieme() {
        // Arrange : l'adhérent a 2 réservations actives (sur d'autres livres)
        arrangeContexteAdherentAvec(2);
        Reservation sauvegarde = new Reservation();
        sauvegarde.setId(99);
        sauvegarde.setLivre(livreIndisponible);
        sauvegarde.setAdherent(adherent);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(sauvegarde);

        // Act
        ReservationResponseDTO response = reservationService.creerReservation(requeteDepuisToken());

        // Assert : la 3e réservation est créée, au nom de l'adhérent identifié par le token
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(99);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository, times(1)).save(captor.capture());
        Reservation creee = captor.getValue();

        assertThat(creee.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        assertThat(creee.getAdherent().getUserId()).isEqualTo(ADHERENT_ID);
        assertThat(creee.getDateReservation()).isNotNull();
        assertThat(creee.getDateExpiration()).isNotNull();
        // RG-04 : dateExpiration = dateReservation + 7 jours (tolérance 1h pour les changements d'heure)
        long difference = creee.getDateExpiration().getTime() - creee.getDateReservation().getTime();
        long septJours = 7L * 24 * 3600 * 1000;
        assertThat(difference).isCloseTo(septJours, within(3600_000L));
    }

    @Test
    void creerReservation_adherentAvecTroisReservationsActives_refusee() {
        // Arrange : l'adhérent a déjà 3 réservations actives
        arrangeContexteAdherentAvec(3);

        // Act & Assert : RG-03 enfreinte, aucune sauvegarde
        BusinessRuleViolationException exception = catchThrowableOfType(
                () -> reservationService.creerReservation(requeteDepuisToken()),
                BusinessRuleViolationException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getRegle()).isEqualTo("RG-03");
        assertThat(exception.getMessage()).contains("RG-03");
        verify(reservationRepository, never()).save(any(Reservation.class));
    }
}