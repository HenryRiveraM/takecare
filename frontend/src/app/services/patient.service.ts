import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ===== INTERFACES =====
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

export interface UpdatePatientProfile {
  names?: string;
  firstLastname?: string;
  secondLastname?: string;
  birthDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PatientService {

  private readonly baseUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/users'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/users';

  constructor(private http: HttpClient) {}

  getProfile(): Observable<PatientProfile> {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const userId = user?.id;
    return this.http.get<PatientProfile>(
      `${this.baseUrl}/profile/${userId}`
    );
  }

  updateProfile(data: UpdatePatientProfile): Observable<PatientProfile> {
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    const userId = user?.id;
    return this.http.put<PatientProfile>(
      `${this.baseUrl}/profile/${userId}`,
      data
    );
  }
}