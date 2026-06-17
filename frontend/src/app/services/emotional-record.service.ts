import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

export type EmotionalMood = 'VERY_GOOD' | 'GOOD' | 'NEUTRAL' | 'SAD' | 'ANXIOUS';

export interface EmotionalRecordRequest {
  mood: EmotionalMood;
  energyLevel: number;
  anxietyLevel: number;
  sleepQuality: number;
  notes?: string;
}

export interface EmotionalRecord extends EmotionalRecordRequest {
  id?: number;
  patientId?: number;
  recordDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

type EmotionalRecordApiResponse = EmotionalRecord[] | { data?: EmotionalRecord[] | EmotionalRecord | null };
type EmotionalRecordSaveResponse = EmotionalRecord | { data?: EmotionalRecord | null };

@Injectable({
  providedIn: 'root'
})
export class EmotionalRecordService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getRecords(patientId: number): Observable<EmotionalRecord[]> {
    return this.http
      .get<EmotionalRecordApiResponse>(`${this.baseUrl}/api/v1/patients/${patientId}/emotional-records`)
      .pipe(
        map(response => {
          if (Array.isArray(response)) {
            return response;
          }

          if (Array.isArray(response?.data)) {
            return response.data;
          }

          return [];
        })
      );
  }

  createRecord(patientId: number, payload: EmotionalRecordRequest): Observable<EmotionalRecord> {
    return this.http
      .post<EmotionalRecordSaveResponse>(`${this.baseUrl}/api/v1/patients/${patientId}/emotional-records`, payload)
      .pipe(
        map(response => {
          if ('data' in Object(response)) {
            return (response as { data?: EmotionalRecord | null }).data || payload;
          }

          return response as EmotionalRecord;
        })
      );
  }
}
