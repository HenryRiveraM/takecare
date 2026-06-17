import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export type LogbookAuthorRole = 'SPECIALIST' | 'PATIENT';

export interface LogbookNote {
  id: number;
  planId: number;
  authorId: number;
  authorName: string;
  authorRole: LogbookAuthorRole;
  content: string;
  createdDate: string;
}

export interface CreateLogbookNotePayload {
  authorId: number;
  authorRole: LogbookAuthorRole;
  authorName: string;
  content: string;
}

@Injectable({
  providedIn: 'root'
})
export class LogbookService {

  private readonly baseUrl = environment.apiUrl;

  // 🔧 Almacén en memoria solo para demo mientras no exista el endpoint real.
  private mockNotesByPlan: Record<number, LogbookNote[]> = {};
  private mockIdCounter = 1000;

  constructor(private http: HttpClient) {}

  // TODO: reemplazar por la llamada real cuando el backend expon el endpoint de bitácora.
  // Endpoint esperado: GET /api/v1/care-plans/{planId}/logbook
  getNotesByPlan(planId: number): Observable<LogbookNote[]> {
    if (!this.mockNotesByPlan[planId]) {
      this.mockNotesByPlan[planId] = this.seedMockNotes(planId);
    }

    return of([...this.mockNotesByPlan[planId]]).pipe(delay(350));

    // return this.http.get<LogbookNote[]>(`${this.baseUrl}/api/v1/care-plans/${planId}/logbook`);
  }

  // TODO: reemplazar por la llamada real cuando el backend exponga el endpoint de bitácora.
  // Endpoint esperado: POST /api/v1/care-plans/{planId}/logbook
  addNote(planId: number, payload: CreateLogbookNotePayload): Observable<LogbookNote> {
    const newNote: LogbookNote = {
      id: this.mockIdCounter++,
      planId,
      authorId: payload.authorId,
      authorName: payload.authorName,
      authorRole: payload.authorRole,
      content: payload.content,
      createdDate: new Date().toISOString()
    };

    if (!this.mockNotesByPlan[planId]) {
      this.mockNotesByPlan[planId] = [];
    }
    this.mockNotesByPlan[planId].push(newNote);

    return of(newNote).pipe(delay(300));

    // return this.http.post<LogbookNote>(`${this.baseUrl}/api/v1/care-plans/${planId}/logbook`, payload);
  }

  private seedMockNotes(planId: number): LogbookNote[] {
    return [
      {
        id: this.mockIdCounter++,
        planId,
        authorId: 100,
        authorName: 'Dra. María Sánchez',
        authorRole: 'SPECIALIST',
        content: 'Recordatorio: seguir practicando la respiración 4-7-8 antes de dormir, como hablamos en la sesión.',
        createdDate: '2026-06-10T15:20:00'
      },
      {
        id: this.mockIdCounter++,
        planId,
        authorId: 1,
        authorName: 'Juan Pérez',
        authorRole: 'PATIENT',
        content: 'Lo intenté anoche y me ayudó a dormir más rápido. Sigo notando ansiedad por las mañanas.',
        createdDate: '2026-06-11T08:05:00'
      }
    ];
  }
}