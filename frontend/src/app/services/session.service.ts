import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CreateSessionRequest {
  patientId: number;
  scheduleId: number;
  typeOfSession: number;
}

export interface SessionResponse {
  id: number;
  patientId: number;
  scheduleId: number;
  specialistId: number;
  status: number;
  typeOfSession: number;
  createdDate: string;
  patientName: string;
  specialistName: string;

  patientEmail?: string;
  scheduleDate?: string;
  startTime?: string;
  endTime?: string;
}

export interface SessionStatusResponse {
  sessionId: number;
  specialistId: number;
  patientId: number;
  scheduleId: number;
  status: number;
  scheduleStatus: number;
  updatedAt: string;
  notificationDescription: string;
}

export interface CancelSessionRequest {
  patientId: number;
}

export interface UpdateSessionStatusRequest {
  specialistId: number;
  action: 'accept' | 'reject';
}

@Injectable({
  providedIn: 'root'
})
export class SessionService {

  private readonly apiUrl = `${environment.apiUrl}/api/v1/sessions`;

  constructor(private http: HttpClient) {}

  createSession(request: CreateSessionRequest): Observable<SessionResponse> {
    return this.http.post<SessionResponse>(this.apiUrl, request);
  }

  getSessionsByPatient(patientId: number): Observable<SessionResponse[]> {
    return this.http.get<SessionResponse[]>(`${this.apiUrl}/patient/${patientId}`);
  }

  getSessionsBySpecialist(specialistId: number): Observable<SessionResponse[]> {
    return this.http.get<SessionResponse[]>(`${this.apiUrl}/specialist/${specialistId}`);
  }

  updateSessionStatus(
    sessionId: number,
    request: UpdateSessionStatusRequest
  ): Observable<SessionStatusResponse> {
    return this.http.patch<SessionStatusResponse>(
      `${this.apiUrl}/${sessionId}/status`,
      request
    );
  }

  cancelSession(
    sessionId: number,
    request: CancelSessionRequest
  ): Observable<SessionStatusResponse> {
    return this.http.patch<SessionStatusResponse>(
      `${this.apiUrl}/${sessionId}/cancel`,
      request
    );
  }
}