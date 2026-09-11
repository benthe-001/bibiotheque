import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Users } from '../_model/users';
import { UsersService } from '../_service/users.service';
import { messageErreurHttp } from '../_util/message-erreur.util';

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit {

  users: Users[] = [];
  etat: 'CHARGEMENT' | 'DONNEES' | 'VIDE' | 'ERREUR' = 'CHARGEMENT';
  messageErreur: string = '';

  constructor(private usersService: UsersService,
    private router: Router) { }

  ngOnInit(): void {
    this.getUsers();
  }

  public getUsers() {
    this.etat = 'CHARGEMENT';
    this.messageErreur = '';
    this.usersService.getUsersList().subscribe(data => {
      this.users = data;
      this.etat = data.length === 0 ? 'VIDE' : 'DONNEES';
    }, (err) => {
      this.etat = 'ERREUR';
      this.messageErreur = messageErreurHttp(err, 'afficher les comptes');
    });
  }

  userDetails(userId: number) {
    this.router.navigate(['user-details', userId ]);
  }

  updateUser(userId: number) {
    this.router.navigate(['update-user', userId ]);
  }

  /** Nom du premier rôle, sans plante si le rôle est absent. */
  nomRole(user: Users): string {
    return user.role && user.role.length > 0 ? user.role[0].roleName : '—';
  }
}
