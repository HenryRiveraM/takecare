import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { AuthService } from '../../services/auth.service';
import { SessionRating, SessionReport, SessionService } from '../../services/session.service';

export type AppointmentStatus = 'pending' | 'accepted' | 'rejected';
export type DialogAction = 'accept' | 'reject';

export interface Appointment {
  id: string;
  patientName: string;
  patientEmail: string;
  date: Date;
  time: string;
  reason: string;
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
  specialistId = 0;
  ratingsByAppointment: Record<string, SessionRating> = {};
  reportsByAppointment: Record<string, SessionReport> = {};

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
    private authService: AuthService,
    private sessionService: SessionService
  ) {}

  ngOnInit(): void {
    this.specialistId = this.authService.getUser()?.id ?? 0;
    this.loadMockAppointments();
    this.loadRatings();
    this.loadReports();
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
    const existingRating = this.ratingsByAppointment[appointment.id];

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
    const existingReport = this.reportsByAppointment[appointment.id];

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
      appointmentId: appointment.id,
      specialistId: this.specialistId,
      patientName: appointment.patientName,
      stars: this.ratingDialog.stars,
      comment: this.ratingDialog.comment.trim()
    });

    this.ratingsByAppointment[appointment.id] = savedRating;
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
      appointmentId: appointment.id,
      specialistId: this.specialistId,
      patientName: appointment.patientName,
      reason: this.reportDialog.reason,
      details: this.reportDialog.details.trim()
    });

    this.reportsByAppointment[appointment.id] = savedReport;
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

  acceptAppointment(appointment: Appointment): void {
    appointment.loading = true;

    setTimeout(() => {
      appointment.status = 'accepted';
      appointment.loading = false;
      this.showToastMessage('appointments.toast.accepted', 'success');
    }, 800);
  }

  rejectAppointment(appointment: Appointment): void {
    appointment.loading = true;

    setTimeout(() => {
      appointment.status = 'rejected';
      appointment.loading = false;
      this.showToastMessage('appointments.toast.rejected', 'error');
    }, 800);
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
      rejected: 'fas fa-times-circle'
    };
    return icons[status];
  }

  getStatusLabel(status: AppointmentStatus): string {
    const labels = {
      pending:  'appointments.status.pending',
      accepted: 'appointments.status.accepted',
      rejected: 'appointments.status.rejected'
    };
    return labels[status];
  }

  isRateable(appointment: Appointment): boolean {
    return appointment.status === 'accepted' && this.hasSessionEnded(appointment);
  }

  hasRating(appointmentId: string): boolean {
    return !!this.ratingsByAppointment[appointmentId];
  }

  getRating(appointmentId: string): SessionRating | null {
    return this.ratingsByAppointment[appointmentId] ?? null;
  }

  hasReport(appointmentId: string): boolean {
    return !!this.reportsByAppointment[appointmentId];
  }

  getReport(appointmentId: string): SessionReport | null {
    return this.reportsByAppointment[appointmentId] ?? null;
  }

  getStarArray(stars: number): number[] {
    return Array.from({ length: 5 }, (_, index) => index + 1).map((value) => (value <= stars ? 1 : 0));
  }

  private loadMockAppointments(): void {
    this.appointments = [
      {
        id: '1',
        patientName: 'María López',
        patientEmail: 'maria@email.com',
        date: new Date('2026-04-28'),
        time: '10:00 AM',
        reason: 'Consulta por ansiedad generalizada',
        status: 'pending'
      },
      {
        id: '2',
        patientName: 'Carlos Mendoza',
        patientEmail: 'carlos@email.com',
        date: new Date('2026-04-29'),
        time: '02:30 PM',
        reason: 'Seguimiento terapia familiar',
        status: 'pending'
      },
      {
        id: '3',
        patientName: 'Ana Gutierrez',
        patientEmail: 'ana@email.com',
        date: new Date('2026-04-27'),
        time: '11:00 AM',
        reason: 'Primera consulta',
        status: 'accepted'
      },
      {
        id: '5',
        patientName: 'Pedro Salazar',
        patientEmail: 'pedro@email.com',
        date: new Date('2026-04-30'),
        time: '04:00 PM',
        reason: 'Sesión de seguimiento emocional',
        status: 'accepted'
      },
      {
        id: '4',
        patientName: 'Luis Rojas',
        patientEmail: 'luis@email.com',
        date: new Date('2026-04-26'),
        time: '09:00 AM',
        reason: 'Consulta por depresión',
        status: 'rejected'
      }
    ];
  }

  private loadRatings(): void {
    if (!this.specialistId) {
      return;
    }

    const ratings = this.sessionService.getRatingsBySpecialist(this.specialistId);
    this.ratingsByAppointment = ratings.reduce<Record<string, SessionRating>>((acc, rating) => {
      acc[rating.appointmentId] = rating;
      return acc;
    }, {});
  }

  private loadReports(): void {
    if (!this.specialistId) {
      return;
    }

    const reports = this.sessionService.getReportsBySpecialist(this.specialistId);
    this.reportsByAppointment = reports.reduce<Record<string, SessionReport>>((acc, report) => {
      acc[report.appointmentId] = report;
      return acc;
    }, {});
  }

  private hasSessionEnded(appointment: Appointment): boolean {
    const appointmentDateTime = this.buildAppointmentDateTime(appointment);
    return appointmentDateTime.getTime() < Date.now();
  }

  private buildAppointmentDateTime(appointment: Appointment): Date {
    const baseDate = new Date(appointment.date);
    const normalizedTime = appointment.time.trim();
    const match = normalizedTime.match(/^(\d{1,2}):(\d{2})\s?(AM|PM)$/i);

    if (!match) {
      return baseDate;
    }

    let hours = Number(match[1]);
    const minutes = Number(match[2]);
    const meridiem = match[3].toUpperCase();

    if (meridiem === 'PM' && hours !== 12) {
      hours += 12;
    }

    if (meridiem === 'AM' && hours === 12) {
      hours = 0;
    }

    return new Date(
      baseDate.getFullYear(),
      baseDate.getMonth(),
      baseDate.getDate(),
      hours,
      minutes,
      0,
      0
    );
  }
}
