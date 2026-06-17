import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  constructor(private http: HttpClient) {}

  getNotesByPlan(planId: number): Observable<LogbookNote[]> {
    return this.http.get<LogbookNote[]>(`${this.baseUrl}/api/v1/care-plans/${planId}/logbook`);
  }

  addNote(planId: number, payload: CreateLogbookNotePayload): Observable<LogbookNote> {
    return this.http.post<LogbookNote>(`${this.baseUrl}/api/v1/care-plans/${planId}/logbook`, payload);
  }
}