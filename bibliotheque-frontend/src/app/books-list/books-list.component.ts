import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Books } from '../_model/books'
import { BooksService } from '../_service/books.service';
import { messageErreurHttp } from '../_util/message-erreur.util';

@Component({
  selector: 'app-books-list',
  templateUrl: './books-list.component.html',
  styleUrls: ['./books-list.component.css']
})
export class BooksListComponent implements OnInit {

  books: Books[] = [];
  etat: 'CHARGEMENT' | 'DONNEES' | 'VIDE' | 'ERREUR' = 'CHARGEMENT';
  messageErreur: string = '';

  constructor(private booksService: BooksService,
    private router: Router) { }

  ngOnInit(): void {
    this.getBooks();
  }

  public getBooks() {
    this.etat = 'CHARGEMENT';
    this.messageErreur = '';
    this.booksService.getBooksList().subscribe(data => {
      this.books = data;
      this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
    }, (err) => {
      this.etat = 'ERREUR';
      this.messageErreur = messageErreurHttp(err, 'afficher le catalogue');
    });
  }

  updateBook(bookId: number) {
    this.router.navigate(['update-book', bookId ]);
  }

  deleteBook(bookId: number) {
    // Confirmation avant suppression (action irréversible)
    if (!confirm('Supprimer définitivement ce livre du catalogue ?')) {
      return;
    }
    this.booksService.deleteBook(bookId).subscribe(() => {
      this.getBooks();
    });
  }

  bookDetails(bookId: number) {
    this.router.navigate(['book-details', bookId ]);
  }

}
