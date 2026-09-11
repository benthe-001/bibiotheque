# Pull Request — feature/reservation-securite-benthe-diallo

## Titre de la PR

**Sécuriser l'API de réservation (rôles ADHERENT / BIBLIOTHECAIRE, 401/403) et la prouver par des tests**

- **Branche** : `feature/reservation-securite-benthe-diallo`
- **Base** : travail de réservation existant (module `/api/reservations`)

## Contexte

L'API de réservation fonctionnait mais était ouverte à tous (`permitAll` sur `/api/reservations/**`) :
n'importe qui pouvait consulter les réservations d'un autre adhérent, en créer au nom de qui il
voulait ou annuler celles qu'il n'avait pas faites. Cette PR ferme l'API selon la matrice
d'autorisation demandée, puis la prouve par des tests automatisés.

## Où chaque règle de sécurité est implémentée (RS-01 → RS-05)

| Réf. | Règle | Où (une phrase) |
|---|---|---|
| **RS-01** | Sans token, tout endpoint de réservation renvoie 401 | `WebSecurityConfiguration` : `/api/reservations/**` sort du `permitAll` et tombe sous `anyRequest().authenticated()` ; le `JwtAuthenticationEntryPoint` (401 JSON) répond aux requêtes non authentifiées. |
| **RS-02** | Un ADHERENT qui tente une action réservée au BIBLIOTHECAIRE reçoit 403 | `ReservationController` : `@PreAuthorize("hasRole('BIBLIOTHECAIRE')")` sur `DELETE /api/reservations/{id}` (le refus `@PreAuthorize` est converti en 403 JSON par le handler `AccessDeniedException` du `GlobalExceptionHandler`). |
| **RS-03** | Un ADHERENT qui accède à la réservation d'un autre reçoit 403 | `ReservationService.verifierAccesProprietaire()` : contrôle d'appartenance sur `GET /{id}` et `PATCH /{id}/annuler` (avant les règles métier), lève `ForbiddenException("RS-03", …)` → 403 JSON. |
| **RS-04** | Un ADHERENT ne peut pas créer de réservation au nom d'un autre | `ReservationService.resoudreAdherentId()` : pour un ADHERENT, `adherentId` vient **du token** (`CurrentUserService` → `UserPrincipal`), l'`adherentId` du corps est ignoré ; seul le BIBLIOTHECAIRE peut créer au nom de quelqu'un d'autre. |
| **RS-05** | `GET /api/reservations` par un ADHERENT ne retourne que ses réservations | `ReservationService.restreindreFiltreAdherent()` : le filtre `adherentId` est écrasé par l'identité du token pour un ADHERENT, quel que soit le filtre demandé. |

## Matrice d'autorisation livrée

| Endpoint | Anonyme | ADHERENT | BIBLIOTHECAIRE |
|---|---|---|---|
| `POST /api/reservations` | 401 | OUI — pour lui-même (identité du token) | OUI — pour n'importe qui |
| `GET /api/reservations` | 401 | OUI — ses réservations seulement | OUI — toutes |
| `GET /api/reservations/{id}` | 401 | OUI — si elle lui appartient (sinon 403) | OUI — toutes |
| `PATCH /api/reservations/{id}/annuler` | 401 | OUI — si elle lui appartient (sinon 403) | OUI — toutes |
| `DELETE /api/reservations/{id}` | 401 | 403 | OUI |

## Distinguer 401 et 403

- **401** (je ne sais pas qui vous êtes) : token absent, invalide ou expiré → `JwtAuthenticationEntryPoint` renvoie `401` avec un JSON `ErrorResponseDTO` homogène.
- **403** (je sais qui vous êtes, mais vous n'avez pas le droit) : rôles insuffisants (`@PreAuthorize` → `AccessDeniedException`) ou réservation n'appartenant pas à l'adhérent (`ForbiddenException`) → 403 JSON.
- Aucun utilisateur non authentifié n'atteint un `@PreAuthorize` (il reçoit un 401), et aucun utilisateur authentifié sans droits ne reçoit de 401.

## Choix d'architecture

- **Rôles** : le système de rôles existant (`Admin` / `User` en base) est conservé ; le nouveau
  package `security/` mappe les rôles à la frontière sécurité — `Admin` → `BIBLIOTHECAIRE`,
  `User` → `ADHERENT` (les rôles historiques restent aussi accordés pour ne pas casser les
  `@PreAuthorize` existants). Aucune migration de données requise.
- **Identité** : nouveau `UserPrincipal` (`UserDetails` portant `userId`) alimenté par le
  `JwtRequestFilter` ; `CurrentUserService` est la source unique de l'identité courante
  (jamais le corps de la requête).
- **Tests** : le service est passé en **injection par constructeur** pour être testable par mocks purs.
- **Zéro secret codé en dur** : `JWT_SECRET`, `JWT_VALIDITY_SECONDS`, `APP_CORS_ALLOWED_ORIGINS`
  dans `application.properties` ; mot de passe PostgreSQL du `docker-compose` via `.env`
  (voir `.env.example`, `.env` est git-ignoré).

## Preuve par les tests

Commande (aucune base ne doit tourner, tout est automatisé) :

```bash
cd bibliotheque-backend
./mvnw test            # Windows : mvnw.cmd test
```

Résultat obtenu : **BUILD SUCCESS — 8 tests, 0 échec, 0 erreur**.

| Suite | Ce que ça prouve |
|---|---|
| `ReservationServiceTest` (2 tests, pur Mockito, aucune base) | **RG-03** : un adhérent ayant 2 réservations actives peut en créer une 3ᵉ (statut EN_ATTENTE, dates RG-04 vérifiées) ; un adhérent ayant 3 réservations actives est refusé (409 RG-03, aucun `save`). |
| `ReservationApiSecurityIntegrationTest` (5 tests, chaîne de filtrage JWT réelle, H2 en mémoire) | Sans token → **401** (RS-01) ; token ADHERENT → **200** avec ses seules réservations (RS-05) ; consultation de la réservation d'un autre → **403 RS-03** ; suppression par un ADHERENT → **403** (RS-02) ; création avec `adherentId` d'un autre adhérent dans le corps → créée **pour lui-même** (RS-04). |
| `BibliothequeApplicationTests` | Le contexte complet démarre sur H2 (`mvnw test` autonome). |

*(Coller ici la capture du résultat des tests.)*

## Checklist de revue

- [x] `mvnw test` vert sans aucune manipulation manuelle ni base PostgreSQL
- [x] Noms de méthodes de test explicites (ce qui est testé, jamais test1)
- [x] 401 ≠ 403 respectés partout
- [x] Aucun secret codé en dur (JWT, CORS, PostgreSQL)
- [x] Frontend non impacté (l'UI envoie déjà le token via `AuthInterceptor`)
- [ ] Capture des tests collée dans cette description
- [ ] Relue par un pair

## Points suivants possibles (hors périmètre)

- Appliquer le même verrouillage aux endpoints `/borrow/**` (encore `permitAll`).
- Renommer les rôles en base (`ADHERENT` / `BIBLIOTHECAIRE`) lors d'une migration dédiée.
- Job planifié pour passer les réservations expirées en `EXPIREE`.
