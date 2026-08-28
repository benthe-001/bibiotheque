import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Reservation } from '../_model/reservation';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {

  private baseURL = `${environment.apiUrl}/api/reservations`;

  constructor(private httpClient: HttpClient) { }

  getReservations(statut?: string, adherentId?: number): Observable<Reservation[]> {
    const params: any = {};
    if (statut) {
      params.statut = statut;
    }
    if (adherentId) {
      params.adherentId = adherentId;
    }
    return this.httpClient.get<Reservation[]>(`${this.baseURL}`, { params });
  }

  getReservationById(id: number): Observable<Reservation> {
    return this.httpClient.get<Reservation>(`${this.baseURL}/${id}`);
  }

  createReservation(livreId: number, adherentId: number): Observable<Reservation> {
    return this.httpClient.post<Reservation>(`${this.baseURL}`, { livreId, adherentId });
  }

  annulerReservation(id: number): Observable<Reservation> {
    return this.httpClient.patch<Reservation>(`${this.baseURL}/${id}/annuler`, {});
  }

  deleteReservation(id: number): Observable<Object> {
    return this.httpClient.delete(`${this.baseURL}/${id}`);
  }
}