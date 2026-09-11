import { Component, OnInit } from '@angular/core';
import { Books } from '../_model/books';
import { Borrow } from '../_model/borrow';
import { BooksService } from '../_service/books.service';
import { BorrowService } from '../_service/borrow.service';
import { UserAuthService } from '../_service/user-auth.service';

@Component({
  selector: 'app-borrow-book',
  templateUrl: './borrow-book.component.html',
  styleUrls: ['./borrow-book.component.css']
})
export class BorrowBookComponent implements OnInit {

  books: Books[] = [];
  etat: 'CHARGEMENT' | 'DONNEES' | 'VIDE' | 'ERREUR' = 'CHARGEMENT';
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
    this.message = '';
    this.messageErreur = '';
    this.booksService.getBooksList().subscribe(data => {
      this.books = data;
      this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
    }, () => {
      this.etat = 'ERREUR';
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
    }, () => {
      this.empruntEnCours = null;
      this.messageErreur = "L'emprunt n'a pas pu être effectué. Le livre est peut-être indisponible.";
    });
  }
}
