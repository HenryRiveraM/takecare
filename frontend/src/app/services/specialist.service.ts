import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface SpecialistDirectoryItem {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  fullName?: string;
  email?: string;
  biography?: string;
  officeUbi?: string;
  sessionCost?: number;
  status?: number | boolean | null;
  accountVerified?: number | null;
  speciality?: string;
  specialties?: string[];
  shift?: string;
  reputationAverage?: number;
  certificationImg?: string;
}

export interface SpecialistLocationRequest {
  addressLine: string;
  city: string;
  neighborhood?: string;
  reference?: string;
  visibility?: 'public' | 'private';
}

export interface SpecialistLocationResponse {
  specialistId: number;
  addressLine: string;
  city: string;
  neighborhood: string;
  reference: string;
  visibility: 'public' | 'private' | null;
  visibilityPersisted: boolean;
  officeUbi: string;
}

@Injectable({
  providedIn: 'root'
})
export class SpecialistService {

  private readonly baseUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/specialists'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/specialists';

  constructor(private http: HttpClient) {}

  getAllSpecialists(): Observable<SpecialistDirectoryItem[]> {
    return this.http
      .get<Array<Partial<SpecialistDirectoryItem> & {
        fullName?: string;
        specialties?: string[];
        specialty?: string;
      }>>(this.baseUrl)
      .pipe(map((specialists) => specialists.map((specialist) => this.normalizeSpecialist(specialist))));
  }

  updateLocation(id: number, data: SpecialistLocationRequest): Observable<SpecialistLocationResponse> {
    return this.http.put<SpecialistLocationResponse>(`${this.baseUrl}/${id}/location`, data);
  }

  getProfile(id: number): Observable<any> {
    console.log(`Obteniendo perfil de especialista: ${this.baseUrl}/${id}/profile`);
    return this.http.get(`${this.baseUrl}/${id}/profile`);
  }

  updateProfile(id: number, data: any): Observable<any> {
    console.log(`Actualizando perfil de especialista: ${this.baseUrl}/${id}/profile`, data);
    return this.http.put(`${this.baseUrl}/${id}/profile`, data);
  }

  private normalizeSpecialist(
    specialist: Partial<SpecialistDirectoryItem> & {
      fullName?: string;
      specialties?: string[];
      specialty?: string;
    }
  ): SpecialistDirectoryItem {
    const fullName = specialist.fullName?.trim() || this.buildFullName(specialist);
    const nameParts = this.splitFullName(fullName);
    const specialties = specialist.specialties?.filter(Boolean) ?? this.normalizeSingleSpecialty(specialist);

    return {
      id: Number(specialist.id ?? 0),
      names: specialist.names?.trim() || nameParts.names,
      firstLastname: specialist.firstLastname?.trim() || nameParts.firstLastname,
      secondLastname: specialist.secondLastname?.trim() || nameParts.secondLastname,
      fullName,
      email: specialist.email?.trim(),
      biography: specialist.biography?.trim(),
      officeUbi: specialist.officeUbi?.trim(),
      sessionCost: specialist.sessionCost,
      status: specialist.status,
      accountVerified: specialist.accountVerified,
      speciality: specialist.speciality?.trim() || specialties[0] || '',
      specialties,
      shift: specialist.shift,
      reputationAverage: specialist.reputationAverage,
      certificationImg: specialist.certificationImg
    };
  }

  private buildFullName(specialist: Partial<SpecialistDirectoryItem>): string {
    return [
      specialist.names,
      specialist.firstLastname,
      specialist.secondLastname
    ]
      .filter(Boolean)
      .join(' ')
      .trim();
  }

  private splitFullName(fullName: string): { names: string; firstLastname: string; secondLastname: string } {
    const parts = fullName.split(/\s+/).filter(Boolean);

    if (parts.length === 0) {
      return { names: '', firstLastname: '', secondLastname: '' };
    }

    if (parts.length === 1) {
      return { names: parts[0], firstLastname: '', secondLastname: '' };
    }

    if (parts.length === 2) {
      return { names: parts[0], firstLastname: parts[1], secondLastname: '' };
    }

    return {
      names: parts.slice(0, parts.length - 2).join(' '),
      firstLastname: parts[parts.length - 2],
      secondLastname: parts[parts.length - 1]
    };
  }

  private normalizeSingleSpecialty(
    specialist: Partial<SpecialistDirectoryItem> & { specialty?: string }
  ): string[] {
    const specialty = specialist.speciality?.trim() || specialist.specialty?.trim();
    return specialty ? [specialty] : [];
  }
}
