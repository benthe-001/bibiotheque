import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Observable } from 'rxjs';
import { Users } from '../_model/users';
import { UserAuthService } from './user-auth.service';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UsersService {

  private baseURL = `${environment.apiUrl}/admin/users`;
  requestHeader = new HttpHeaders(
    { 'No-Auth': 'True' }
  );

  constructor(
    private httpClient: HttpClient,
    private userAuthService: UserAuthService
  ) { }

  public login(loginData: NgForm) {
    return this.httpClient.post(`${environment.apiUrl}/authenticate`, loginData, {
      headers: this.requestHeader,
    });
  }

  public roleMatch(allowedRoles: any): boolean {
    const userRoles: any = this.userAuthService.getRoles();

    return Array.isArray(userRoles)
      && userRoles.some(userRole => allowedRoles.includes(userRole.roleName));
  }

  getUsersList(): Observable<Users[]> {
    return this.httpClient.get<Users[]>(`${this.baseURL}`);
  }

  createUser(user: Users): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, user);
  }

  getUserById(userId: number): Observable<Users> {
    return this.httpClient.get<Users>(`${this.baseURL}/${userId}`);
  }

  updateUser(userId: number, user: Users): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${userId}`, user);
  }

}
