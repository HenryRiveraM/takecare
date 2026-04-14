import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable, forkJoin, of, map } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

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

interface SpecialistProfileResponse {
  id: number;
  names: string;
  firstLastname?: string;
  secondLastname?: string;
  officeUbi?: string;
  sessionCost?: number;
  email?: string;
  biography?: string;
}

interface SpecialistDetailsResponse {
  id: number;
  birthDate?: string;
  birth_date?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = environment.apiUrl;
  private readonly specialistProfilesBaseUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/specialists'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/specialists';

  constructor(private http: HttpClient) {}

  private normalizeUser(raw: any): User {
  return {
    id: raw.id,
    names: raw.names ?? '',
    firstLastname: raw.firstLastname ?? '',
    secondLastname: raw.secondLastname ?? '',
    email: raw.email ?? '',
    birthDate: raw.birthDate ?? '',
    ciNumber: raw.ciNumber ?? '',
    ciDocumentImg: raw.ciDocumentImg ?? '',
    status: raw.status ?? 1,
    strikes: raw.strikes ?? 0,
    accountVerified: raw.accountVerified ?? 2,
    role: raw.role
  };
}

private normalizePatient(raw: any): Patient {
  return {
    ...this.normalizeUser(raw),
    clinicalHistory: raw.clinicalHistory ?? '',
    selfieVerification: raw.selfieVerification ?? '',
    role: 1 as const
  };
}

private normalizeSpecialist(raw: any): Specialist {
  return {
    ...this.normalizeUser(raw),
    biography: raw.biography ?? '',
    certificationImg: raw.certificationImg ?? '',
    officeUbi: raw.officeUbi ?? '',
    sessionCost: raw.sessionCost ?? 0,
    reputationAverage: raw.reputationAverage ?? 0,
    role: 2 as const
  };
}

  private enrichSpecialistsWithProfiles(specialists: Specialist[]): Observable<Specialist[]> {
    if (specialists.length === 0) {
      return of([]);
    }

    const requests = specialists.map((specialist) =>
      forkJoin({
        profile: this.http.get<SpecialistProfileResponse>(`${this.specialistProfilesBaseUrl}/${specialist.id}/profile`).pipe(
          catchError(() => of(null))
        ),
        details: this.http.get<SpecialistDetailsResponse>(`${this.specialistProfilesBaseUrl}/${specialist.id}`).pipe(
          catchError(() => of(null))
        )
      }).pipe(
        map(({ profile, details }) => ({
          ...specialist,
          names: profile?.names || specialist.names,
          firstLastname: profile?.firstLastname || specialist.firstLastname,
          secondLastname: profile?.secondLastname || specialist.secondLastname,
          email: profile?.email || specialist.email,
          biography: profile?.biography || specialist.biography,
          officeUbi: profile?.officeUbi || specialist.officeUbi,
          sessionCost: profile?.sessionCost ?? specialist.sessionCost,
          birthDate: details?.birthDate || details?.birth_date || specialist.birthDate
        }))
      )
    );

    return forkJoin(requests);
  }

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
