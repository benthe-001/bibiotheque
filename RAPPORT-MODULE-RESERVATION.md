# Rapport — Module Réservation

## Contexte

Ce rapport documente l'implémentation complète du module **Réservation** pour le projet **Gestion de Bibliothèque**. Le module permet à un adhérent de réserver un livre déjà emprunté (indisponible) et d'être prévenu dès qu'un exemplaire revient.

---

## Étape 1 : Analyse de l'architecture existante

### 1.1 Structure du projet backend

Le projet backend est organisé selon le pattern **MVC (Modèle-Vue-Contrôleur)** avec les packages suivants :

```
bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/
├── configuration/       → Configuration Spring Security, CORS, JWT
├── controller/          → Contrôleurs REST (BooksController, BorrowController, AdminController, JwtController)
├── dao/                 → Repositories JPA (BooksRepository, BorrowRepository, UsersRepository)
├── entity/              → Entités JPA (Books, Borrow, Users, Role, JwtRequest, JwtResponse)
├── exceptions/          → Exceptions personnalisées (NotFoundException)
├── service/             → Services métier (JwtService)
└── util/                → Utilitaires (JwtUtil)
```

### 1.2 Technologies utilisées

| Technologie | Version | Rôle |
|---|---|---|
| Spring Boot | 2.7.18 | Framework principal |
| Java | 17 | Langage |
| PostgreSQL | — | Base de données |
| Lombok | — | Réduction du code boilerplate (`@Data`) |
| Spring Data JPA | — | Persistance |
| Spring Security + JWT | — | Authentification |
| springdoc-openapi-ui | 1.7.0 | Documentation Swagger |

### 1.3 Patterns observés dans le projet existant

- **Entités** : annotées `@Data`, `@Entity`, `@Table(name = "...")`, avec `@Id` et `@GeneratedValue`.
- **Repositories** : interfaces étendant `JpaRepository<Entité, Integer>`.
- **Contrôleurs** : annotés `@RestController`, `@RequestMapping("/...")`, avec `@CrossOrigin("http://localhost:4200/")`.
- **Exceptions** : classes annotées `@ResponseStatus(value = HttpStatus.XXX)`.
- **Sécurité** : les endpoints sont autorisés via `WebSecurityConfiguration` avec `.antMatchers(...).permitAll()`.

---

## Étape 2 : Création de l'énumération `StatutReservation`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/StatutReservation.java`

```java
public enum StatutReservation {
    EN_ATTENTE,
    DISPONIBLE,
    ANNULEE,
    EXPIREE,
    HONOREE
}
```

**Explication** : Cette énumération définit les 5 statuts possibles d'une réservation, conformément au cahier des charges.

---

## Étape 3 : Création de l'entité `Reservation`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Reservation.java`

```java
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "Reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "livre_id", nullable = false)
    private Books livre;

    @ManyToOne
    @JoinColumn(name = "adherent_id", nullable = false)
    private Users adherent;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using = JsonDataSerializer.class)
    private Date dateReservation;

    @Temporal(TemporalType.TIMESTAMP)
    @JsonSerialize(using = JsonDataSerializer.class)
    private Date dateExpiration;

    @Enumerated(EnumType.STRING)
    private StatutReservation statut;
}
```

**Explication** :
- `id` : identifiant généré automatiquement (IDENTITY).
- `livre` : relation `@ManyToOne` vers l'entité `Books` (obligatoire).
- `adherent` : relation `@ManyToOne` vers l'entité `Users` (obligatoire).
- `dateReservation` : date et heure générée par le serveur, jamais fournie par le client.
- `dateExpiration` : date et heure calculée (RG-04).
- `statut` : énumération stockée en chaîne de caractères (`EnumType.STRING`).
- `@JsonSerialize(using = JsonDataSerializer.class)` : formate les dates au format `dd-MM-yyyy` comme dans le projet existant.

---

## Étape 4 : Création du repository `ReservationRepository`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/ReservationRepository.java`

```java
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    List<Reservation> findByStatut(StatutReservation statut);
    List<Reservation> findByAdherent_UserId(Integer adherentId);
    List<Reservation> findByAdherent_UserIdAndStatutIn(Integer adherentId, List<StatutReservation> statuts);
    List<Reservation> findByLivre_BookIdAndStatutIn(Integer bookId, List<StatutReservation> statuts);
    List<Reservation> findByStatutIn(List<StatutReservation> statuts);
}
```

**Explication** :
- `findByStatut` : pour filtrer par statut (GET /api/reservations?statut=...).
- `findByAdherent_UserId` : pour filtrer par adhérent (GET /api/reservations?adherentId=...).
- `findByAdherent_UserIdAndStatutIn` : pour RG-03 (compter les réservations actives d'un adhérent).
- `findByLivre_BookIdAndStatutIn` : pour RG-02 (vérifier si un adhérent a déjà réservé un livre).
- `findByStatutIn` : pour lister les réservations actives.

---

## Étape 5 : Création des DTOs

### 5.1 DTO d'entrée `ReservationRequestDTO`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ReservationRequestDTO.java`

```java
@Data
public class ReservationRequestDTO {
    private Integer livreId;
    private Integer adherentId;
}
```

**Explication** : Le client n'envoie que `livreId` et `adherentId`. Tout le reste est déterminé par le serveur.

### 5.2 DTO de sortie `ReservationResponseDTO`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ReservationResponseDTO.java`

```java
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
```

**Explication** : L'entité `Reservation` ne sort jamais du service. Ce DTO expose les informations utiles au client, y compris les noms du livre et de l'adhérent pour une meilleure lisibilité.

---

## Étape 6 : Création des exceptions personnalisées

### 6.1 `BusinessRuleViolationException` (409 Conflict)

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/BusinessRuleViolationException.java`

```java
@ResponseStatus(value = HttpStatus.CONFLICT)
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
```

**Explication** : Cette exception est levée lorsqu'une règle de gestion (RG-01, RG-02, RG-03, RG-05, RG-06) est violée. Elle renvoie un code HTTP **409** avec un message qui nomme la règle enfreinte.

### 6.2 `InvalidRequestException` (400 Bad Request)

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/InvalidRequestException.java`

```java
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
```

**Explication** : Cette exception est levée lorsqu'une requête est invalide (par exemple, `livreId` ou `adherentId` manquant). Elle renvoie un code HTTP **400** avec un message qui indique quel champ manque.

---

## Étape 7 : Création du service `ReservationService`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/service/ReservationService.java`

### 7.1 Méthode `creerReservation(ReservationRequestDTO request)`

Cette méthode implémente les règles de gestion **RG-01, RG-02, RG-03, RG-04** :

```java
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
```

### 7.2 Méthode `listerReservations(StatutReservation statut, Integer adherentId)`

Cette méthode liste les réservations avec filtrage optionnel par statut et/ou par adhérent.

### 7.3 Méthode `consulterReservation(Integer id)`

Cette méthode consulte une réservation par son identifiant. Si elle n'existe pas, une `NotFoundException` (404) est levée.

### 7.4 Méthode `annulerReservation(Integer id)`

Cette méthode implémente les règles de gestion **RG-05 et RG-06** :

```java
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
```

### 7.5 Méthode `supprimerReservation(Integer id)`

Cette méthode supprime définitivement une réservation. Si elle n'existe pas, une `NotFoundException` (404) est levée.

### 7.6 Méthode privée `toResponseDTO(Reservation reservation)`

Cette méthode convertit une entité `Reservation` en `ReservationResponseDTO`. C'est le seul endroit où l'entité est transformée en DTO, garantissant que l'entité ne sort jamais du service.

---

## Étape 8 : Création du contrôleur `ReservationController`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/ReservationController.java`

```java
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponseDTO> creerReservation(@RequestBody ReservationRequestDTO request) {
        ReservationResponseDTO response = reservationService.creerReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponseDTO>> listerReservations(
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Integer adherentId) {
        List<ReservationResponseDTO> reservations = reservationService.listerReservations(statut, adherentId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDTO> consulterReservation(@PathVariable Integer id) {
        ReservationResponseDTO response = reservationService.consulterReservation(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/annuler")
    public ResponseEntity<ReservationResponseDTO> annulerReservation(@PathVariable Integer id) {
        ReservationResponseDTO response = reservationService.annulerReservation(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerReservation(@PathVariable Integer id) {
        reservationService.supprimerReservation(id);
        return ResponseEntity.noContent().build();
    }
}
```

**Explication** :
- Le contrôleur ne contient **aucune logique métier** — il délègue tout au service.
- Chaque endpoint est annoté avec `@Operation` et `@ApiResponses` pour la documentation Swagger.
- Les codes de retour sont conformes au cahier des charges :
  - POST → 201 (créé), 400 (requête invalide), 404 (introuvable), 409 (règle violée)
  - GET → 200
  - GET/{id} → 200, 404
  - PATCH/{id}/annuler → 200, 404, 409
  - DELETE/{id} → 204, 404

---

## Étape 9 : Mise à jour de la configuration de sécurité

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java`

```java
.authorizeRequests().antMatchers("/authenticate", "/borrow/**", "/admin/books/", "/admin/users", "/api/reservations/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
```

**Explication** : Le chemin `/api/reservations/**` a été ajouté à la liste des endpoints autorisés sans authentification, conformément au pattern existant.

---

## Étape 10 : Compilation et vérification

La compilation du projet a été effectuée avec succès :

```
mvn -f bibliotheque-backend/pom.xml clean compile
```

Résultat : **BUILD SUCCESS** — 31 fichiers source compilés sans erreur.

---

## Étape 11 : Création de la branche et commit

### 11.1 Création de la branche

```bash
git checkout -b feature/reservation-benthe-diallo
```

### 11.2 Commit

```bash
git commit -m "feat: ajout du module Réservation (entité, repository, service, contrôleur, DTO, règles de gestion RG-01 à RG-06)"
```

Résultat : **10 fichiers modifiés, 395 insertions, 1 suppression**.

---

## Récapitulatif des règles de gestion implémentées

| Réf. | Règle | Implémentation |
|---|---|---|
| **RG-01** | On ne peut réserver qu'un livre indisponible | Dans `ReservationService.creerReservation()` — vérifie `livre.getNoOfCopies() > 0` et lève `BusinessRuleViolationException` avec le message "RG-01 : ..." |
| **RG-02** | Un adhérent ne peut avoir qu'une seule réservation active sur un même livre | Dans `ReservationService.creerReservation()` — vérifie via `findByLivre_BookIdAndStatutIn` si une réservation active existe déjà pour ce livre et cet adhérent |
| **RG-03** | Un adhérent ne peut pas dépasser 3 réservations actives simultanées | Dans `ReservationService.creerReservation()` — vérifie via `findByAdherent_UserIdAndStatutIn` que le nombre de réservations actives est < 3 |
| **RG-04** | dateExpiration = dateReservation + 7 jours | Dans `ReservationService.creerReservation()` — utilise `Calendar` pour ajouter 7 jours à la date de réservation |
| **RG-05** | Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE | Dans `ReservationService.annulerReservation()` — vérifie que le statut est dans `STATUTS_ACTIFS` |
| **RG-06** | Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état | Dans `ReservationService.annulerReservation()` — vérifie que le statut n'est pas dans `STATUTS_TERMINAUX` |

---

## Récapitulatif des endpoints

| Verbe | Chemin | Rôle | Succès | Erreurs |
|---|---|---|---|---|
| POST | `/api/reservations` | Créer une réservation | 201 | 400, 404, 409 |
| GET | `/api/reservations` | Lister, filtrable par statut et par adhérent | 200 | — |
| GET | `/api/reservations/{id}` | Consulter | 200 | 404 |
| PATCH | `/api/reservations/{id}/annuler` | Annuler | 200 | 404, 409 |
| DELETE | `/api/reservations/{id}` | Supprimer | 204 | 404 |

---

## Fichiers créés/modifiés

### Fichiers créés (9)

1. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/StatutReservation.java`
2. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Reservation.java`
3. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/ReservationRepository.java`
4. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ReservationRequestDTO.java`
5. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ReservationResponseDTO.java`
6. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/BusinessRuleViolationException.java`
7. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/InvalidRequestException.java`
8. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/service/ReservationService.java`
9. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/ReservationController.java`

### Fichier modifié (1)

1. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java`

---

## Points d'attention pour la suite

1. **Test unitaire RG-03** : Un test unitaire sur la limite de 3 réservations actives peut être ajouté.
2. **Endpoint réservations expirées** : Un endpoint `GET /api/reservations/expirees` peut être ajouté.
3. **Passage automatique en EXPIREE** : Un job planifié (`@Scheduled`) peut être ajouté pour passer automatiquement en `EXPIREE` les réservations dont la date est dépassée.
4. **Pull Request** : La branche `feature/reservation-benthe-diallo` doit être poussée et une Pull Request créée avec la capture Swagger des 5 endpoints et une phrase par règle de gestion.