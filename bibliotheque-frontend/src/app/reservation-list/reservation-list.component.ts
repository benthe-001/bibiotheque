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

  confirmerAnnulation(id: number): void {
    if (confirm('Voulez-vous vraiment annuler cette réservation ?')) {
      this.annuler.emit(id);
    }
  }

  formaterDate(date: string): string {
    if (!date) {
      return '';
    }
    const d = new Date(date);
    return d.toLocaleDateString('fr-FR') + ' ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}