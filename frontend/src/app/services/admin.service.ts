import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Patient {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  email: string;
  birthDate: string; // fecha en formato ISO o similar
  status: boolean;
  strikes: number;
}

export interface Specialist {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  email: string;
  birthDate: string; // fecha en formato ISO o similar
  status: boolean;
  strikes: number;
}

// En tu admin.service.ts

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/v1/admin';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    return new HttpHeaders({
      'X-Admin-Id': user.id
    });
  }

  /*getPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.apiUrl}/patients`, {
      headers: this.getHeaders()
    });
  }*/

  getPatients(): Observable<Patient[]> {
    const headers = new HttpHeaders().set('X-Admin-Id', '1'); 
    return this.http.get<Patient[]>(`${this.apiUrl}/patients`, { headers });
  }

  getSpecialists(): Observable<Specialist[]> {
    return this.http.get<Specialist[]>(`${this.apiUrl}/specialists`, {
      headers: this.getHeaders()
    });
  }

  getPendingValidations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/pending-validations`);
  }

  validateUser(id: number, status: 'approved' | 'rejected'): Observable<any> {
    return this.http.patch(`${this.apiUrl}/admin/validate-user/${id}`, { status });
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
}