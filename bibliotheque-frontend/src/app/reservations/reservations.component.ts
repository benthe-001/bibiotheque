import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Reservation } from '../_model/reservation';
import { Users } from '../_model/users';
import { BooksService } from '../_service/books.service';
import { ReservationService } from '../_service/reservation.service';
import { UsersService } from '../_service/users.service';
import { messageErreurHttp } from '../_util/message-erreur.util';

export type EtatChargement = 'CHARGEMENT' | 'DONNEES' | 'VIDE' | 'ERREUR';

@Component({
  selector: 'app-reservations',
  templateUrl: './reservations.component.html',
  styleUrls: ['./reservations.component.css']
})
export class ReservationsComponent implements OnInit {

  etat: EtatChargement = 'CHARGEMENT';
  messageErreur: string = '';
  messageCreation: string = '';

  reservations: Reservation[] = [];
  livres: Books[] = [];
  adherents: Users[] = [];

  filtreStatut: string = 'TOUS';

  statuts: string[] = ['EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

  /** Libellés français lisibles pour les statuts (filtre et messages). */
  private static readonly LIBELLES_STATUTS: { [cle: string]: string } = {
    'EN_ATTENTE': 'En attente',
    'DISPONIBLE': 'Disponible',
    'ANNULEE': 'Annulée',
    'EXPIREE': 'Expirée',
    'HONOREE': 'Honorée'
  };

  libelleStatut(statut: string): string {
    return ReservationsComponent.LIBELLES_STATUTS[statut] || statut;
  }

  // État du formulaire
  formulaireEnCours: boolean = false;
  messageSucces: string = '';

  // État de l'annulation
  annulationEnCours: number | null = null;
  messageErreurAnnulation: string = '';

  constructor(
    private reservationService: ReservationService,
    private booksService: BooksService,
    private usersService: UsersService
  ) { }

  ngOnInit(): void {
    this.chargerDonnees();
  }

  chargerDonnees(): void {
    this.etat = 'CHARGEMENT';
    this.messageErreur = '';
    this.messageCreation = '';
    this.messageErreurAnnulation = '';

    const statut = this.filtreStatut === 'TOUS' ? undefined : this.filtreStatut;
    this.reservationService.getReservations(statut).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
      },
      error: (err) => {
        this.etat = 'ERREUR';
        this.messageErreur = messageErreurHttp(err, 'afficher les réservations');
      }
    });

    this.booksService.getBooksList().subscribe({
      next: (data) => this.livres = data,
      error: () => this.livres = []
    });

    this.usersService.getUsersList().subscribe({
      next: (data) => this.adherents = data.filter(user => this.estAdherent(user)),
      error: () => this.adherents = []
    });
  }

  filtrerParStatut(statut: string): void {
    this.filtreStatut = statut;
    this.etat = 'CHARGEMENT';
    this.messageErreur = '';
    this.messageCreation = '';
    this.messageErreurAnnulation = '';

    this.reservationService.getReservations(statut === 'TOUS' ? undefined : statut).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
      },
      error: (err) => {
        this.etat = 'ERREUR';
        this.messageErreur = messageErreurHttp(err, 'afficher les réservations');
      }
    });
  }

  creerReservation(livreId: number, adherentId: number): void {
    this.formulaireEnCours = true;
    this.messageSucces = '';
    this.messageErreur = '';
    this.messageErreurAnnulation = '';

    this.reservationService.createReservation(livreId, adherentId).subscribe({
      next: (reservation) => {
        this.formulaireEnCours = false;
        this.messageSucces = `Réservation n°${reservation.id} créée avec succès pour le livre "${reservation.livreTitre}".`;
        this.filtrerApresCreation();
      },
      error: (err) => {
        this.formulaireEnCours = false;
        this.messageCreation = this.extraireMessageErreur(err, "La réservation n'a pas pu être créée.");
      }
    });
  }

  private filtrerApresCreation(): void {
    const statut = this.filtreStatut;
    this.reservationService.getReservations(statut === 'TOUS' ? undefined : statut).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
      },
      error: (err) => {
        this.etat = 'ERREUR';
        this.messageErreur = messageErreurHttp(err, 'actualiser les réservations');
      }
    });
  }

  annulerReservation(id: number): void {
    this.annulationEnCours = id;
    this.messageErreurAnnulation = '';
    this.messageCreation = '';
    this.messageSucces = '';

    this.reservationService.annulerReservation(id).subscribe({
      next: (reservation) => {
        this.annulationEnCours = null;
        this.messageSucces = `La réservation n°${reservation.id} a été annulée.`;
        this.filtrerApresCreation();
      },
      error: (err) => {
        this.annulationEnCours = null;
        this.messageErreurAnnulation = this.extraireMessageErreur(err, 'Une erreur est survenue lors de l\'annulation.');
      }
    });
  }

  peutEtreAnnulee(statut: string): boolean {
    return statut === 'EN_ATTENTE' || statut === 'DISPONIBLE';
  }

  private estAdherent(user: Users): boolean {
    const roles = Array.isArray(user.role) ? user.role : [user.role];
    return roles.some(role => role && role.roleName === 'User');
  }

  private extraireMessageErreur(err: any, defaut: string): string {
    if (err.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que le backend est démarré, puis réessayez.';
    }
    let payload = err.error || {};
    if (typeof payload === 'string') {
      try {
        payload = JSON.parse(payload);
      } catch {
        payload = {};
      }
    }
    if (payload && typeof payload.error === 'string') {
      try {
        payload = JSON.parse(payload.error);
      } catch {
        payload = {};
      }
    }
    const regle = payload && typeof payload.regle === 'string'
      ? payload.regle.toUpperCase()
      : '';
    const messagesParRegle: { [regle: string]: string } = {
      'RG-01': 'Ce livre est actuellement disponible : réservez uniquement les livres indisponibles.',
      'RG-02': 'Ce livre fait déjà partie des réservations en cours de cet adhérent.',
      'RG-03': 'Cet adhérent a déjà 3 réservations en cours, le maximum autorisé.',
      'RG-05': 'Seule une réservation en attente ou disponible peut être annulée.',
      'RG-06': 'Cette réservation est déjà clôturée : elle ne peut plus être modifiée.'
    };
    if (messagesParRegle[regle]) {
      return messagesParRegle[regle];
    }
    if (err.status >= 500) {
      return 'Le service rencontre un problème. Réessayez plus tard.';
    }
    if (err.status === 404) {
      return 'Le livre ou l’adhérent sélectionné est introuvable.';
    }
    if (err.status === 400) {
      return 'Sélectionnez un livre et un adhérent valides avant de réserver.';
    }
    return defaut;
  }
}