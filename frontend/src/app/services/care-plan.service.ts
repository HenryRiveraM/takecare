import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type CarePlanStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
export type CarePlanItemType = 'OBJECTIVE' | 'ACTIVITY' | 'RECOMMENDATION';
export type CarePlanItemStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export interface CarePlanItemPayload {
  title: string;
  description?: string | null;
  itemType: CarePlanItemType;
  dueDate?: string | null;
}

export interface CreateCarePlanPayload {
  title: string;
  therapeuticObjectives: string;
  generalRecommendations: string;
  professionalObservations?: string | null;
  reviewDate: string;
  reviewStartTime: string;
  reviewEndTime: string;
  items: CarePlanItemPayload[];
}

export interface UpdateCarePlanPayload {
  title?: string;
  therapeuticObjectives?: string;
  generalRecommendations?: string;
  professionalObservations?: string | null;
  status?: CarePlanStatus;
  reviewDate?: string | null;
  reviewScheduleId?: number | null;
}

export interface UpdateCarePlanItemPayload {
  title?: string;
  description?: string | null;
  itemType?: CarePlanItemType;
  status?: CarePlanItemStatus;
  dueDate?: string | null;
}

export interface CarePlanItem {
  id: number;
  title: string;
  description?: string | null;
  itemType: CarePlanItemType;
  status: CarePlanItemStatus;
  dueDate: string | null;
  completedDate: string | null;
  createdDate?: string;
  updatedDate?: string | null;
}

export interface CarePlan {
  id: number;
  specialistId: number;
  patientId: number;
  patientName?: string | null;
  title: string;
  therapeuticObjectives?: string;
  generalRecommendations?: string;
  professionalObservations?: string | null;
  status: CarePlanStatus;
  progressPercentage: number;
  reviewSessionId?: number | null;
  reviewDate: string | null;
  reviewStartTime?: string | null;
  reviewEndTime?: string | null;
  createdDate: string;
  updatedDate?: string | null;
  items?: CarePlanItem[];
}

export interface CarePlanListResponse {
  totalCarePlans: number;
  carePlans: CarePlan[];
}

@Injectable({
  providedIn: 'root'
})
export class CarePlanService {

  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  createCarePlan(
    specialistId: number,
    patientId: number,
    payload: CreateCarePlanPayload
  ): Observable<CarePlan> {
    return this.http.post<CarePlan>(
      `${this.baseUrl}/api/v1/specialists/${specialistId}/patients/${patientId}/care-plans`,
      payload
    );
  }

  getCarePlansByPatient(specialistId: number, patientId: number): Observable<CarePlanListResponse> {
    return this.http.get<CarePlanListResponse>(
      `${this.baseUrl}/api/v1/specialists/${specialistId}/patients/${patientId}/care-plans`
    );
  }

  getCarePlansBySpecialist(specialistId: number): Observable<CarePlanListResponse> {
    return this.http.get<CarePlanListResponse>(
      `${this.baseUrl}/api/v1/specialists/${specialistId}/care-plans`
    );
  }

  getPatientCarePlans(patientId: number): Observable<CarePlanListResponse> {
    return this.http.get<CarePlanListResponse>(`${this.baseUrl}/api/v1/patients/${patientId}/care-plans`);
  }

  getCarePlanById(planId: number, specialistId?: number, patientId?: number): Observable<CarePlan> {
    let params = new HttpParams();
    if (specialistId) params = params.set('specialistId', specialistId);
    if (patientId) params = params.set('patientId', patientId);

    return this.http.get<CarePlan>(`${this.baseUrl}/api/v1/care-plans/${planId}`, { params });
  }

  updateCarePlan(
    planId: number,
    specialistId: number,
    payload: UpdateCarePlanPayload
  ): Observable<CarePlan> {
    const params = new HttpParams().set('specialistId', specialistId);
    return this.http.patch<CarePlan>(`${this.baseUrl}/api/v1/care-plans/${planId}`, payload, { params });
  }

  deleteCarePlan(planId: number, specialistId: number): Observable<void> {
    const params = new HttpParams().set('specialistId', specialistId);
    return this.http.delete<void>(`${this.baseUrl}/api/v1/care-plans/${planId}`, { params });
  }

  addCarePlanItem(
    planId: number,
    specialistId: number,
    payload: CarePlanItemPayload
  ): Observable<CarePlanItem> {
    const params = new HttpParams().set('specialistId', specialistId);
    return this.http.post<CarePlanItem>(`${this.baseUrl}/api/v1/care-plans/${planId}/items`, payload, { params });
  }

  updateCarePlanItem(
    itemId: number,
    specialistId: number,
    payload: UpdateCarePlanItemPayload
  ): Observable<CarePlanItem> {
    const params = new HttpParams().set('specialistId', specialistId);
    return this.http.patch<CarePlanItem>(`${this.baseUrl}/api/v1/care-plan-items/${itemId}`, payload, { params });
  }

  completeCarePlanItem(itemId: number, patientId: number): Observable<CarePlan> {
    const params = new HttpParams().set('patientId', patientId);
    return this.http.patch<CarePlan>(`${this.baseUrl}/api/v1/care-plan-items/${itemId}/complete`, {}, { params });
  }

  markCarePlanItemPending(itemId: number, patientId: number): Observable<CarePlan> {
    const params = new HttpParams().set('patientId', patientId);
    return this.http.patch<CarePlan>(`${this.baseUrl}/api/v1/care-plan-items/${itemId}/pending`, {}, { params });
  }
}
