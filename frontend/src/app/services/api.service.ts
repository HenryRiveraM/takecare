import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PatientProfile {
  id?: number;
  names: string;
  firstLastname: string;
  secondLastname?: string;
  birthDate: string;
  ciNumber: string;
  email: string;
  clinicalHistory?: string;
  accountVerified?: number;
}

export interface SpecialistRegisterRequest {
  names: string;
  firstLastname: string;
  secondLastname?: string;
  birthDate: string;
  ciNumber: string;
  email: string;
  password: string;
  biography: string;
  certificationImg: string;
  officeUbi?: string;
  sessionCost: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private readonly baseUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app';

  constructor(private http: HttpClient) {}

  registerPatient(data: PatientProfile): Observable<ApiResponse<PatientProfile>> {
    return this.http.post<ApiResponse<PatientProfile>>(
      `${this.baseUrl}/api/v1/users/register/patient`,
      data
    );
  }

  registerSpecialist(data: SpecialistRegisterRequest): Observable<string> {
    return this.http.post(
      `${this.baseUrl}/api/v1/users/register/specialist`,
      data,
      { responseType: 'text' }
    );
  }

  getPatientProfile(): Observable<PatientProfile> {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const userId = user?.id;
    console.log(`API: Obteniendo perfil de paciente. UserId: ${userId}`);
    return this.http.get<PatientProfile>(
      `${this.baseUrl}/api/v1/users/profile/${userId}`
    );
  }

  updatePatientProfile(data: PatientProfile): Observable<PatientProfile> {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const userId = user?.id;
    console.log(`API: Actualizando perfil de paciente. UserId: ${userId}`);
    return this.http.put<PatientProfile>(
      `${this.baseUrl}/api/v1/users/profile/${userId}`,
      data
    );
  }
}