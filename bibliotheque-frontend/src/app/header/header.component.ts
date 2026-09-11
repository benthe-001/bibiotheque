import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../_service/users.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  name = this.userAuthService.getName();
  initiale = this.name ? this.name.charAt(0) : '?';

  constructor(
    private userAuthService: UserAuthService,
    private router: Router,
    public userService: UsersService,
  ) { }

  ngOnInit(): void {
    // Rafraîchit le nom affiché si la session a changé (connexion/déconnexion)
    this.name = this.userAuthService.getName();
    this.initiale = this.name ? this.name.charAt(0) : '?';
  }

  public isLoggedIn() {
    return this.userAuthService.isLoggedIn();
  }

  public logout() {
    this.userAuthService.clear();
    this.router.navigate(['/']);
  }
}
