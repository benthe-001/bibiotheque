import { Component, OnInit } from '@angular/core';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { messageErreurHttp } from '../_util/message-erreur.util';

@Component({
  selector: 'app-return-book',
  templateUrl: './return-book.component.html',
  styleUrls: ['./return-book.component.css']
})
export class ReturnBookComponent implements OnInit {

  borrow: Borrow[] = [];
  private livres: { bookId: number; bookName: string }[] = [];
  etat: 'CHARGEMENT' | 'DONNEES' | 'VIDE' | 'ERREUR' = 'CHARGEMENT';
  messageErreurChargement: string = '';
  message: string = '';
  messageErreur: string = '';
  retourEnCours: number | null = null;

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private userAuthService: UserAuthService
  ) { }

  /** Identifiant de session, relu à chaque usage (jamais null après le guard). */
  get userId(): number {
    return this.userAuthService.getUserId() ?? 0;
  }

  ngOnInit(): void {
    this.getBorrowsByUser();
    // Charge le catalogue pour afficher les titres plutôt que les identifiants
    this.booksService.getBooksList().subscribe(data => {
      this.livres = data;
    }, () => {
      this.livres = [];
    });
  }

  /** Titre lisible d'un livre à partir de son identifiant. */
  nomLivre(bookId: number): string {
    const livre = this.livres.find(l => l.bookId === bookId);
    return livre ? livre.bookName : 'Livre n°' + bookId;
  }

  public getBorrowsByUser() {
    this.etat = 'CHARGEMENT';
    this.messageErreurChargement = '';
    this.message = '';
    this.messageErreur = '';
    this.borrowService.getBooksBorrowedByUser(this.userId).subscribe(data => {
      this.borrow = data;
      this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
    }, (err) => {
      this.etat = 'ERREUR';
      this.messageErreurChargement = messageErreurHttp(err, 'afficher vos emprunts');
    });
  }

  /** Indique si l'emprunt a dépassé sa date d'échéance. */
  enRetard(b: Borrow): boolean {
    return b.returnDate === null && b.dueDate !== null && new Date(b.dueDate) < new Date();
  }

  public returnBook(borrowId: number) {
    this.retourEnCours = borrowId;
    this.message = '';
    this.messageErreur = '';
    const brw: Borrow = new Borrow();
    brw.borrowId = borrowId;
    this.borrowService.returnBook(brw).subscribe(() => {
      this.retourEnCours = null;
      this.message = 'Livre retourné avec succès. Merci !';
      this.getBorrowsByUser();
    }, (err) => {
      this.retourEnCours = null;
      this.messageErreur = messageErreurHttp(err, 'enregistrer le retour');
    });
  }

  /** Formate une date en format lisible français (ou renvoie la valeur brute). */
  formaterDate(date: Date | string | null): string {
    if (!date) {
      return '—';
    }
    const d = date instanceof Date ? date : this.parserDate(date);
    return isNaN(d.getTime()) ? String(date) : d.toLocaleDateString('fr-FR');
  }

  private parserDate(date: string): Date {
    const parts = date.split('-');
    if (parts.length === 3 && parts[0].length === 2 && parts[2].length === 4) {
      return new Date(Number(parts[2]), Number(parts[1]) - 1, Number(parts[0]));
    }
    return new Date(date);
  }

}
