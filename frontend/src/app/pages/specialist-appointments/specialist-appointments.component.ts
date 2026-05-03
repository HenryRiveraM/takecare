import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { AuthService } from '../../services/auth.service';
import {
  SessionResponse,
  SessionReport,
  SessionRating,
  SessionService
} from '../../services/session.service';

export type AppointmentStatus = 'pending' | 'accepted' | 'rejected' | 'cancelled' | 'finished';
export type DialogAction = 'accept' | 'reject';

export interface Appointment {
  id: number;
  patientName: string;
  patientEmail: string;
  date: Date;
  time: string;
  endTime?: string;
  sessionTypeLabel: string;
  status: AppointmentStatus;
  loading?: boolean;
}

interface ConfirmDialog {
  visible: boolean;
  action: DialogAction;
  appointment: Appointment | null;
}

interface RatingDialog {
  visible: boolean;
  appointment: Appointment | null;
  stars: number;
  comment: string;
  saving: boolean;
  error: string;
}

interface ReportDialog {
  visible: boolean;
  appointment: Appointment | null;
  reason: string;
  details: string;
  saving: boolean;
  error: string;
}

@Component({
  selector: 'app-specialist-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './specialist-appointments.component.html',
  styleUrls: ['./specialist-appointments.component.css']
})
export class SpecialistAppointmentsComponent implements OnInit {

  loading = false;
  errorMsg = '';
  activeFilter: AppointmentStatus = 'pending';

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  specialistId!: number;
  ratingsBySession: Record<number, SessionRating> = {};
  reportsBySession: Record<number, SessionReport> = {};

  confirmDialog: ConfirmDialog = {
    visible: false,
    action: 'accept',
    appointment: null
  };

  ratingDialog: RatingDialog = {
    visible: false,
    appointment: null,
    stars: 0,
    comment: '',
    saving: false,
    error: ''
  };

  reportDialog: ReportDialog = {
    visible: false,
    appointment: null,
    reason: '',
    details: '',
    saving: false,
    error: ''
  };

  appointments: Appointment[] = [];

  constructor(
    public sidebarService: SidebarService,
    private sessionService: SessionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.specialistId = this.getLoggedSpecialistId();

    if (!this.specialistId) {
      this.errorMsg = 'appointments.error.noSpecialistId';
      return;
    }

    this.loadRatings();
    this.loadReports();
    this.loadAppointments();
  }

  get filteredAppointments(): Appointment[] {
    return this.appointments.filter(apt => apt.status === this.activeFilter);
  }

  get pendingCount(): number {
    return this.appointments.filter(apt => apt.status === 'pending').length;
  }

  setFilter(filter: AppointmentStatus): void {
    this.activeFilter = filter;
  }

  openConfirmDialog(appointment: Appointment, action: DialogAction): void {
    this.confirmDialog = {
      visible: true,
      action,
      appointment
    };
  }

  closeConfirmDialog(): void {
    this.confirmDialog = {
      visible: false,
      action: 'accept',
      appointment: null
    };
  }

  openRatingDialog(appointment: Appointment): void {
    const existingRating = this.ratingsBySession[appointment.id];

    this.ratingDialog = {
      visible: true,
      appointment,
      stars: existingRating?.stars ?? 0,
      comment: existingRating?.comment ?? '',
      saving: false,
      error: ''
    };
  }

  closeRatingDialog(): void {
    this.ratingDialog = {
      visible: false,
      appointment: null,
      stars: 0,
      comment: '',
      saving: false,
      error: ''
    };
  }

  openReportDialog(appointment: Appointment): void {
    const existingReport = this.reportsBySession[appointment.id];

    this.reportDialog = {
      visible: true,
      appointment,
      reason: existingReport?.reason ?? '',
      details: existingReport?.details ?? '',
      saving: false,
      error: ''
    };
  }

  closeReportDialog(): void {
    this.reportDialog = {
      visible: false,
      appointment: null,
      reason: '',
      details: '',
      saving: false,
      error: ''
    };
  }

  setRatingStars(stars: number): void {
    this.ratingDialog.stars = stars;
    this.ratingDialog.error = '';
  }

  saveRating(): void {
    if (!this.ratingDialog.appointment) {
      return;
    }

    if (this.ratingDialog.stars < 1) {
      this.ratingDialog.error = 'appointments.rating.errors.starsRequired';
      return;
    }

    if (this.ratingDialog.comment.trim().length < 5) {
      this.ratingDialog.error = 'appointments.rating.errors.commentRequired';
      return;
    }

    this.ratingDialog.saving = true;

    const appointment = this.ratingDialog.appointment;
    const savedRating = this.sessionService.saveRating({
      sessionId: appointment.id,
      specialistId: this.specialistId,
      patientName: appointment.patientName,
      stars: this.ratingDialog.stars,
      comment: this.ratingDialog.comment.trim()
    });

    this.ratingsBySession[appointment.id] = savedRating;
    this.ratingDialog.saving = false;
    this.closeRatingDialog();
    this.showToastMessage('appointments.rating.toast.saved', 'success');
  }

  saveReport(): void {
    if (!this.reportDialog.appointment) {
      return;
    }

    if (!this.reportDialog.reason) {
      this.reportDialog.error = 'appointments.report.errors.reasonRequired';
      return;
    }

    if (this.reportDialog.details.trim().length < 10) {
      this.reportDialog.error = 'appointments.report.errors.detailsRequired';
      return;
    }

    this.reportDialog.saving = true;

    const appointment = this.reportDialog.appointment;
    const savedReport = this.sessionService.saveReport({
      sessionId: appointment.id,
      specialistId: this.specialistId,
      patientName: appointment.patientName,
      reason: this.reportDialog.reason,
      details: this.reportDialog.details.trim()
    });

    this.reportsBySession[appointment.id] = savedReport;
    this.reportDialog.saving = false;
    this.closeReportDialog();
    this.showToastMessage('appointments.report.toast.saved', 'error');
  }

  confirmAction(): void {
    if (!this.confirmDialog.appointment) return;

    const { action, appointment } = this.confirmDialog;
    this.closeConfirmDialog();

    if (action === 'accept') {
      this.acceptAppointment(appointment);
    } else {
      this.rejectAppointment(appointment);
    }
  }

  loadAppointments(): void {
    this.loading = true;
    this.errorMsg = '';

    this.sessionService.getSessionsBySpecialist(this.specialistId).subscribe({
      next: (response) => {
        console.log('GET sessions by specialist response:', response);
        this.appointments = response.map(item => this.mapSessionResponse(item));
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error al cargar citas del especialista:', error);
        this.errorMsg = 'appointments.error.load';
        this.loading = false;
      }
    });
  }

  acceptAppointment(appointment: Appointment): void {
    this.updateStatus(appointment, 'accept', 'accepted');
  }

  rejectAppointment(appointment: Appointment): void {
    this.updateStatus(appointment, 'reject', 'rejected');
  }

  private updateStatus(
    appointment: Appointment,
    action: 'accept' | 'reject',
    newStatus: AppointmentStatus
  ): void {
    appointment.loading = true;

    const request = {
      specialistId: this.specialistId,
      action
    };

    console.log('PATCH session status request:', {
      sessionId: appointment.id,
      url: `/api/v1/sessions/${appointment.id}/status`,
      body: request
    });

    this.sessionService.updateSessionStatus(appointment.id, {
      specialistId: this.specialistId,
      action
    }).subscribe({
      next: () => {
        appointment.status = newStatus;
        appointment.loading = false;

        const messageKey =
          newStatus === 'accepted'
            ? 'appointments.toast.accepted'
            : 'appointments.toast.rejected';

        const toastType =
          newStatus === 'accepted'
            ? 'success'
            : 'error';

        this.showToastMessage(messageKey, toastType);
      },
      error: (error: any) => {
        console.error('Error al actualizar estado de cita:', error);
        console.error('Backend response body:', error.error);

        appointment.loading = false;
        this.showToastMessage('appointments.toast.statusError', 'error');
      }
    });
  }

  showToastMessage(messageKey: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);

    this.toastMessage = messageKey;
    this.toastType = type;
    this.showToast = true;

    this.toastTimer = setTimeout(() => {
      this.showToast = false;
    }, 3000);
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .slice(0, 2)
      .map(n => n[0])
      .join('')
      .toUpperCase();
  }

  getStatusIcon(status: AppointmentStatus): string {
    const icons = {
      pending:  'fas fa-clock',
      accepted: 'fas fa-check-circle',
      rejected: 'fas fa-times-circle',
      cancelled: 'fas fa-ban',
      finished: 'fas fa-check'
    };

    return icons[status];
  }

  getStatusLabel(status: AppointmentStatus): string {
    const labels = {
      pending:  'appointments.status.pending',
      accepted: 'appointments.status.accepted',
      rejected: 'appointments.status.rejected',
      cancelled: 'appointments.status.cancelled',
      finished: 'appointments.status.finished'
    };

    return labels[status];
  }

  isRateable(appointment: Appointment): boolean {
    return appointment.status === 'finished' || (
      appointment.status === 'accepted' && this.hasSessionEnded(appointment)
    );
  }

  hasRating(sessionId: number): boolean {
    return !!this.ratingsBySession[sessionId];
  }

  getRating(sessionId: number): SessionRating | null {
    return this.ratingsBySession[sessionId] ?? null;
  }

  hasReport(sessionId: number): boolean {
    return !!this.reportsBySession[sessionId];
  }

  getReport(sessionId: number): SessionReport | null {
    return this.reportsBySession[sessionId] ?? null;
  }

  getStarArray(stars: number): number[] {
    return Array.from({ length: 5 }, (_, index) => index + 1).map((value) => (value <= stars ? 1 : 0));
  }

  private mapSessionResponse(item: SessionResponse): Appointment {
    const appointmentDate = item.scheduleDate
      ? new Date(item.scheduleDate)
      : item.createdDate
        ? new Date(item.createdDate)
        : new Date();

    return {
      id: item.id,
      patientName: item.patientName || 'Paciente sin nombre',
      patientEmail: item.patientEmail || '',
      date: appointmentDate,
      time: this.buildAppointmentTime(item),
      endTime: this.formatTime(item.endTime),
      sessionTypeLabel: this.getSessionTypeLabel(item.typeOfSession),
      status: this.mapStatus(item.status),
      loading: false
    };
  }

  private getSessionTypeLabel(typeOfSession: number): string {
    switch (typeOfSession) {
      case 1:
        return 'Sesión virtual';
      case 2:
        return 'Sesión presencial';
      default:
        return 'Tipo de sesión no especificado';
    }
  }

  private mapStatus(status: number): AppointmentStatus {
    switch (status) {
      case 2:
        return 'accepted';
      case 3:
        return 'rejected';
      case 4: 
        return 'cancelled';
      case 5: 
        return 'finished';
      case 1:
      default:
        return 'pending';
    }
  }

  private loadRatings(): void {
    const ratings = this.sessionService.getRatingsBySpecialist(this.specialistId);
    this.ratingsBySession = ratings.reduce<Record<number, SessionRating>>((acc, rating) => {
      acc[rating.sessionId] = rating;
      return acc;
    }, {});
  }

  private loadReports(): void {
    const reports = this.sessionService.getReportsBySpecialist(this.specialistId);
    this.reportsBySession = reports.reduce<Record<number, SessionReport>>((acc, report) => {
      acc[report.sessionId] = report;
      return acc;
    }, {});
  }

  private buildAppointmentTime(item: SessionResponse): string {
    const start = this.formatTime(item.startTime);
    const end = this.formatTime(item.endTime);

    if (start && end) {
      return `${start} - ${end}`;
    }

    if (start) {
      return start;
    }

    if (end) {
      return end;
    }

    return 'Horario no disponible';
  }

  private formatTime(time?: string): string {
    if (!time) return '';

    if (/^\d{2}:\d{2}:\d{2}$/.test(time)) {
      return time.substring(0, 5);
    }

    if (/^\d{2}:\d{2}$/.test(time)) {
      return time;
    }

    return time;
  }

  private hasSessionEnded(appointment: Appointment): boolean {
    const sessionDate = new Date(appointment.date);

    if (Number.isNaN(sessionDate.getTime())) {
      return false;
    }

    const endTime = appointment.endTime || this.extractEndTime(appointment.time);
    if (!endTime) {
      return sessionDate.getTime() < Date.now();
    }

    const [hours, minutes] = endTime.split(':').map(Number);
    const sessionEnd = new Date(sessionDate);
    sessionEnd.setHours(hours || 0, minutes || 0, 0, 0);

    return sessionEnd.getTime() <= Date.now();
  }

  private extractEndTime(range: string): string {
    if (!range.includes('-')) {
      return '';
    }

    const parts = range.split('-').map((value) => value.trim());
    return parts[1] || '';
  }

  private getLoggedSpecialistId(): number {
    const currentUser: any =
      this.authService.getUser?.() ||
      JSON.parse(localStorage.getItem('currentUser') || 'null') ||
      JSON.parse(localStorage.getItem('user') || 'null');

    return Number(
      currentUser?.specialistId ||
      currentUser?.specialist?.id ||
      currentUser?.id ||
      0
    );
  }
}
