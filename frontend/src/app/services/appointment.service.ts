import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CreateAppointmentRequest {
  patientId: number;
  scheduleId: number;
  typeOfSession: number;
}

export interface AppointmentResponse {
  id: number;
  patientId: number;
  scheduleId: number;
  specialistId: number;
  status: number;
  typeOfSession: number;
  createdDate: string;
  patientName: string;
  specialistName: string;
}

export interface AppointmentStatusResponse {
  sessionId: number;
  specialistId: number;
  patientId: number;
  scheduleId: number;
  status: number;
  scheduleStatus: number;
  updatedAt: string;
  notificationDescription: string;
}

export interface CancelAppointmentRequest {
  patientId: number;
}

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {

  private readonly apiUrl = `${environment.apiUrl}/api/v1/appointments`;

  constructor(private http: HttpClient) {}

  createAppointment(request: CreateAppointmentRequest): Observable<AppointmentResponse> {
    return this.http.post<AppointmentResponse>(this.apiUrl, request);
  }

  getAppointmentsByPatient(patientId: number): Observable<AppointmentResponse[]> {
    return this.http.get<AppointmentResponse[]>(`${this.apiUrl}/patient/${patientId}`);
  }

  getAppointmentsBySpecialist(specialistId: number): Observable<AppointmentResponse[]> {
    return this.http.get<AppointmentResponse[]>(`${this.apiUrl}/specialist/${specialistId}`);
  }

  cancelAppointment(appointmentId: number,request: CancelAppointmentRequest): Observable<AppointmentStatusResponse> {
    return this.http.patch<AppointmentStatusResponse>(
      `${this.apiUrl}/${appointmentId}/cancel`,
      request
    );
  }
}