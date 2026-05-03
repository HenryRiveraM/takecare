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

@Injectable({
  providedIn: 'root'
})
export class SessionService {

  private readonly apiUrl = `${environment.apiUrl}/api/v1/sessions`;
  private readonly ratingsStorageKey = 'specialist_session_ratings';

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

  getRatingsBySpecialist(specialistId: number): SessionRating[] {
    return this.readRatings().filter((rating) => rating.specialistId === specialistId);
  }

  getRatingBySession(sessionId: number, specialistId: number): SessionRating | null {
    return this.readRatings().find(
      (rating) => rating.sessionId === sessionId && rating.specialistId === specialistId
    ) ?? null;
  }

  saveRating(rating: Omit<SessionRating, 'createdAt' | 'updatedAt'>): SessionRating {
    const ratings = this.readRatings();
    const now = new Date().toISOString();
    const existingIndex = ratings.findIndex(
      (item) => item.sessionId === rating.sessionId && item.specialistId === rating.specialistId
    );

    const nextRating: SessionRating = existingIndex >= 0
      ? {
          ...ratings[existingIndex],
          ...rating,
          updatedAt: now
        }
      : {
          ...rating,
          createdAt: now,
          updatedAt: now
        };

    if (existingIndex >= 0) {
      ratings[existingIndex] = nextRating;
    } else {
      ratings.push(nextRating);
    }

    localStorage.setItem(this.ratingsStorageKey, JSON.stringify(ratings));
    return nextRating;
  }

  private readRatings(): SessionRating[] {
    const raw = localStorage.getItem(this.ratingsStorageKey);

    if (!raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw) as SessionRating[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}
