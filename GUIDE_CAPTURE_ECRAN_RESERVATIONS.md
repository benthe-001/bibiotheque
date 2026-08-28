# Guide de Capture d'Écran - Écran de Gestion des Réservations

## 📸 Captures d'écran requises pour la PR

La PR nécessite 4 captures d'écran pour valider les 4 états clés du système.

---

## 1️⃣ CAPTURE : État de Chargement

**URL :** `http://localhost:4200/#/reservations`  
**Préconditions :** Authentifié en tant que Admin ou User

**Étapes :**
1. Naviguer vers `/reservations`
2. **Immédiatement** (avant 2 secondes), prendre une capture
3. Vous devriez voir :
   - Spinner (animation circulaire) au centre
   - Texte "Chargement des réservations..." en gris
   - Pas de tableau
   - Pas de filtre
   - Pas de formulaire

**Éléments à vérifier sur la capture :**
- ✅ Spinner visible
- ✅ Message explicite
- ✅ Interface dégagée (pas de tableau figé)
- ✅ Pas de message d'erreur

**Fichier à enregistrer :** `screenshot-01-chargement.png`

---

## 2️⃣ CAPTURE : État Données

**URL :** `http://localhost:4200/#/reservations`  
**Préconditions :** Authentifié + données en base de données

**Étapes :**
1. Naviguer vers `/reservations`
2. Attendre le chargement complet (spinner disparu)
3. **Prendre une capture** du tableau rempli

**Vous devriez voir :**
- Filtres de statut au-dessus du tableau
- Tableau avec colonnes : Livre | Adhérent | Statut | Date réservation | Date expiration | Action
- Au moins 1-2 lignes de réservations
- Badges colorés selon statut
- Dates formatées en français (jj/mm/aaaa)
- Formulaire "Nouvelle réservation" en bas
- Bouton "Annuler" sur les lignes EN_ATTENTE/DISPONIBLE

**Éléments à vérifier sur la capture :**
- ✅ Filtres visibles et fonctionnels
- ✅ Tableau avec toutes les colonnes
- ✅ Badges de couleur cohérents
- ✅ Dates lisibles au format français
- ✅ Boutons d'action visibles
- ✅ Formulaire visible en bas

**Fichier à enregistrer :** `screenshot-02-donnees.png`

---

## 3️⃣ CAPTURE : État Liste Vide

**URL :** `http://localhost:4200/#/reservations`  
**Préconditions :** Authentifié + aucune réservation en base (ou filtrer par statut sans résultats)

**Étapes :**
1. Naviguer vers `/reservations`
2. Attendre le chargement
3. Optionnel : Filtrer par un statut qui n'a pas de réservations
4. **Prendre une capture**

**Vous devriez voir :**
- Alert info (bleu clair) avec message "Aucune réservation"
- Pas de tableau
- Pas de ligne vide
- Filtres toujours visibles
- Formulaire toujours visible

**Éléments à vérifier sur la capture :**
- ✅ Message explicite et lisible
- ✅ Alert info (couleur cohérente)
- ✅ Pas de tableau vide sans en-têtes (bug courant)
- ✅ Formulaire toujours accessible
- ✅ Pas de message d'erreur

**Fichier à enregistrer :** `screenshot-03-vide.png`

---

## 4️⃣ CAPTURE : État Erreur 409

**URL :** `http://localhost:4200/#/reservations`  
**Préconditions :** Authentifié + avoir un utilisateur avec 3 réservations actives

**Étapes :**
1. Naviguer vers `/reservations`
2. Attendre le chargement
3. Dans le formulaire, sélectionner :
   - Un livre indisponible
   - Un adhérent qui a **déjà 3 réservations**
4. Cliquer sur "Réserver"
5. **Prendre une capture** avant que le message disparaisse

**Vous devriez voir :**
- Alert danger (rouge) dans le formulaire
- Message du serveur lisible, par exemple :
  - "Limite de 3 réservations atteinte"
  - ou "Réservation déjà existante pour ce livre"
- Bouton "Réserver" redevient actif
- Formulaire n'est pas réinitialisé
- Liste n'est pas modifiée

**Éléments à vérifier sur la capture :**
- ✅ Alert danger avec bordure rouge
- ✅ Message d'erreur compréhensible
- ✅ Pas de message générique "Une erreur est survenue"
- ✅ Pas d'alert() pop-up
- ✅ Formulaire toujours visible

**Fichier à enregistrer :** `screenshot-04-erreur-409.png`

---

## Alternatives si les Conditions Précédentes ne sont pas Réunies

### Si la base est vide (pas de réservations)
- Créer une réservation manuellement via l'API
- Puis capturer l'état "Données"

### Si l'utilisateur n'a pas 3 réservations
- Utiliser l'API pour créer 3 réservations
- Puis essayer d'en créer une 4ème pour obtenir le 409

### Pour l'erreur backend (alternative au 409)
Si la 409 est trop difficile à reproduire, on peut aussi capturer :
- Arrêter le backend : `docker-compose stop backend`
- Recharger `/reservations`
- Capture : Message "Le serveur est injoignable..."
- Redémarrer : `docker-compose start backend`
- Cliquer "Réessayer"

---

## Outils de Capture

### Windows
- **Snipping Tool** (intégré) : Win + Shift + S
- **PrintScreen** : Ctrl + PrtScn (capture l'écran entier)
- **Firefox/Chrome** : F12 → Outil capture d'écran (Ctrl+Shift+S)

### Linux
```bash
gnome-screenshot -a  # Sélection rectangulaire
import screenshot.png  # ImageMagick
```

### Tous les OS
- Utiliser le navigateur : F12 → "Outil de capture"
- Chevron de zoom : 100% pour une capture lisible

---

## Organisation des Fichiers

Créer un dossier `screenshots-reservations/` à la racine du projet :

```
c:\dev\bibiotheque\
├── screenshots-reservations/
│   ├── 01-chargement.png
│   ├── 02-donnees.png
│   ├── 03-vide.png
│   └── 04-erreur-409.png
├── PR_DESCRIPTION_FRONTEND_RESERVATIONS.md
└── ...
```

---

## Checklist Avant de Committer les Screenshots

- [ ] Les 4 fichiers PNG sont dans `screenshots-reservations/`
- [ ] Chaque capture est claire et lisible (pas trop zoomé/dézoomé)
- [ ] Les URLs et messages sont en français
- [ ] Pas de données sensibles (pas de tokens, pas de mots de passe)
- [ ] Chaque capture montre bien le cas correspondant
- [ ] Les fichiers sont nommés de façon claire
- [ ] Taille raisonnable (< 1 MB par fichier)

---

## Committer les Screenshots

```bash
git add screenshots-reservations/
git commit -m "docs: Ajouter captures d'écran pour la PR - Écran de Gestion des Réservations"
git push origin feature/reservation-ui-benthe-diallo
```

---

## Afficher les Screenshots dans la PR

Dans la description de la PR GitHub, intégrer les images :

```markdown
## 📸 Captures d'écran

### État de Chargement
![Chargement](screenshots-reservations/01-chargement.png)

### État Données
![Données](screenshots-reservations/02-donnees.png)

### État Liste Vide
![Vide](screenshots-reservations/03-vide.png)

### État Erreur 409
![Erreur 409](screenshots-reservations/04-erreur-409.png)
```

---

**Note :** Les captures d'écran sont essentielles pour la revue de code. Elles permettent au reviewer de valider visuellement que l'interface remplit tous les critères sans avoir à la déployer localement.
