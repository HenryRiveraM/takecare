import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

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

  private readonly apiUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/admin'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/admin';

  private readonly specialistsFallbackUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/specialists'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/specialists';

  private readonly specialistProfilesBaseUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/specialists'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/specialists';

  constructor(private http: HttpClient) {}

  private normalizeUser(rawUser: any): User {
    const source = rawUser?.user ?? rawUser ?? {};

    return {
      id: source.id ?? rawUser?.id ?? 0,
      names: source.names ?? source.name ?? source.fullName ?? '',
      firstLastname: source.firstLastname ?? source.first_lastname ?? source.lastname ?? '',
      secondLastname: source.secondLastname ?? source.second_lastname ?? '',
      email: source.email ?? '',
      birthDate: source.birthDate ?? source.birth_date ?? '',
      status: source.status ?? rawUser?.status ?? null,
      strikes: source.strikes ?? rawUser?.strikes ?? 0,
      accountVerified: source.accountVerified ?? source.account_verified ?? rawUser?.accountVerified
    };
  }

  private normalizeSpecialist(rawSpecialist: any): Specialist {
    const normalizedUser = this.normalizeUser(rawSpecialist);

    return {
      ...normalizedUser,
      biography: rawSpecialist?.biography ?? rawSpecialist?.user?.biography ?? '',
      certificationImg: rawSpecialist?.certificationImg ?? rawSpecialist?.certification_img,
      officeUbi: rawSpecialist?.officeUbi ?? rawSpecialist?.office_ubi ?? '',
      sessionCost: rawSpecialist?.sessionCost ?? rawSpecialist?.session_cost,
      reputationAverage: rawSpecialist?.reputationAverage ?? rawSpecialist?.reputation_average
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

  private suspendResource(id: number): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/users/${id}/suspend`,
      {},
      { headers: this.getHeaders() }
    );
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

  // 🔹 PACIENTES
  getPatients(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}/patients`, {
      headers: this.getHeaders()
    }).pipe(
      map((patients) => patients.map((patient) => this.normalizeUser(patient)))
    );
  }

  // 🔹 ESPECIALISTAS
  getSpecialists(): Observable<Specialist[]> {
    return this.http.get<Specialist[]>(`${this.apiUrl}/specialists`, {
      headers: this.getHeaders()
    }).pipe(
      map((specialists) => specialists.map((specialist) => this.normalizeSpecialist(specialist))),
      switchMap((specialists) => this.enrichSpecialistsWithProfiles(specialists)),
      catchError((error) => {
        console.warn('Fallo el endpoint admin de especialistas, usando endpoint público como respaldo.', error);
        return this.http.get<Specialist[]>(this.specialistsFallbackUrl).pipe(
          map((specialists) => specialists.map((specialist) => this.normalizeSpecialist(specialist))),
          switchMap((specialists) => this.enrichSpecialistsWithProfiles(specialists))
        );
      })
    );
  }

  // 🔹 VALIDACIONES - Obtiene especialistas PENDIENTES de validación
  getPendingValidations(): Observable<Specialist[]> {
    // El backend no tiene endpoint específico para pending, así que obtenemos todos los especialistas
    // y filtramos en el frontend aquellos con accountVerified === 2 (pendiente)
    return this.getSpecialists();
  }

  // 🔥 SUSPENDER USUARIO
  suspendUser(id: number): Observable<any> {
    return this.suspendResource(id);
  }

  suspendPatient(id: number): Observable<any> {
    return this.suspendResource(id);
  }

  suspendSpecialist(id: number): Observable<any> {
    return this.suspendResource(id);
  }

  // 🔥 ELIMINAR PACIENTE
  deletePatient(id: number): Observable<void> {
    return this.suspendPatient(id);
  }

  // 🔥 ELIMINAR ESPECIALISTA
  deleteSpecialist(id: number): Observable<void> {
    return this.suspendSpecialist(id);
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
