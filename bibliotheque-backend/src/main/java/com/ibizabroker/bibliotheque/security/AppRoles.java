package com.ibizabroker.bibliotheque.security;

import java.util.HashSet;
import java.util.Set;

/**
 * Rôles canoniques du module réservation.
 * <p>
 * La base existante stocke les rôles historiques "Admin" / "User" : le mapping
 * vers les rôles canoniques est appliqué à la frontière sécurité (aucune
 * migration de données requise) :
 * <ul>
 *     <li>"Admin" équivaut à BIBLIOTHECAIRE</li>
 *     <li>"User" équivaut à ADHERENT</li>
 * </ul>
 */
public final class AppRoles {

    /** Adhérent : gère ses propres réservations uniquement. */
    public static final String ADHERENT = "ADHERENT";

    /** Bibliothécaire : gère toutes les réservations. */
    public static final String BIBLIOTHECAIRE = "BIBLIOTHECAIRE";

    /** Rôles historiques conservés pour la rétrocompatibilité des @PreAuthorize existants. */
    public static final String ADMIN = "Admin";
    public static final String USER = "User";

    private static final Set<String> EQUIVALENTS_BIBLIOTHECAIRE = Set.of("ADMIN");
    private static final Set<String> EQUIVALENTS_ADHERENT = Set.of("USER");

    private AppRoles() {
    }

    /**
     * Autorités Spring (sans préfixe ROLE_) associées à un roleName stocké en base :
     * le nom historique est conservé (rétrocompatibilité) et le rôle canonique
     * équivalent est ajouté.
     */
    public static Set<String> authorityRoleNames(String roleName) {
        Set<String> noms = new HashSet<>();
        if (roleName == null) {
            return noms;
        }
        String normalise = roleName.trim().toUpperCase();
        noms.add(roleName.trim());
        if (EQUIVALENTS_BIBLIOTHECAIRE.contains(normalise)) {
            noms.add(BIBLIOTHECAIRE);
        } else if (EQUIVALENTS_ADHERENT.contains(normalise)) {
            noms.add(ADHERENT);
        } else {
            noms.add(normalise);
        }
        return noms;
    }
}