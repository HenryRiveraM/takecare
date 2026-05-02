import { Injectable } from '@angular/core';

export interface SessionRating {
  appointmentId: string;
  specialistId: number;
  patientName: string;
  stars: number;
  comment: string;
  createdAt: string;
  updatedAt: string;
}

export interface SessionReport {
  appointmentId: string;
  specialistId: number;
  patientName: string;
  reason: string;
  details: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  private readonly ratingsStorageKey = 'specialist_session_ratings';
  private readonly reportsStorageKey = 'specialist_session_reports';

  getRatingsBySpecialist(specialistId: number): SessionRating[] {
    return this.readRatings().filter((rating) => rating.specialistId === specialistId);
  }

  getRatingByAppointment(appointmentId: string, specialistId: number): SessionRating | null {
    return this.readRatings().find(
      (rating) => rating.appointmentId === appointmentId && rating.specialistId === specialistId
    ) ?? null;
  }

  saveRating(rating: Omit<SessionRating, 'createdAt' | 'updatedAt'>): SessionRating {
    const ratings = this.readRatings();
    const now = new Date().toISOString();
    const existingIndex = ratings.findIndex(
      (item) => item.appointmentId === rating.appointmentId && item.specialistId === rating.specialistId
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

  getReportsBySpecialist(specialistId: number): SessionReport[] {
    return this.readReports().filter((report) => report.specialistId === specialistId);
  }

  getReportByAppointment(appointmentId: string, specialistId: number): SessionReport | null {
    return this.readReports().find(
      (report) => report.appointmentId === appointmentId && report.specialistId === specialistId
    ) ?? null;
  }

  saveReport(report: Omit<SessionReport, 'createdAt' | 'updatedAt'>): SessionReport {
    const reports = this.readReports();
    const now = new Date().toISOString();
    const existingIndex = reports.findIndex(
      (item) => item.appointmentId === report.appointmentId && item.specialistId === report.specialistId
    );

    const nextReport: SessionReport = existingIndex >= 0
      ? {
          ...reports[existingIndex],
          ...report,
          updatedAt: now
        }
      : {
          ...report,
          createdAt: now,
          updatedAt: now
        };

    if (existingIndex >= 0) {
      reports[existingIndex] = nextReport;
    } else {
      reports.push(nextReport);
    }

    localStorage.setItem(this.reportsStorageKey, JSON.stringify(reports));
    return nextReport;
  }

  private readRatings(): SessionRating[] {
    const raw = localStorage.getItem(this.ratingsStorageKey);
    return this.safeParseArray<SessionRating>(raw);
  }

  private readReports(): SessionReport[] {
    const raw = localStorage.getItem(this.reportsStorageKey);
    return this.safeParseArray<SessionReport>(raw);
  }

  private safeParseArray<T>(raw: string | null): T[] {
    if (!raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw) as T[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}
