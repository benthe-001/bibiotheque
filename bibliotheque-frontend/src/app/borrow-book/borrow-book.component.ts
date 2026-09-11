import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';
import { messageErreurHttp } from '../_util/message-erreur.util';

@Component({
  selector: 'app-borrow-book',
  templateUrl: './borrow-book.component.html',
  styleUrls: ['./borrow-book.component.css']
})
export class BorrowBookComponent implements OnInit {

  books: Books[] = [];
  etat: 'CHARGEMENT' | 'DONNEES' | 'VIDE' | 'ERREUR' = 'CHARGEMENT';
  messageErreurChargement: string = '';
  message: string = '';
  messageErreur: string = '';
  empruntEnCours: number | null = null;

  borrow: Borrow = new Borrow();

  constructor(
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private borrowService: BorrowService,
  ) { }

  userId = this.userAuthService.getUserId();

  ngOnInit(): void {
    this.getBooks();
  }

  public getBooks() {
    this.etat = 'CHARGEMENT';
    this.messageErreurChargement = '';
    this.message = '';
    this.messageErreur = '';
    this.booksService.getBooksList().subscribe(data => {
      this.books = data;
      this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
    }, (err) => {
      this.etat = 'ERREUR';
      this.messageErreurChargement = messageErreurHttp(err, "afficher les livres à emprunter");
    });
  }

  borrowBook(bookId: number) {
    this.empruntEnCours = bookId;
    this.message = '';
    this.messageErreur = '';
    this.borrow = new Borrow();
    this.borrow.bookId = bookId;
    this.borrow.userId = this.userId;
    this.borrowService.borrowBook(this.borrow).subscribe((data: any) => {
      this.empruntEnCours = null;
      // Le backend renvoie une phrase décrivant l'emprunt réussi
      this.message = typeof data === 'string' ? data : 'Emprunt effectué avec succès.';
      this.getBooks();
    }, (err) => {
      this.empruntEnCours = null;
      this.messageErreur = messageErreurHttp(err, "enregistrer l'emprunt");
    });
  }
}
