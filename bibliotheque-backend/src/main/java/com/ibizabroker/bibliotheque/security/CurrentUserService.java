package com.ibizabroker.bibliotheque.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Source unique de l'identité de l'appelant : le SecurityContext, alimenté par le
 * JwtRequestFilter à partir du token. Jamais le corps de la requête (RS-04).
 */
@Component
public class CurrentUserService {

    /** Retourne l'utilisateur courant, ou null si non authentifié. */
    public UserPrincipal getCurrentUser() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification != null
                && authentification.isAuthenticated()
                && authentification.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentification.getPrincipal();
        }
        return null;
    }

    public boolean isBibliothecaire() {
        UserPrincipal utilisateur = getCurrentUser();
        return utilisateur != null && utilisateur.hasRole(AppRoles.BIBLIOTHECAIRE);
    }

    public boolean isAdherent() {
        UserPrincipal utilisateur = getCurrentUser();
        return utilisateur != null && utilisateur.hasRole(AppRoles.ADHERENT);
    }

    /** Retourne l'id de l'utilisateur courant, ou null si non déterminable. */
    public Integer getCurrentUserId() {
        UserPrincipal utilisateur = getCurrentUser();
        return utilisateur == null ? null : utilisateur.getUserId();
    }
}