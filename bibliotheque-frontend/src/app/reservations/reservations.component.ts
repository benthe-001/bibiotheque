import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Reservation } from '../_model/reservation';
import { Users } from '../_model/users';
import { BooksService } from '../_service/books.service';
import { ReservationService } from '../_service/reservation.service';
import { UsersService } from '../_service/users.service';

export type EtatChargement = 'CHARGEMENT' | 'DONNEES' | 'VIDE' | 'ERREUR';

@Component({
  selector: 'app-reservations',
  templateUrl: './reservations.component.html',
  styleUrls: ['./reservations.component.css']
})
export class ReservationsComponent implements OnInit {

  etat: EtatChargement = 'CHARGEMENT';
  messageErreur: string = '';

  reservations: Reservation[] = [];
  livres: Books[] = [];
  adherents: Users[] = [];

  filtreStatut: string = 'TOUS';

  statuts: string[] = ['EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

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
    this.messageErreurAnnulation = '';

    const statut = this.filtreStatut === 'TOUS' ? undefined : this.filtreStatut;
    this.reservationService.getReservations(statut).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
      },
      error: (err) => {
        this.etat = 'ERREUR';
        this.messageErreur = this.extraireMessageErreur(err, 'Le serveur est injoignable. Vérifiez que le backend est démarré.');
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
    this.messageErreurAnnulation = '';

    this.reservationService.getReservations(statut === 'TOUS' ? undefined : statut).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
      },
      error: (err) => {
        this.etat = 'ERREUR';
        this.messageErreur = this.extraireMessageErreur(err, 'Le serveur est injoignable. Vérifiez que le backend est démarré.');
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
        this.messageErreur = this.extraireMessageErreur(err, 'La réservation n’a pas pu être créée.');
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
        this.messageErreur = this.extraireMessageErreur(err, 'Le serveur est injoignable. Vérifiez que le backend est démarré.');
      }
    });
  }

  annulerReservation(id: number): void {
    this.annulationEnCours = id;
    this.messageErreurAnnulation = '';
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
      'RG-01': 'Impossible de réserver ce livre : il est disponible.',
      'RG-02': 'Impossible de réserver ce livre une deuxième fois : cet livre possède déjà une réservation active.',
      'RG-03': 'Impossible de réserver : cet adhérent a atteint le quota de 3 réservations actives.',
      'RG-05': 'Cette réservation ne peut pas être annulée dans son état actuel.',
      'RG-06': 'Cette réservation est terminée et ne peut plus changer d’état.'
    };
    if (messagesParRegle[regle]) {
      return messagesParRegle[regle];
    }
    if (err.status >= 500) {
      return 'La réservation ne peut pas être traitée pour le moment. Réessayez plus tard.';
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