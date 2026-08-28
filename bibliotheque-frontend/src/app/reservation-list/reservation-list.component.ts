import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Reservation } from '../_model/reservation';

@Component({
  selector: 'app-reservation-list',
  templateUrl: './reservation-list.component.html',
  styleUrls: ['./reservation-list.component.css']
})
export class ReservationListComponent {

  @Input() reservations: Reservation[] = [];
  @Input() annulationEnCours: number | null = null;
  @Output() annuler = new EventEmitter<number>();

  reservationAConfirmer: number | null = null;

  confirmerAnnulation(id: number): void {
    this.reservationAConfirmer = id;
  }

  validerAnnulation(id: number): void {
    this.reservationAConfirmer = null;
    this.annuler.emit(id);
  }

  abandonnerAnnulation(): void {
    this.reservationAConfirmer = null;
  }

  formaterDate(date: string): string {
    if (!date) {
      return '';
    }
    const d = this.parserDate(date);
    if (isNaN(d.getTime())) {
      return date;
    }
    return d.toLocaleDateString('fr-FR');
  }

  private parserDate(date: string): Date {
    const parts = date.split('-');
    if (parts.length === 3 && parts[0].length === 2 && parts[2].length === 4) {
      const jour = Number(parts[0]);
      const mois = Number(parts[1]) - 1;
      const annee = Number(parts[2]);
      return new Date(annee, mois, jour);
    }
    return new Date(date);
  }
}