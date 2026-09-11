package com.ibizabroker.bibliotheque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequestDTO;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.Role;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.service.JwtService;
import com.ibizabroker.bibliotheque.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sécurité des endpoints de réservation, avec un vrai filtrage JWT et une base H2
 * en mémoire (profil "test") : aucune PostgreSQL ne tourne.
 * <p>
 * Règles prouvées : RS-01 (401 sans token), RS-02 (403 sur action BIBLIOTHECAIRE),
 * RS-03 (403 sur réservation d'autrui), RS-04 (identité du token), RS-05 (ses seules données).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Users alice;
    private Users bob;
    private Books livre;
    private Books autreLivre;
    private Integer reservationBobId;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        usersRepository.deleteAll();
        booksRepository.deleteAll();

        // Rôles historiques "User" (rétrocompat) -> mappés en ADHERENT par la couche sécurité
        Role roleUserAlice = new Role();
        roleUserAlice.setRoleName("User");
        alice = creerAdherent("alice", "Alice Adherent", "motdepasse-alice", roleUserAlice);

        Role roleUserBob = new Role();
        roleUserBob.setRoleName("User");
        bob = creerAdherent("bob", "Bob Adherent", "motdepasse-bob", roleUserBob);

        livre = creerLivre("Livre indisponible");
        autreLivre = creerLivre("Autre livre indisponible");

        creerReservation(alice, livre);
        reservationBobId = creerReservation(bob, livre).getId();
    }

    private Users creerAdherent(String username, String nom, String motDePasse, Role role) {
        Users utilisateur = new Users();
        utilisateur.setUsername(username);
        utilisateur.setName(nom);
        utilisateur.setPassword(passwordEncoder.encode(motDePasse));
        utilisateur.setRole(Set.of(role));
        return usersRepository.save(utilisateur);
    }

    private Books creerLivre(String nom) {
        Books livreACreer = new Books();
        livreACreer.setBookName(nom);
        livreACreer.setNoOfCopies(0); // RG-01 : réservable car indisponible
        return booksRepository.save(livreACreer);
    }

    private Reservation creerReservation(Users adherent, Books livreReserve) {
        Reservation reservation = new Reservation();
        reservation.setLivre(livreReserve);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(reservation.getDateReservation());
        calendar.add(Calendar.DATE, 7);
        reservation.setDateExpiration(calendar.getTime());
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        return reservationRepository.save(reservation);
    }

    /** Génère un vrai JWT signé comme en production, via le JwtUtil de l'application. */
    private String tokenDe(String username) {
        UserDetails userDetails = jwtService.loadUserByUsername(username);
        return jwtUtil.generateToken(userDetails);
    }

    @Test
    void getReservations_sansToken_renvoie401() throws Exception {
        // RS-01
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReservations_avecTokenAdherent_renvoie200_etUniquementSesPropresReservations() throws Exception {
        // RS-05
        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + tokenDe("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].adherentId").value(alice.getUserId()))
                .andExpect(jsonPath("$[0].livreId").value(livre.getBookId()));
    }

    @Test
    void consulterReservation_adherentConsulteReservationsDUnAutre_renvoie403() throws Exception {
        // RS-03 : alice consulte la réservation de bob
        mockMvc.perform(get("/api/reservations/{id}", reservationBobId)
                        .header("Authorization", "Bearer " + tokenDe("alice")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.regle").value("RS-03"));
    }

    @Test
    void supprimerReservation_adherent_renvoie403_carReserveAuBibliothecaire() throws Exception {
        // RS-02 : la suppression est réservée au BIBLIOTHECAIRE
        mockMvc.perform(delete("/api/reservations/{id}", reservationBobId)
                        .header("Authorization", "Bearer " + tokenDe("alice")))
                .andExpect(status().isForbidden());
    }

    @Test
    void creerReservation_adherentQuiMetLAdherentIdDUnAutreEstCreeePourLuiMeme() throws Exception {
        // RS-04 : alice met l'id de bob dans le corps — l'identité du token prime,
        // la réservation est créée au nom d'alice, pas de bob.
        ReservationRequestDTO requete = new ReservationRequestDTO();
        requete.setLivreId(autreLivre.getBookId());
        requete.setAdherentId(bob.getUserId());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + tokenDe("alice"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adherentId").value(alice.getUserId()))
                .andExpect(jsonPath("$.adherentNom").value("Alice Adherent"));
    }
}