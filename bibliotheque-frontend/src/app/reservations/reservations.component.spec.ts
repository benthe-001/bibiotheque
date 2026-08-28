import { of, throwError } from 'rxjs';
import { ReservationsComponent } from './reservations.component';

describe('ReservationsComponent', () => {
  let reservationService: any;
  let booksService: any;
  let usersService: any;
  let component: ReservationsComponent;

  beforeEach(() => {
    reservationService = {
      getReservations: jasmine.createSpy('getReservations').and.returnValue(of([])),
      createReservation: jasmine.createSpy('createReservation')
    };
    booksService = {
      getBooksList: jasmine.createSpy('getBooksList').and.returnValue(of([]))
    };
    usersService = {
      getUsersList: jasmine.createSpy('getUsersList').and.returnValue(of([
        { userId: 1, name: 'Alice' },
        { userId: 2, name: 'Bob' }
      ]))
    };
    component = new ReservationsComponent(reservationService, booksService, usersService);
  });

  it('charge tous les adhérents fournis par l API', () => {
    component.chargerDonnees();

    expect(component.adherents.length).toBe(2);
    expect(component.adherents[1].userId).toBe(2);
    expect(component.etat).toBe('VIDE');
  });

  it('affiche une erreur quand le rafraîchissement échoue', () => {
    reservationService.getReservations.and.returnValues(of([]), throwError(() => ({ status: 0 })));
    component.chargerDonnees();
    component.filtrerParStatut('TOUS');

    expect(component.etat).toBe('ERREUR');
    expect(component.messageErreur).toContain('serveur est injoignable');
  });
});