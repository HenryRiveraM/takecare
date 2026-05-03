import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { AuthService } from '../../services/auth.service';
import { SessionResponse, SessionService } from '../../services/session.service';

@Component({
  selector: 'app-patient-appointments',
  standalone: true,
  imports: [CommonModule, SidebarComponent],
  templateUrl: './patient-appointments.component.html',
  styleUrl: './patient-appointments.component.css'
})
export class PatientAppointmentsComponent implements OnInit {

  appointments: SessionResponse[] = [];
  loading = false;
  errorMsg = '';
  successMsg = '';
  cancellingId: number | null = null;
  appointmentToCancel: SessionResponse | null = null;
  showCancelConfirm = false;

  constructor(
    private sessionService: SessionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadAppointments();
  }

  loadAppointments(): void {
    const patientId = this.getPatientId();

    if (!patientId) {
      this.errorMsg = 'No se pudo identificar al paciente actual.';
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.sessionService.getSessionsByPatient(patientId).subscribe({
      next: (sessions) => {
        this.appointments = sessions;
        this.loading = false;
      },
      error: (error) => {
        this.errorMsg = error.error?.message || 'No se pudieron cargar tus citas.';
        this.loading = false;
      }
    });
  }

  openCancelConfirm(appointment: SessionResponse): void {
    this.appointmentToCancel = appointment;
    this.showCancelConfirm = true;
    this.errorMsg = '';
    this.successMsg = '';
  }

  closeCancelConfirm(): void {
    this.appointmentToCancel = null;
    this.showCancelConfirm = false;
  }

  confirmCancelAppointment(): void {
    if (!this.appointmentToCancel) {
      return;
    }

    const patientId = this.getPatientId();

    if (!patientId) {
      this.errorMsg = 'No se pudo identificar al paciente actual.';
      this.closeCancelConfirm();
      return;
    }

    const appointmentId = this.appointmentToCancel.id;

    this.cancellingId = appointmentId;
    this.errorMsg = '';
    this.successMsg = '';

    this.sessionService.cancelSession(appointmentId, { patientId }).subscribe({
      next: () => {
        this.cancellingId = null;
        this.successMsg = 'Cita cancelada correctamente.';
        this.closeCancelConfirm();
        this.loadAppointments();
      },
      error: (error) => {
        this.cancellingId = null;
        this.errorMsg = error.error?.message || 'No se pudo cancelar la cita.';
        this.closeCancelConfirm();
      }
    });
  }

  canCancel(session: SessionResponse): boolean {
    return session.status === 1 || session.status === 2;
  }

  getStatusLabel(status: number): string {
    const labels: Record<number, string> = {
      1: 'Pendiente',
      2: 'Aceptada',
      3: 'Rechazada',
      4: 'Finalizada',
      5: 'Cancelada'
    };

    return labels[status] || 'Desconocido';
  }

  getStatusClass(status: number): string {
    const classes: Record<number, string> = {
      1: 'pending',
      2: 'accepted',
      3: 'rejected',
      4: 'finished',
      5: 'cancelled'
    };

    return classes[status] || 'unknown';
  }

  private getPatientId(): number | null {
    const user = this.authService.getUser();

    if (!user || !user.id) {
      return null;
    }

    return user.id;
  }
}