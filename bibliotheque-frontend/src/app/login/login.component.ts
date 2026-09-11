import { Component, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';
import { messageErreurConnexion } from '../_util/message-erreur.util';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  connexionEnCours: boolean = false;
  messageErreur: string = '';

  constructor(private userService: UsersService,
    private userAuthSerivce: UserAuthService,
    private router: Router
  ) { }

  ngOnInit() {
  }

  login(loginForm: NgForm) {
    if (loginForm.invalid || this.connexionEnCours) {
      return;
    }
    this.connexionEnCours = true;
    this.messageErreur = '';
    this.userService.login(loginForm.value).subscribe(
      (response: any) => {
        this.userAuthSerivce.setRoles(response.user.role);
        this.userAuthSerivce.setToken(response.jwtToken);
        this.userAuthSerivce.setUserId(response.user.userId);
        this.userAuthSerivce.setName(response.user.name);

        const roles = Array.isArray(response.user.role) ? response.user.role : [response.user.role];
        const roleNames = roles.map((r: any) => r && r.roleName).filter((r: any) => !!r);
        if (roleNames.includes('Admin')) {
          this.router.navigate(['/books']);
        } else if (roleNames.includes('User')) {
          this.router.navigate(['/borrow-book'])
        } else {
          // Rôle inconnu : pas de redirection silencieuse, message explicite.
          this.connexionEnCours = false;
          this.messageErreur = 'Votre compte est connecté mais sans rôle reconnu. Contactez le bibliothécaire.';
        }
      },
      (error) => {
        this.connexionEnCours = false;
        this.messageErreur = messageErreurConnexion(error);
      }
    );
  }

}