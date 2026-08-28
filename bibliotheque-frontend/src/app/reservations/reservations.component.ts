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

  filtreStatut: string = '';

  statuts: string[] = ['EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];

  // État du formulaire
  formulaireLivreId: number | null = null;
  formulaireAdherentId: number | null = null;
  formulaireEnCours: boolean = false;
  messageSucces: string = '';
  messageErreurFormulaire: string = '';

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

    this.reservationService.getReservations().subscribe({
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
      next: (data) => this.adherents = data.filter(u => [301, 302, 303].includes(u.userId)),
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
        this.messageErreur = this.extraireMessageErreur(err, 'Une erreur est survenue lors de la création de la réservation.');
      }
    });
  }

  private filtrerApresCreation(): void {
    const statut = this.filtreStatut;
    this.reservationService.getReservations(statut === 'TOUS' ? undefined : statut).subscribe({
      next: (data) => {
        this.reservations = data;
        this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
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

  private extraireMessageErreur(err: any, defaut: string): string {
    if (err.status === 0) {
      return 'Le serveur est injoignable. Vérifiez que le backend est démarré, puis réessayez.';
    }
    if (err.error && err.error.message) {
      return err.error.message;
    }
    if (err.error && typeof err.error === 'string') {
      return err.error;
    }
    return defaut;
  }
}