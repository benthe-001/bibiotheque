import { Component, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';

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

        const role = response.user.role[0].roleName;
        if (role === 'Admin') {
          this.router.navigate(['/books']);
        } else {
          this.router.navigate(['/borrow-book'])
        }
      },
      (error) => {
        this.connexionEnCours = false;
        this.messageErreur = error.status === 0
          ? "Le serveur est injoignable. Vérifiez que le backend est démarré."
          : "Identifiant ou mot de passe incorrect. Réessayez.";
      }
    );
  }

}