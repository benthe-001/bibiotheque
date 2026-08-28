import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Books } from '../_model/books';
import { Users } from '../_model/users';

@Component({
  selector: 'app-reservation-form',
  templateUrl: './reservation-form.component.html',
  styleUrls: ['./reservation-form.component.css']
})
export class ReservationFormComponent {

  @Input() livres: Books[] = [];
  @Input() adherents: Users[] = [];
  @Input() enCours: boolean = false;
  @Input() messageSucces: string = '';
  @Input() messageErreur: string = '';
  @Output() soumettre = new EventEmitter<{ livreId: number, adherentId: number }>();

  livreId: number | null = null;
  adherentId: number | null = null;

  get formulaireValide(): boolean {
    return this.livreId !== null && this.adherentId !== null;
  }

  soumettreFormulaire(): void {
    if (!this.formulaireValide || this.enCours) {
      return;
    }
    this.soumettre.emit({ livreId: this.livreId!, adherentId: this.adherentId! });
    this.livreId = null;
    this.adherentId = null;
  }
}