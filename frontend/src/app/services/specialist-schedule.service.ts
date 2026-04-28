import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

import {
  SpecialistScheduleGroup,
  SpecialistScheduleRequest,
  SpecialistScheduleResponse
} from '../models/specialist-schedule.model';

@Injectable({
  providedIn: 'root'
})
export class SpecialistScheduleService {

  private readonly apiUrl = `${environment.apiUrl}/api/v1/schedules`;

  constructor(private http: HttpClient) {}

  createSchedule(
    specialistId: number,
    schedule: SpecialistScheduleRequest
  ): Observable<SpecialistScheduleResponse> {
    return this.http.post<SpecialistScheduleResponse>(
      `${this.apiUrl}/specialist/${specialistId}/create`,
      schedule
    );
  }

  getSchedulesByDateRange(
    specialistId: number,
    startDate: string,
    endDate: string
  ): Observable<SpecialistScheduleResponse[]> {
    return this.http.get<SpecialistScheduleResponse[]>(
      `${this.apiUrl}/specialist/${specialistId}/range`,
      {
        params: {
          startDate,
          endDate
        }
      }
    );
  }

  getAllSchedulesBySpecialist(
    specialistId: number
  ): Observable<SpecialistScheduleResponse[]> {
    return this.http.get<SpecialistScheduleResponse[]>(
      `${this.apiUrl}/specialist/${specialistId}/all`
    );
  }

  getSchedulesGroupedByDay(
    specialistId: number
  ): Observable<SpecialistScheduleGroup[]> {
    return this.http.get<SpecialistScheduleGroup[]>(
      `${this.apiUrl}/specialist/${specialistId}/grouped`
    );
  }

  updateSchedule(
    scheduleId: number,
    schedule: SpecialistScheduleRequest
  ): Observable<SpecialistScheduleResponse> {
    return this.http.put<SpecialistScheduleResponse>(
      `${this.apiUrl}/${scheduleId}`,
      schedule
    );
  }

  deleteSchedule(scheduleId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${scheduleId}`);
  }
}