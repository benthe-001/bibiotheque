/**
 * Messages d'erreur HTTP traduits en langage simple pour l'utilisateur.
 * Le composant appelant peut prefixer/suffixer selon son contexte
 * (ex. "catalogue", "comptes"...). Pour l'écran de connexion, préférer
 * messageErreurConnexion() qui distingue session expirée et échec
 * d'authentification.
 */
export function messageErreurHttp(err: any, action: string): string {
  if (!err || err.status === 0) {
    return `Impossible de ${action} : le service est momentanément injoignable. Réessayez dans un instant.`;
  }
  if (err.status === 401) {
    return `Votre session a expiré. Reconnectez-vous pour ${action}.`;
  }
  if (err.status === 403) {
    return `Vous n'avez pas l'autorisation de ${action}.`;
  }
  if (err.status === 404) {
    return `L'élément demandé est introuvable.`;
  }
  if (err.status >= 500) {
    return `Le service rencontre un problème pour ${action}. Réessayez plus tard.`;
  }
  return `Impossible de ${action} pour le moment. Réessayez.`;
}

/**
 * Message adapté à l'écran de connexion : ici l'utilisateur n'est pas encore
 * authentifié, donc on ne parle jamais de "session expirée". Un 401/400
 * signifie identifiants incorrects, un 0 un service injoignable, un 500 un
 * incident technique côté serveur.
 */
export function messageErreurConnexion(err: any): string {
  if (!err || err.status === 0) {
    return 'Impossible de se connecter : le service est momentanément injoignable. Réessayez dans un instant.';
  }
  if (err.status === 400 || err.status === 401) {
    return 'Identifiant ou mot de passe incorrect. Réessayez.';
  }
  if (err.status >= 500) {
    return "Le service rencontre un problème technique. Réessayez plus tard ou contactez le bibliothécaire.";
  }
  return 'La connexion est impossible pour le moment. Réessayez.';
}

