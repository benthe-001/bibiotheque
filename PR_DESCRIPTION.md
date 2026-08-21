# 📌 Module Réservation — Description de la Pull Request

## 📖 Contexte

Ce module permet à un adhérent de réserver un livre indisponible et d'être prévenu dès qu'un exemplaire revient.

---

## 🖼️ Capture Swagger des 5 endpoints

![Swagger - Endpoints Réservation](URL_DE_VOTRE_CAPTURE)

> **Comment obtenir la capture :**
> 1. Ouvrez `http://localhost:8080/swagger-ui/index.html` dans votre navigateur
> 2. Cherchez la section **"reservation-controller"**
> 3. Dépliez les 5 endpoints pour voir les codes de retour
> 4. Faites une capture d'écran (`Win + Shift + S` sur Windows)
> 5. Glissez-déposez l'image directement dans la description de la PR sur GitHub (Option A)
>    **OU** déposez-la dans `screenshots/swagger_reservations.png` et poussez-la (Option B)

---

## 📚 Règles de gestion implémentées

| Réf. | Règle | Où c'est implémenté |
|---|---|---|
| **RG-01** | On ne peut réserver qu'un livre indisponible | Dans `ReservationService.creerReservation()` — vérifie `livre.getNoOfCopies() > 0` et lève une `BusinessRuleViolationException` (409) avec le message "RG-01 : ..." |
| **RG-02** | Un adhérent ne peut avoir qu'une seule réservation active sur un même livre | Dans `ReservationService.creerReservation()` — utilise `findByLivre_BookIdAndStatutIn` pour vérifier qu'aucune réservation active n'existe déjà pour ce livre et cet adhérent |
| **RG-03** | Un adhérent ne peut pas dépasser 3 réservations actives simultanées | Dans `ReservationService.creerReservation()` — utilise `findByAdherent_UserIdAndStatutIn` et vérifie que la taille est < 3 avant de créer |
| **RG-04** | dateExpiration = dateReservation + 7 jours | Dans `ReservationService.creerReservation()` — utilise `Calendar.add(Calendar.DATE, 7)` pour calculer la date d'expiration côté serveur |
| **RG-05** | Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE | Dans `ReservationService.annulerReservation()` — vérifie que le statut est dans `STATUTS_ACTIFS` (EN_ATTENTE, DISPONIBLE) |
| **RG-06** | Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état | Dans `ReservationService.annulerReservation()` — vérifie que le statut n'est pas dans `STATUTS_TERMINAUX` (ANNULEE, EXPIREE, HONOREE) |

---

## 🔗 Endpoints créés

| Verbe | Chemin | Rôle | Succès | Erreurs |
|---|---|---|---|---|
| POST | `/api/reservations` | Créer une réservation | 201 | 400, 404, 409 |
| GET | `/api/reservations` | Lister (filtrable par statut et adhérent) | 200 | — |
| GET | `/api/reservations/{id}` | Consulter | 200 | 404 |
| PATCH | `/api/reservations/{id}/annuler` | Annuler | 200 | 404, 409 |
| DELETE | `/api/reservations/{id}` | Supprimer | 204 | 404 |

---

## 🐳 Dockerisation

Le projet est dockerisé avec `docker-compose.yml` :
- **PostgreSQL** (port 5433)
- **Backend Spring Boot** (port 8080)
- **Frontend Angular + Nginx** (port 4200)

---

## 📁 Fichiers modifiés/créés

### Module Réservation (9 fichiers créés)
- `entity/StatutReservation.java`
- `entity/Reservation.java`
- `dao/ReservationRepository.java`
- `dto/ReservationRequestDTO.java`
- `dto/ReservationResponseDTO.java`
- `exceptions/BusinessRuleViolationException.java`
- `exceptions/InvalidRequestException.java`
- `service/ReservationService.java`
- `controller/ReservationController.java`

### Dockerisation (6 fichiers créés)
- `bibliotheque-backend/Dockerfile`
- `bibliotheque-backend/.dockerignore`
- `bibliotheque-frontend/Dockerfile`
- `bibliotheque-frontend/.dockerignore`
- `bibliotheque-frontend/nginx.conf`
- `docker-compose.yml`

### Fichiers modifiés (3)
- `configuration/WebSecurityConfiguration.java`
- `src/main/resources/application.properties`
- `src/environments/environment.prod.ts`

---

## ✅ Comment tester

```bash
# Démarrer les conteneurs
docker compose up -d

# Tester l'API
curl http://localhost:8080/api/reservations

# Ouvrir Swagger
http://localhost:8080/swagger-ui/index.html

# Ouvrir le frontend
http://localhost:4200
```

---

## 📝 Guide pour créer la PR

### Étape 1 : Pousser la branche
```bash
git push origin feature/reservation-benthe-diallo
```

### Étape 2 : Créer la PR sur GitHub
1. Allez sur `https://github.com/benthe-001/bibiotheque`
2. Cliquez sur **"Compare & pull request"** (bannière jaune)
3. Base : `main` ← Compare : `feature/reservation-benthe-diallo`
4. Copiez-collez le contenu de ce fichier dans la description

### Étape 3 : Ajouter la capture Swagger
- **Option A (recommandée)** : Glissez-déposez l'image directement dans la description
- **Option B** : Poussez l'image dans `screenshots/` et utilisez le lien raw GitHub

### Étape 4 : Relire par un pair
1. Onglet **"Files changed"** dans la PR
2. Cliquez sur **"Review changes"**
3. Laissez un commentaire ou approuvez