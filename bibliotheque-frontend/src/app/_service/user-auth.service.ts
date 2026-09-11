import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UserAuthService {

  constructor() { }

  public setRoles(roles: []) {
    localStorage.setItem('roles', JSON.stringify(roles));
  }

  /**
   * Lecture defensive : sans session (cle absente) ou payload corrompu,
   * on retourne un tableau vide au lieu de lever une exception
   * (JSON.parse(null) crashait toute l'application au rechargement).
   */
  public getRoles(): any[] {
    try {
      const raw = localStorage.getItem('roles');
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : (parsed ? [parsed] : []);
    } catch {
      return [];
    }
  }

  public setToken(jwtToken: string) {
    localStorage.setItem('jwtToken', jwtToken);
  }

  public getToken(): string | null {
    return localStorage.getItem('jwtToken');
  }

  public setUserId(userId: number) {
    localStorage.setItem('userId', JSON.stringify(userId));
  }

  public getUserId(): number | null {
    try {
      const raw = localStorage.getItem('userId');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  public setName(userId: number) {
    localStorage.setItem('name', JSON.stringify(userId));
  }

  public getName(): string | null {
    try {
      const raw = localStorage.getItem('name');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  public clear() {
    localStorage.clear();
  }

  public isLoggedIn() {
    return this.getRoles().length > 0 && !!this.getToken();
  }

}