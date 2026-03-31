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
  status: number; // ⚠️ IMPORTANTE: 1 = activo | 0 = suspendido
  strikes: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private apiUrl = 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/admin';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    return new HttpHeaders({
      'X-Admin-Id': user.id?.toString() || ''
    });
  }

  // 🔹 PACIENTES
  getPatients(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/patients`, {
      headers: this.getHeaders()
    });
  }

  // 🔹 ESPECIALISTAS
  getSpecialists(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/specialists`, {
      headers: this.getHeaders()
    });
  }

  // 🔥 SUSPENDER (UNIFICADO)
  suspendUser(id: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/users/${id}/suspend`,
      {},
      { headers: this.getHeaders() }
    );
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
// 🔹 VALIDACIONES (Mezcla pacientes y especialistas pendientes)
  getPendingValidations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/pending-validations`, {
      headers: this.getHeaders()
    });
  }

  // 🔹 APROBAR O RECHAZAR VALIDACIÓN
  validateUser(id: number, status: 'approved' | 'rejected'): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/validations/${id}`,
      { status }, // El body con el nuevo estado
      { headers: this.getHeaders() }
    );
  }
}