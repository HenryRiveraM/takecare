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

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private baseUrl = 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/';

  constructor(private http: HttpClient) {}

  registerPatient(data: PatientProfile): Observable<ApiResponse<PatientProfile>> {
    return this.http.post<ApiResponse<PatientProfile>>(
      `${this.baseUrl}api/v1/users/register/patient`,
      data
    );
  }

  getPatientProfile(): Observable<PatientProfile> {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const userId = user?.id;
    return this.http.get<PatientProfile>(
      `${this.baseUrl}api/v1/users/profile/${userId}`
    );
  }

  updatePatientProfile(data: PatientProfile): Observable<PatientProfile> {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const userId = user?.id;
    return this.http.put<PatientProfile>(
      `${this.baseUrl}api/v1/users/profile/${userId}`,
      data
    );
  }
}