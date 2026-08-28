# Frontend: écran de gestion des réservations

## Résumé

Cette Pull Request ajoute et finalise l’écran Angular de gestion des réservations.

## Changements

- Affichage des réservations avec filtrage par statut.
- Formulaire de réservation avec L1 visible mais refusé par RG-01.
- Sélection limitée aux adhérents (`User`).
- Messages métier gérés par le frontend pour RG-01, RG-02 et RG-03.
- Confirmation d’annulation intégrée à l’interface, sans popup navigateur.
- Correction du contraste et harmonisation visuelle avec Book List.
- Conservation des erreurs HTTP par l’intercepteur Angular.

## Règles métier vérifiées

- RG-01 : un livre disponible ne peut pas être réservé.
- RG-02 : un même livre ne peut pas être réservé deux fois par le même adhérent.
- RG-03 : un adhérent est limité à 3 réservations actives.
- RG-05/RG-06 : seules les réservations annulables peuvent être annulées.

## Captures d’écran

- État de chargement : `screenshots-reservations/01-chargement.png`
- Liste remplie : `screenshots-reservations/02-donnees.png`
- Liste vide : `screenshots-reservations/03-vide.png`
- Refus 409 affiché : `screenshots-reservations/04-refus-409.png`

## Vérification

- `npm run build` réussi.
- API de réservation testée avec les réponses RG-01, RG-02 et RG-03.
- Conteneur frontend reconstruit avec Docker Compose.

## Relecture

Reviewer demandé : à assigner sur GitHub.

## Branche

`feature/reservation-ui-benthe-diallo`
