import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  email: string;
  birthDate: string;
  status: number;
  strikes: number;
  role: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private apiUrl = 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/admin';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-Admin-Id': '1'
    });
  }

  getPatients(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/patients`, {
      headers: this.getHeaders()
    });
  }

  getSpecialists(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/specialists`, {
      headers: this.getHeaders()
    });
  }

  deletePatient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/patients/${id}`, {
      headers: this.getHeaders()
    });
  }

  deleteSpecialist(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/specialists/${id}`, {
      headers: this.getHeaders()
    });
  }

  suspendUser(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${id}/suspend`, {});
  }
}