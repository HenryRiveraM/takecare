import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Patient {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  email: string;
  birthDate: string;
  status: number | boolean | null;
  strikes: number;
}

export interface Specialist {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  email: string;
  birthDate: string;
  status: number | boolean | null;
  strikes: number;
}

// En tu admin.service.ts

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/admin'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/admin';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      return new HttpHeaders();
    }

    const user = JSON.parse(storedUser);
    const adminId = user?.id ? String(user.id) : '';

    return adminId
      ? new HttpHeaders({ 'X-Admin-Id': adminId })
      : new HttpHeaders();
  }

  getPatients(): Observable<Patient[]> {
    return this.http.get<Patient[]>(`${this.apiUrl}/patients`, {
      headers: this.getHeaders()
    });
  }

  getSpecialists(): Observable<Specialist[]> {
    return this.http.get<Specialist[]>(`${this.apiUrl}/specialists`, {
      headers: this.getHeaders()
    });
  }

  getPendingValidations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/pending-validations`, {
      headers: this.getHeaders()
    });
  }

  validateUser(id: number, status: 'approved' | 'rejected'): Observable<any> {
    return this.http.patch(`${this.apiUrl}/users/${id}/verify`, { status }, {
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
}