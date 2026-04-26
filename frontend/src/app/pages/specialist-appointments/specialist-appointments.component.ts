import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';

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

  confirmDialog: ConfirmDialog = {
    visible: false,
    action: 'accept',
    appointment: null
  };

  appointments: Appointment[] = [];

  constructor(public sidebarService: SidebarService) {}

  ngOnInit(): void {
    this.loadMockAppointments();
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
}
