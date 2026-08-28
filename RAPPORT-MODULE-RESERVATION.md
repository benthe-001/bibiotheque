# Rapport — Module Réservation

## Contexte

Ce rapport documente l'implémentation complète du module **Réservation** pour le projet **Gestion de Bibliothèque**. Le module permet à un adhérent de réserver un livre déjà emprunté (indisponible) et d'être prévenu dès qu'un exemplaire revient.

---

## Étape 1 : Analyse de l'architecture existante

### 1.1 Structure du projet backend

Le projet backend est organisé selon le pattern **MVC (Modèle-Vue-Contrôleur)** avec les packages suivants :

```
bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/
├── configuration/       → Configuration Spring Security, CORS, JWT, OpenAPI
├── controller/          → Contrôleurs REST (BooksController, BorrowController, AdminController, JwtController, ReservationController)
├── dao/                 → Repositories JPA (BooksRepository, BorrowRepository, UsersRepository, ReservationRepository)
├── dto/                 → DTOs (ReservationRequestDTO, ReservationResponseDTO, ErrorResponseDTO)
├── entity/              → Entités JPA (Books, Borrow, Users, Role, Reservation, StatutReservation)
├── exceptions/          → Exceptions personnalisées (NotFoundException, BusinessRuleViolationException, InvalidRequestException, GlobalExceptionHandler)
├── service/             → Services métier (JwtService, ReservationService)
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
| Docker / Docker Compose | — | Conteneurisation |

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
    private String livreTitre;
    private Integer adherentId;
    private String adherentNom;
    private Date dateReservation;
    private Date dateExpiration;
    private StatutReservation statut;
}
```

**Explication** : L'entité `Reservation` ne sort jamais du service. Ce DTO expose les informations utiles au client. Le champ `livreTitre` est utilisé conformément aux exigences de la collection Postman.

### 5.3 DTO d'erreur `ErrorResponseDTO`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ErrorResponseDTO.java`

```java
@Data
@AllArgsConstructor
public class ErrorResponseDTO {
    private String regle;
    private String message;
}
```

**Explication** : Ce DTO standardise les réponses d'erreur. Le champ `regle` identifie la règle de gestion enfreinte (RG-01, RG-02, etc.) et `message` explique l'erreur.

---

## Étape 6 : Création des exceptions personnalisées

### 6.1 `BusinessRuleViolationException` (409 Conflict)

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/BusinessRuleViolationException.java`

```java
@Getter
@ResponseStatus(value = HttpStatus.CONFLICT)
public class BusinessRuleViolationException extends RuntimeException {
    private final String regle;
    public BusinessRuleViolationException(String regle, String message) {
        super(message);
        this.regle = regle;
    }
}
```

**Explication** : Cette exception est levée lorsqu'une règle de gestion (RG-01, RG-02, RG-03, RG-05, RG-06) est violée. Elle renvoie un code HTTP **409** avec le code de la règle (`regle`) et un message explicatif.

### 6.2 `InvalidRequestException` (400 Bad Request)

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/InvalidRequestException.java`

```java
@Getter
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends RuntimeException {
    private final String regle;
    public InvalidRequestException(String message) {
        super(message);
        this.regle = null;
    }
}
```

**Explication** : Cette exception est levée lorsqu'une requête est invalide (par exemple, `livreId` ou `adherentId` manquant). Elle renvoie un code HTTP **400**.

### 6.3 `GlobalExceptionHandler` (Handler global)

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(ex.getRegle(), ex.getMessage()));
    }
    // ... autres handlers
}
```

**Explication** : Ce handler centralise la gestion des erreurs et formate les réponses avec `regle` et `message`. Il gère aussi les erreurs de type mismatch (statut invalide → 400) et les corps de requête illisibles.

---

## Étape 7 : Création du service `ReservationService`

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/service/ReservationService.java`

### 7.1 Méthode `creerReservation(ReservationRequestDTO request)`

Cette méthode implémente les règles de gestion **RG-01, RG-02, RG-03, RG-04** :

```java
public ReservationResponseDTO creerReservation(ReservationRequestDTO request) {
    // Validation des champs obligatoires : on liste TOUS les champs manquants
    List<String> manquants = new ArrayList<>();
    if (request.getLivreId() == null) manquants.add("livreId");
    if (request.getAdherentId() == null) manquants.add("adherentId");
    if (!manquants.isEmpty()) {
        throw new InvalidRequestException("Champ(s) obligatoire(s) manquant(s) : " + String.join(", ", manquants) + ".");
    }

    // Vérification de l'existence du livre
    Books livre = booksRepository.findById(request.getLivreId())
            .orElseThrow(() -> new NotFoundException("Livre avec l'id " + request.getLivreId() + " introuvable."));

    // Vérification de l'existence de l'adhérent
    Users adherent = usersRepository.findById(request.getAdherentId())
            .orElseThrow(() -> new NotFoundException("Adherent avec l'id " + request.getAdherentId() + " introuvable."));

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
    if (reservationsActivesAdherent.size() >= 3) {
        throw new BusinessRuleViolationException("RG-03",
                "RG-03 : L'adherent \"" + adherent.getName() + "\" a déjà " + reservationsActivesAdherent.size()
                        + " réservations actives, le maximum autorisé est de 3.");
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
            .orElseThrow(() -> new NotFoundException("Reservation avec l'id " + id + " introuvable."));

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
- Les codes de retour sont conformes au cahier des charges.

---

## Étape 9 : Mise à jour de la configuration de sécurité

**Fichier** : `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java`

```java
.authorizeRequests().antMatchers("/authenticate", "/borrow/**", "/admin/books/", "/admin/users", "/api/reservations/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
```

**Explication** : Le chemin `/api/reservations/**` a été ajouté à la liste des endpoints autorisés sans authentification, conformément au pattern existant.

---

## Étape 10 : Création des fixtures SQL

**Fichier** : `bibliotheque-backend/src/main/resources/fixtures-reservation.sql`

Ce fichier contient les données de test conformes aux exigences de la collection Postman :

| Réf. | Donnée | État exigé |
|---|---|---|
| L1 (201) | Un livre | Disponible — 3 exemplaires, aucun emprunt en cours |
| L2 (202), L3 (203), L4 (204), L5 (205) | Quatre livres | Tous empruntés et non rendus — 0 exemplaire |
| A1 (301) | Un adhérent | Réservataire principal (`a1_reservataire`) |
| A2 (302) | Un adhérent | Celui qui saturera son quota (`a2_quota`) |
| A3 (303) | Un adhérent | L'emprunteur : il détient L2 à L5 (`a3_emprunteur`) |

Mot de passe des trois adhérents : `admin123`

---

## Étape 11 : Dockerisation du projet

### 11.1 Dockerfile backend

**Fichier** : `bibliotheque-backend/Dockerfile`

```dockerfile
# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 11.2 Dockerfile frontend

**Fichier** : `bibliotheque-frontend/Dockerfile`

```dockerfile
# Build stage
FROM node:18-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# Run stage
FROM nginx:alpine
COPY --from=build /app/dist/bibliotheque-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 11.3 Docker Compose

**Fichier** : `docker-compose.yml`

```yaml
services:
  db:
    image: postgres:15-alpine
    container_name: bibliotheque-db
    environment:
      POSTGRES_DB: bibliotheque
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: Benthe@2001
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build:
      context: ./bibliotheque-backend
      dockerfile: Dockerfile
    container_name: bibliotheque-backend
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/bibliotheque
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: Benthe@2001
    ports:
      - "8080:8080"

  frontend:
    build:
      context: ./bibliotheque-frontend
      dockerfile: Dockerfile
    container_name: bibliotheque-frontend
    depends_on:
      - backend
    ports:
      - "4200:80"

volumes:
  postgres_data:
```

---

## Étape 12 : Tests effectués

### 12.1 Tests manuels avec curl

| Test | Requête | Résultat attendu | Résultat obtenu |
|---|---|---|---|
| RG-01 | POST /api/reservations (L1=201, A1=301) | 409 + regle=RG-01 | ✅ `{"regle":"RG-01","message":"RG-01 : Le livre \"L1 - Le livre disponible\" est disponible..."}` |
| Nominal | POST /api/reservations (L2=202, A1=301) | 201 + statut=EN_ATTENTE | ✅ `{"id":5,"livreId":202,"livreTitre":"L2 - Le livre emprunte 1",...}` |
| RG-02 | POST /api/reservations (L2=202, A1=301) | 409 + regle=RG-02 | ✅ `{"regle":"RG-02","message":"RG-02 : L'adherent \"A1 Reservataire\" a déjà une réservation active..."}` |
| RG-03 | POST /api/reservations (L5=205, A2=302) après 3 réservations | 409 + regle=RG-03 | ✅ `{"regle":"RG-03","message":"RG-03 : L'adherent \"A2 Quota\" a déjà 3 réservations actives, le maximum autorisé est de 3."}` |
| RG-05 | PATCH /api/reservations/8/annuler | 200 + statut=ANNULEE | ✅ `{"id":8,...,"statut":"ANNULEE"}` |
| RG-06 | PATCH /api/reservations/8/annuler (2e fois) | 409 + regle=RG-05 + message RG-06 | ✅ `{"regle":"RG-05","message":"RG-05 : Une reservation ne peut être annulée... RG-06 : Le statut ANNULEE est terminal..."}` |
| 400 | POST /api/reservations (corps vide) | 400 + les 2 champs nommés | ✅ `{"regle":null,"message":"Champ(s) obligatoire(s) manquant(s) : livreId, adherentId."}` |
| 404 | POST /api/reservations (livre=999999) | 404 | ✅ `{"regle":null,"message":"Livre avec l'id 999999 introuvable."}` |
| 400 | GET /api/reservations?statut=PEUT_ETRE | 400 + valeurs acceptées | ✅ `{"regle":null,"message":"La valeur 'PEUT_ETRE' n'est pas valide pour le parametre 'statut'. Valeurs acceptees : EN_ATTENTE, DISPONIBLE, ANNULEE, EXPIREE, HONOREE."}` |

### 12.2 Tests avec la collection Postman

La collection `bibliotheque-reservation.postman_collection.json` est prête à être importée dans Postman et exécutée avec le Collection Runner. Elle couvre :
- **0 - Authentification** : récupération des jetons admin et adhérent
- **1 - Jeu de données** : affichage de L1-L5, A1-A3 et des emprunts
- **2 - Parcours nominal** : créer, consulter, lister, filtrer, annuler
- **3 - Règles de gestion** : RG-01 à RG-06, chacune déclenchée volontairement
- **4 - Validation et 404** : champs manquants, ressources absentes
- **5 - Sécurité** : 401 sans jeton, 403 sans le rôle
- **6 - Nettoyage** : remise à zéro

---

## Récapitulatif des règles de gestion implémentées

| Réf. | Règle | Implémentation |
|---|---|---|
| **RG-01** | On ne peut réserver qu'un livre indisponible | Dans `ReservationService.creerReservation()` — vérifie `livre.getNoOfCopies() > 0` et lève `BusinessRuleViolationException("RG-01", ...)` |
| **RG-02** | Un adhérent ne peut avoir qu'une seule réservation active sur un même livre | Dans `ReservationService.creerReservation()` — vérifie via `findByLivre_BookIdAndStatutIn` si une réservation active existe déjà |
| **RG-03** | Un adhérent ne peut pas dépasser 3 réservations actives simultanées | Dans `ReservationService.creerReservation()` — vérifie via `findByAdherent_UserIdAndStatutIn` que le nombre est < 3 |
| **RG-04** | dateExpiration = dateReservation + 7 jours | Dans `ReservationService.creerReservation()` — utilise `Calendar.add(Calendar.DATE, 7)` |
| **RG-05** | Une réservation ne peut être annulée que si son statut est EN_ATTENTE ou DISPONIBLE | Dans `ReservationService.annulerReservation()` — vérifie que le statut est dans `STATUTS_ACTIFS` |
| **RG-06** | Une réservation ANNULEE, EXPIREE ou HONOREE ne peut plus changer d'état | Dans `ReservationService.annulerReservation()` — vérifie que le statut n'est pas dans `STATUTS_TERMINAUX` |

---

## Récapitulatif des endpoints

| Verbe | Chemin | Rôle | Succès | Erreurs |
|---|---|---|---|---|
| POST | `/api/reservations` | Créer une réservation | 201 | 400, 404, 409 |
| GET | `/api/reservations` | Lister, filtrable par statut et par adhérent | 200 | 400 (statut invalide) |
| GET | `/api/reservations/{id}` | Consulter | 200 | 404 |
| PATCH | `/api/reservations/{id}/annuler` | Annuler | 200 | 404, 409 |
| DELETE | `/api/reservations/{id}` | Supprimer | 204 | 404 |

---

## Fichiers créés/modifiés

### Fichiers créés (12) — Module Réservation

1. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/StatutReservation.java`
2. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/entity/Reservation.java`
3. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dao/ReservationRepository.java`
4. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ReservationRequestDTO.java`
5. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ReservationResponseDTO.java`
6. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/dto/ErrorResponseDTO.java`
7. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/BusinessRuleViolationException.java`
8. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/InvalidRequestException.java`
9. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/exceptions/GlobalExceptionHandler.java`
10. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/service/ReservationService.java`
11. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/controller/ReservationController.java`
12. `bibliotheque-backend/src/main/resources/fixtures-reservation.sql`

### Fichiers créés (6) — Dockerisation

1. `bibliotheque-backend/Dockerfile`
2. `bibliotheque-backend/.dockerignore`
3. `bibliotheque-frontend/Dockerfile`
4. `bibliotheque-frontend/.dockerignore`
5. `bibliotheque-frontend/nginx.conf`
6. `docker-compose.yml`

### Fichiers modifiés (3)

1. `bibliotheque-backend/src/main/java/com/ibizabroker/bibliotheque/configuration/WebSecurityConfiguration.java`
2. `bibliotheque-backend/src/main/resources/application.properties`
3. `bibliotheque-frontend/src/environments/environment.prod.ts`

---

## Points d'attention pour la suite

1. **Test unitaire RG-03** : Un test unitaire sur la limite de 3 réservations actives peut être ajouté.
2. **Endpoint réservations expirées** : Un endpoint `GET /api/reservations/expirees` peut être ajouté.
3. **Passage automatique en EXPIREE** : Un job planifié (`@Scheduled`) peut être ajouté pour passer automatiquement en `EXPIREE` les réservations dont la date est dépassée.
4. **Pull Request** : La branche `feature/reservation-benthe-diallo` doit être poussée et une Pull Request créée avec la capture Swagger des 5 endpoints et une phrase par règle de gestion.