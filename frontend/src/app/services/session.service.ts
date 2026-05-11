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

export interface SessionRating {
  sessionId: number;
  specialistId: number;
  patientName: string;
  stars: number;
  comment: string;
  createdAt: string;
  updatedAt: string;
}

export interface SessionReport {
  sessionId: number;
  specialistId: number;
  patientName: string;
  reason: string;
  details: string;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface CreateCalificationRequest {
  rating: number;
  comment?: string;
}

export interface CalificationResponse {
  id: number;
  sessionId: number;
  patientId: number;
  specialistId: number;
  rating: number;
  comment: string | null;
  createdDate: string;
  evaluatorRole: string;
}

export interface CreateReportRequest {
  specialistId: number;
  sessionId: number;
  reason: string;
  description?: string;
}

// Nuevo: usado para POST /api/v1/reports/patient
export interface CreatePatientReportRequest {
  patientId: number;
  sessionId: number;
  reason: string;
  description?: string;
}

export interface ReportResponse {
  id: number;
  sessionId: number;
  reporterUserId: number;
  reportedUserId: number;
  reason: string;
  description: string | null;
  status: string;
  createdDate: string | null;
  updatedDate: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class SessionService {

  private readonly apiUrl          = `${environment.apiUrl}/api/v1/sessions`;
  private readonly reportsApiUrl   = `${environment.apiUrl}/api/v1/reports`;
  private readonly specialistsUrl  = `${environment.apiUrl}/api/v1/specialists`;
  private readonly patientsUrl     = `${environment.apiUrl}/api/v1/patients`;

  // localStorage solo se mantiene para ratings (flujo antiguo sin backend propio)
  private readonly ratingsStorageKey = 'specialist_session_ratings';

  constructor(private http: HttpClient) {}

  // ── Sesiones ────────────────────────────────────────────────────────────────

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

  // ── Calificaciones ──────────────────────────────────────────────────────────

  createPatientRating(
    sessionId: number,
    request: CreateCalificationRequest
  ): Observable<CalificationResponse> {
    return this.http.post<CalificationResponse>(
      `${this.apiUrl}/${sessionId}/patient-ratings`,
      request
    );
  }

  getPatientRating(sessionId: number): Observable<CalificationResponse> {
    return this.http.get<CalificationResponse>(
      `${this.apiUrl}/${sessionId}/patient-ratings`
    );
  }

  // ── Reportes: especialista reporta paciente (ya existía) ────────────────────

  /** POST /api/v1/reports — el especialista reporta a un paciente */
  createReport(request: CreateReportRequest): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(this.reportsApiUrl, request);
  }

  /** GET /api/v1/reports/session/{sessionId}/specialist/{specialistId} */
  getReportBySession(sessionId: number, specialistId: number): Observable<ReportResponse> {
    return this.http.get<ReportResponse>(
      `${this.reportsApiUrl}/session/${sessionId}/specialist/${specialistId}`
    );
  }

  // ── Reportes: paciente reporta especialista (nuevos) ────────────────────────

  /** POST /api/v1/reports/patient — el paciente reporta a un especialista */
  createPatientReport(request: CreatePatientReportRequest): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.reportsApiUrl}/patient`, request);
  }

  /** GET /api/v1/reports/session/{sessionId}/patient/{patientId} */
  getPatientReportBySession(sessionId: number, patientId: number): Observable<ReportResponse> {
    return this.http.get<ReportResponse>(
      `${this.reportsApiUrl}/session/${sessionId}/patient/${patientId}`
    );
  }

  /** GET /api/v1/patients/{patientId}/reports — reportes recibidos por el paciente */
  getReportsReceivedByPatient(patientId: number): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${this.patientsUrl}/${patientId}/reports`);
  }

  /** GET /api/v1/specialists/{specialistId}/reports — reportes recibidos por el especialista */
  getReportsReceivedBySpecialist(specialistId: number): Observable<ReportResponse[]> {
    return this.http.get<ReportResponse[]>(`${this.specialistsUrl}/${specialistId}/reports`);
  }

  // ── localStorage: ratings (se conserva el flujo existente) ─────────────────

  getRatingsBySpecialist(specialistId: number): SessionRating[] {
    return this.readRatings().filter(r => r.specialistId === specialistId);
  }

  getRatingBySession(sessionId: number, specialistId: number): SessionRating | null {
    return this.readRatings().find(
      r => r.sessionId === sessionId && r.specialistId === specialistId
    ) ?? null;
  }

  saveRating(rating: Omit<SessionRating, 'createdAt' | 'updatedAt'>): SessionRating {
    const ratings = this.readRatings();
    const now = new Date().toISOString();
    const idx = ratings.findIndex(
      r => r.sessionId === rating.sessionId && r.specialistId === rating.specialistId
    );

    const next: SessionRating = idx >= 0
      ? { ...ratings[idx], ...rating, updatedAt: now }
      : { ...rating, createdAt: now, updatedAt: now };

    if (idx >= 0) { ratings[idx] = next; } else { ratings.push(next); }
    localStorage.setItem(this.ratingsStorageKey, JSON.stringify(ratings));
    return next;
  }

  private readRatings(): SessionRating[] {
    try {
      const parsed = JSON.parse(localStorage.getItem(this.ratingsStorageKey) ?? '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch { return []; }
  }
}
