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
  status: number | boolean | null; // 1 = activo, 0 = suspendido
  strikes: number;
  accountVerified?: number; // 2 = pendiente, 1 = verificado, 0 = rechazado
}

export interface Specialist extends User {
  biography?: string;
  certificationImg?: string;
  officeUbi?: string;
  sessionCost?: number;
  reputationAverage?: number;
}
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

  // 🔹 PACIENTES
  getPatients(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/patients`, {
      headers: this.getHeaders()
    });
  }

  // 🔹 ESPECIALISTAS
  getSpecialists(): Observable<Specialist[]> {
    return this.http.get<Specialist[]>(`${this.apiUrl}/specialists`, {
      headers: this.getHeaders()
    });
  }

  // 🔹 VALIDACIONES - Obtiene especialistas PENDIENTES de validación
  getPendingValidations(): Observable<Specialist[]> {
    // El backend no tiene endpoint específico para pending, así que obtenemos todos los especialistas
    // y filtramos en el frontend aquellos con accountVerified === 2 (pendiente)
    return this.getSpecialists();
  }

  // 🔥 SUSPENDER USUARIO
  suspendUser(id: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/users/${id}/suspend`,
      {},
      { headers: this.getHeaders() }
    );
  }

  // 🔥 ELIMINAR PACIENTE
  deletePatient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/patients/${id}`, {
      headers: this.getHeaders()
    });
  }

  // 🔥 ELIMINAR ESPECIALISTA
  deleteSpecialist(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/specialists/${id}`, {
      headers: this.getHeaders()
    });
  }

  // 🔥 VALIDAR ESPECIALISTA - Aprobar
  approveSpecialist(id: number): Observable<any> {
    console.log(`✅ Aprobando especialista ${id}`);
    return this.http.put(`${this.apiUrl}/specialists/${id}/validate/approve`, {}, {
      headers: this.getHeaders()
    });
  }

  // 🔥 VALIDAR ESPECIALISTA - Rechazar
  rejectSpecialist(id: number): Observable<any> {
    console.log(`❌ Rechazando especialista ${id}`);
    return this.http.put(`${this.apiUrl}/specialists/${id}/validate/reject`, {}, {
      headers: this.getHeaders()
    });
  }

  // 🔥 VALIDAR USUARIO (método genérico para compatibilidad)
  validateUser(id: number, status: 'approved' | 'rejected'): Observable<any> {
    if (status === 'approved') {
      return this.approveSpecialist(id);
    } else {
      return this.rejectSpecialist(id);
    }
  }
}