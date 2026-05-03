import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { AuthService } from '../../services/auth.service';
import {
  SessionResponse,
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
  sessionTypeLabel: string;
  status: AppointmentStatus;
  loading?: boolean;
}

interface ConfirmDialog {
  visible: boolean;
  action: DialogAction;
  appointment: Appointment | null;
}

@Component({
  selector: 'app-specialist-appointments',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, SidebarComponent],
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

  confirmDialog: ConfirmDialog = {
    visible: false,
    action: 'accept',
    appointment: null
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