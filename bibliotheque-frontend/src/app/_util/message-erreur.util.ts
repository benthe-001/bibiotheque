/**
 * Messages d'erreur HTTP traduits en langage simple pour l'utilisateur.
 * Le composant appelant peut prefixer/suffixer selon son contexte
 * (ex. "catalogue", "comptes"...).
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
