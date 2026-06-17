import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface EmotionalSummary {
  averageMood: number;
  averageAnxiety: number;
  averageStress: number;
  totalRecords: number;
  rangeDays: number;
}

@Injectable({
  providedIn: 'root'
})
export class EmotionalRecordService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // TODO: reemplazar por la llamada real cuando el compañero termine el módulo de registro emocional.
  // Endpoint esperado: GET /api/v1/patients/{patientId}/emotional-records/summary?rangeDays=30
  getEmotionalSummary(patientId: number, rangeDays: number = 30): Observable<EmotionalSummary> {
    const mockSummary: EmotionalSummary = {
      averageMood: 6.4,
      averageAnxiety: 4.8,
      averageStress: 5.2,
      totalRecords: 12,
      rangeDays
    };

    return of(mockSummary).pipe(delay(400));

    // return this.http.get<EmotionalSummary>(
    //   `${this.baseUrl}/api/v1/patients/${patientId}/emotional-records/summary`,
    //   { params: { rangeDays } }
    // );
  }
}