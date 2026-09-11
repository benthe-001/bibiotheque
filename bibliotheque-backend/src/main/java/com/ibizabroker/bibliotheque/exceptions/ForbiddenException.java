package com.ibizabroker.bibliotheque.exceptions;

import lombok.Getter;

/**
 * 403 Forbidden : l'identité est connue mais les droits sont insuffisants.
 * Ne jamais confondre avec 401 (identité inconnue — token absent, invalide ou expiré).
 * <p>
 * Utilisée notamment pour les règles de sécurité RS-02, RS-03 et RS-04.
 */
@Getter
public class ForbiddenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Référence de la règle enfreinte (ex : "RS-03"), peut être null. */
    private final String regle;

    public ForbiddenException(String message) {
        this(null, message);
    }

    public ForbiddenException(String regle, String message) {
        super(message);
        this.regle = regle;
    }
}