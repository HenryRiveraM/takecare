import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SpecialistDirectoryItem {
  id: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  email: string;
  biography?: string;
  officeUbi?: string;
  sessionCost?: number;
  status?: number | boolean | null;
  accountVerified?: number | null;
  speciality?: string;
  shift?: string;
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
    return this.http.get<SpecialistDirectoryItem[]>(this.baseUrl);
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
}
