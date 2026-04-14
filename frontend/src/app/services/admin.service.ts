import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { forkJoin, map, Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface User {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  email: string;
  birthDate: string;
  ciNumber: string;
  ciDocumentImg?: string;
  status: number | boolean | null;
  strikes: number;
  accountVerified?: number;
  role?: number; // 1 = paciente, 2 = especialista, 3 = admin
}

export interface Patient extends User {
  clinicalHistory?: string;
  selfieVerification?: string;
  role: 1;
}

export interface Specialist extends User {
  biography?: string;
  certificationImg?: string;
  officeUbi?: string;
  sessionCost?: number;
  reputationAverage?: number;
  role: 2;
}

export interface PendingValidationUser extends User {
  role: 1 | 2;
  certificationImg?: string;
  selfieVerification?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = environment.apiUrl;

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
    return this.http.get<User[]>(`${this.apiUrl}/api/v1/admin/patients`, {
      headers: this.getHeaders()
    }).pipe(
      map(users => users.map(u => ({ ...u, role: 1 as const })))
    );
  }

  getSpecialists(): Observable<Specialist[]> {
    return this.http.get<Specialist[]>(`${this.apiUrl}/api/v1/admin/specialists`, {
      headers: this.getHeaders()
    }).pipe(
      map(users => users.map(u => ({ ...u, role: 2 as const })))
    );
  }

  getPendingValidations(): Observable<PendingValidationUser[]> {
    return forkJoin({
      patients: this.getPatients(),
      specialists: this.getSpecialists()
    }).pipe(
      map(({ patients, specialists }) => {
        const pendingPatients: PendingValidationUser[] = patients
          .filter(user => user.accountVerified === 2)
          .map(user => ({ ...user, role: 1 as const }));

        const pendingSpecialists: PendingValidationUser[] = specialists
          .filter(user => user.accountVerified === 2)
          .map(user => ({ ...user, role: 2 as const }));

        return [...pendingPatients, ...pendingSpecialists];
      })
    );
  }

  updateUserStatus(id: number, status: 0 | 1): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/api/v1/admin/users/${id}/status`,
      { status },
      { headers: this.getHeaders() }
    );
  }

  deletePatient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/v1/admin/patients/${id}`, {
      headers: this.getHeaders()
    });
  }

  deleteSpecialist(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/v1/admin/specialists/${id}`, {
      headers: this.getHeaders()
    });
  }

  approvePatient(id: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/api/v1/admin/patients/${id}/validate/approve`,
      {},
      { headers: this.getHeaders() }
    );
  }

  rejectPatient(id: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/api/v1/admin/patients/${id}/validate/reject`,
      {},
      { headers: this.getHeaders() }
    );
  }

  approveSpecialist(id: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/api/v1/admin/specialists/${id}/validate/approve`,
      {},
      { headers: this.getHeaders() }
    );
  }

  rejectSpecialist(id: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/api/v1/admin/specialists/${id}/validate/reject`,
      {},
      { headers: this.getHeaders() }
    );
  }

  validateUser(id: number, role: 1 | 2, status: 'approved' | 'rejected'): Observable<any> {
    if (role === 1) {
      return status === 'approved'
        ? this.approvePatient(id)
        : this.rejectPatient(id);
    }

    return status === 'approved'
      ? this.approveSpecialist(id)
      : this.rejectSpecialist(id);
  }
}