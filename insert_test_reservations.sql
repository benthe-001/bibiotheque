-- Réservation EN_ATTENTE
INSERT INTO reservation (id, livre_id, adherent_id, date_reservation, date_expiration, statut) VALUES
(501, 202, 301, NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', 'EN_ATTENTE')
ON CONFLICT DO NOTHING;

-- Réservation DISPONIBLE  
INSERT INTO reservation (id, livre_id, adherent_id, date_reservation, date_expiration, statut) VALUES
(502, 203, 302, NOW() - INTERVAL '3 days', NOW() + INTERVAL '4 days', 'DISPONIBLE')
ON CONFLICT DO NOTHING;

-- Réservation ANNULEA
INSERT INTO reservation (id, livre_id, adherent_id, date_reservation, date_expiration, statut) VALUES
(503, 204, 301, NOW() - INTERVAL '5 days', NOW() - INTERVAL '2 days', 'ANNULEA')
ON CONFLICT DO NOTHING;

-- Réservation EXPIREE
INSERT INTO reservation (id, livre_id, adherent_id, date_reservation, date_expiration, statut) VALUES
(504, 205, 302, NOW() - INTERVAL '10 days', NOW() - INTERVAL '3 days', 'EXPIREE')
ON CONFLICT DO NOTHING;

-- Réservation HONOREE
INSERT INTO reservation (id, livre_id, adherent_id, date_reservation, date_expiration, statut) VALUES
(505, 202, 302, NOW() - INTERVAL '8 days', NOW() - INTERVAL '1 days', 'HONOREE')
ON CONFLICT DO NOTHING;

SELECT id, livre_id, adherent_id, statut FROM reservation ORDER BY id;
