import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { AuthService } from '../../services/auth.service';
import { TranslatePipe } from '@ngx-translate/core';
import {
  SessionResponse,
  SessionService,
  SessionRating,
  SessionReport,
  CalificationResponse,
  ReportResponse
} from '../../services/session.service';

interface RatingDialog {
  visible: boolean;
  appointment: SessionResponse | null;
  stars: number;
  comment: string;
  saving: boolean;
  error: string;
}

interface ReportDialog {
  visible: boolean;
  appointment: SessionResponse | null;
  reason: string;
  details: string;
  saving: boolean;
  error: string;
}

@Component({
  selector: 'app-patient-appointments',
  standalone: true,
  imports: [CommonModule, FormsModule, SidebarComponent, TranslatePipe],
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
  patientId!: number;

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  ratingsBySession: Record<number, CalificationResponse> = {};

  reportsBySession: Record<number, ReportResponse> = {};

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

  constructor(
    private sessionService: SessionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.patientId = this.getPatientId() ?? 0;
    this.loadAppointments();
  }

  loadAppointments(): void {
    if (!this.patientId) {
      this.errorMsg = 'No se pudo identificar al paciente actual.';
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.sessionService.getSessionsByPatient(this.patientId).subscribe({
      next: (sessions) => {
        this.appointments = sessions;
        this.loading = false;
        this.loadRatingsAndReportsForFinishedSessions(sessions);
      },
      error: (error) => {
        this.errorMsg = error.error?.message || 'No se pudieron cargar tus citas.';
        this.loading = false;
      }
    });
  }

  private loadRatingsAndReportsForFinishedSessions(sessions: SessionResponse[]): void {
    sessions
      .filter(s => s.status === 4)
      .forEach(session => {

        this.sessionService.getPatientRating(session.id).subscribe({
          next: (rating) => { this.ratingsBySession[session.id] = rating; },
          error: () => {} 
        });

        this.sessionService.getPatientReportBySession(session.id, this.patientId).subscribe({
          next: (report) => { this.reportsBySession[session.id] = report; },
          error: () => {} 
        });
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
    if (!this.appointmentToCancel) return;

    const appointmentId = this.appointmentToCancel.id;
    this.cancellingId = appointmentId;
    this.errorMsg = '';
    this.successMsg = '';

    this.sessionService.cancelSession(appointmentId, { patientId: this.patientId }).subscribe({
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

  openRatingDialog(appointment: SessionResponse): void {
    const existing = this.ratingsBySession[appointment.id];
    this.ratingDialog = {
      visible: true,
      appointment,
      stars: existing?.rating ?? 0,
      comment: existing?.comment ?? '',
      saving: false,
      error: ''
    };
  }

  closeRatingDialog(): void {
    this.ratingDialog = {
      visible: false, appointment: null,
      stars: 0, comment: '', saving: false, error: ''
    };
  }

  setRatingStars(stars: number): void {
    this.ratingDialog.stars = stars;
    this.ratingDialog.error = '';
  }

  saveRating(): void {
    if (!this.ratingDialog.appointment) return;

    if (this.ratingDialog.stars < 1) {
      this.ratingDialog.error = 'Debes seleccionar al menos una estrella.';
      return;
    }

    if (this.ratingDialog.comment.trim().length < 5) {
      this.ratingDialog.error = 'El comentario debe tener al menos 5 caracteres.';
      return;
    }

    this.ratingDialog.saving = true;
    const appointment = this.ratingDialog.appointment;

    this.sessionService.createPatientRating(appointment.id, {
      rating: this.ratingDialog.stars,
      comment: this.ratingDialog.comment.trim()
    }).subscribe({
      next: (saved) => {
        this.ratingsBySession[appointment.id] = saved;
        this.ratingDialog.saving = false;
        this.closeRatingDialog();
        this.showToastMessage('Calificación guardada correctamente', 'success');
      },
      error: (error) => {
        this.ratingDialog.saving = false;
        this.ratingDialog.error = error.error?.message || 'No se pudo guardar la calificación.';
      }
    });
  }


  openReportDialog(appointment: SessionResponse): void {
    const existing = this.reportsBySession[appointment.id];
    this.reportDialog = {
      visible: true,
      appointment,
      // Si ya existe un reporte, pre-rellena el formulario
      reason: existing?.reason ?? '',
      details: existing?.description ?? '',
      saving: false,
      error: ''
    };
  }

  closeReportDialog(): void {
    this.reportDialog = {
      visible: false, appointment: null,
      reason: '', details: '', saving: false, error: ''
    };
  }

  saveReport(): void {
    if (!this.reportDialog.appointment) return;

    if (!this.reportDialog.reason) {
      this.reportDialog.error = 'Debes seleccionar un motivo.';
      return;
    }

    if (this.reportDialog.details.trim().length < 10) {
      this.reportDialog.error = 'Los detalles deben tener al menos 10 caracteres.';
      return;
    }

    this.reportDialog.saving = true;
    const appointment = this.reportDialog.appointment;

    // ✅ Ahora llama al endpoint correcto: POST /api/v1/reports/patient
    this.sessionService.createPatientReport({
      patientId: this.patientId,
      sessionId: appointment.id,
      reason: this.reportDialog.reason,
      description: this.reportDialog.details.trim()
    }).subscribe({
      next: (saved) => {
        this.reportsBySession[appointment.id] = saved;
        this.reportDialog.saving = false;
        this.closeReportDialog();
        this.showToastMessage('Reporte enviado correctamente', 'success');
      },
      error: (error) => {
        this.reportDialog.saving = false;
        this.reportDialog.error = error.error?.message || 'No se pudo enviar el reporte.';
      }
    });
  }

  // ── Helpers de vista ───────────────────────────────────────────────────────

  isRateable(appointment: SessionResponse): boolean {
    return appointment.status === 4;
  }

  hasRating(sessionId: number): boolean {
    return !!this.ratingsBySession[sessionId];
  }

  getRating(sessionId: number): CalificationResponse | null {
    return this.ratingsBySession[sessionId] ?? null;
  }

  hasReport(sessionId: number): boolean {
    return !!this.reportsBySession[sessionId];
  }

  getReport(sessionId: number): ReportResponse | null {
    return this.reportsBySession[sessionId] ?? null;
  }

  getStarArray(stars: number): number[] {
    return Array.from({ length: 5 }, (_, i) => (i + 1 <= stars ? 1 : 0));
  }

  canCancel(session: SessionResponse): boolean {
    return session.status === 1 || session.status === 2;
  }

  getStatusLabel(status: number): string {
    const labels: Record<number, string> = {
      1: 'patientAppointments.status.pending',
      2: 'patientAppointments.status.accepted',
      3: 'patientAppointments.status.rejected',
      4: 'patientAppointments.status.finished',
      5: 'patientAppointments.status.cancelled'
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

  showToastMessage(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => this.showToast = false, 3000);
  }

  private getPatientId(): number | null {
    const user = this.authService.getUser();
    return user?.id ?? null;
  }
}
