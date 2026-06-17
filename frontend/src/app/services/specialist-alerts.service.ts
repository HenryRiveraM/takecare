import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

export type AlertPriority = 'HIGH' | 'MEDIUM' | 'LOW';

export interface SpecialistAlert {
  id: number;
  patientId?: number;
  patientName: string;
  priority: AlertPriority;
  title: string;
  message: string;
  recommendation?: string;
  source?: string;
  detectedAt?: string;
  createdAt?: string;
  reviewed?: boolean;
  reviewedAt?: string | null;
}

type SpecialistAlertsApiResponse = SpecialistAlert[] | { data?: SpecialistAlert[] | null };

@Injectable({
  providedIn: 'root'
})
export class SpecialistAlertsService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAlerts(specialistId: number): Observable<SpecialistAlert[]> {
    return this.http
      .get<SpecialistAlertsApiResponse>(`${this.baseUrl}/api/v1/specialists/${specialistId}/patient-alerts`)
      .pipe(
        map(response => {
          const alerts = Array.isArray(response) ? response : response?.data || [];
          return alerts.map(alert => this.normalizeAlert(alert));
        })
      );
  }

  markAsReviewed(specialistId: number, alertId: number): Observable<SpecialistAlert> {
    return this.http
      .patch<SpecialistAlert>(
        `${this.baseUrl}/api/v1/specialists/${specialistId}/patient-alerts/${alertId}/reviewed`,
        {}
      )
      .pipe(map(alert => this.normalizeAlert(alert)));
  }

  private normalizeAlert(alert: SpecialistAlert): SpecialistAlert {
    return {
      ...alert,
      priority: this.normalizePriority(alert.priority),
      reviewed: Boolean(alert.reviewed)
    };
  }

  private normalizePriority(priority: string | undefined): AlertPriority {
    const normalized = String(priority || '').toUpperCase();

    if (normalized === 'ALTA' || normalized === 'HIGH' || normalized === 'CRITICAL') {
      return 'HIGH';
    }

    if (normalized === 'MEDIA' || normalized === 'MEDIUM') {
      return 'MEDIUM';
    }

    return 'LOW';
  }
}
