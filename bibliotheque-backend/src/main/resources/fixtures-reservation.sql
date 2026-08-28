-- ============================================================
-- FIXTURES - Module Réservation (Séance 2)
-- ============================================================
-- L1 (201) : livre DISPONIBLE, 3 exemplaires, aucun emprunt en cours
-- L2 (202), L3 (203), L4 (204), L5 (205) : empruntés par A3 et non rendus, 0 exemplaire
-- A1 (301) a1_reservataire : réservataire principal
-- A2 (302) a2_quota       : celui qui saturera son quota
-- A3 (303) a3_emprunteur  : l'emprunteur, il détient L2 à L5
-- Mot de passe des trois adhérents : admin123
-- ============================================================

-- ---------- LIVRES ----------
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies) VALUES
(201, 'L1 - Le livre disponible', 'Auteur L1', 'Roman', 3),
(202, 'L2 - Le livre emprunte 1', 'Auteur L2', 'Science-Fiction', 0),
(203, 'L3 - Le livre emprunte 2', 'Auteur L3', 'Policier', 0),
(204, 'L4 - Le livre emprunte 3', 'Auteur L4', 'Fantasy', 0),
(205, 'L5 - Le livre emprunte 4', 'Auteur L5', 'Biographie', 0)
ON CONFLICT (book_id) DO NOTHING;

-- ---------- ROLES ----------
-- S'assure que le rôle User existe (id 2)
INSERT INTO role (role_id, role_name) VALUES
(1, 'Admin'),
(2, 'User')
ON CONFLICT (role_id) DO NOTHING;

-- ---------- UTILISATEURS ----------
-- Hash BCrypt de "admin123"
INSERT INTO users (user_id, username, name, password) VALUES
(301, 'a1_reservataire', 'A1 Reservataire', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2'),
(302, 'a2_quota',       'A2 Quota',       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2'),
(303, 'a3_emprunteur',  'A3 Emprunteur',  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2')
ON CONFLICT (user_id) DO NOTHING;

-- ---------- LIAISON USER_ROLE ----------
INSERT INTO user_role (user_id, role_id) VALUES
(301, 2),
(302, 2),
(303, 2)
ON CONFLICT DO NOTHING;

-- ---------- EMPRUNTS (A3 détient L2 à L5, non rendus) ----------
INSERT INTO borrow (borrow_id, book_id, user_id, issue_date, return_date, due_date) VALUES
(401, 202, 303, NOW() - INTERVAL '10 days', NULL, NOW() - INTERVAL '3 days'),
(402, 203, 303, NOW() - INTERVAL '8 days',  NULL, NOW() - INTERVAL '1 days'),
(403, 204, 303, NOW() - INTERVAL '6 days',  NULL, NOW() + INTERVAL '1 days'),
(404, 205, 303, NOW() - INTERVAL '4 days',  NULL, NOW() + INTERVAL '3 days')
ON CONFLICT (borrow_id) DO NOTHING;

-- ---------- RÉSERVATIONS - Données de test pour tous les statuts ----------
-- ID 501-505 : Réservations avec différents statuts
-- RG-04 indique que dateExpiration = dateReservation + 7 jours

-- Réservation EN_ATTENTE (statut actif, réservé aujourd'hui)
INSERT INTO reservation (reservation_id, book_id, user_id, date_reservation, date_expiration, statut) VALUES
(501, 202, 301, NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', 'EN_ATTENTE')
ON CONFLICT (reservation_id) DO NOTHING;

-- Réservation DISPONIBLE (livre rendu, exemplaire disponible pour le réservataire)
INSERT INTO reservation (reservation_id, book_id, user_id, date_reservation, date_expiration, statut) VALUES
(502, 203, 302, NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', 'DISPONIBLE')
ON CONFLICT (reservation_id) DO NOTHING;

-- Réservation ANNULEA (annulée par l'utilisateur avant expiration)
INSERT INTO reservation (reservation_id, book_id, user_id, date_reservation, date_expiration, statut) VALUES
(503, 204, 301, NOW() - INTERVAL '5 days', NOW() - INTERVAL '2 days', 'ANNULEA')
ON CONFLICT (reservation_id) DO NOTHING;

-- Réservation EXPIREE (date de réservation dépassée, jamais honorée)
INSERT INTO reservation (reservation_id, book_id, user_id, date_reservation, date_expiration, statut) VALUES
(504, 205, 302, NOW() - INTERVAL '10 days', NOW() - INTERVAL '3 days', 'EXPIREE')
ON CONFLICT (reservation_id) DO NOTHING;

-- Réservation HONOREE (réservation terminée avec succès, livre fourni)
INSERT INTO reservation (reservation_id, book_id, user_id, date_reservation, date_expiration, statut) VALUES
(505, 202, 302, NOW() - INTERVAL '8 days', NOW() - INTERVAL '1 days', 'HONOREE')
ON CONFLICT (reservation_id) DO NOTHING;