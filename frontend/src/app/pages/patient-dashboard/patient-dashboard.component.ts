import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms'; 
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { SessionResponse, SessionService } from '../../services/session.service';
import { EmotionalRecord } from '../../services/emotional-record.service';
import { EmotionalRecordModalComponent } from '../../shared/emotional-record-modal/emotional-record-modal.component';


@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule, SidebarComponent, EmotionalRecordModalComponent],
  templateUrl: './patient-dashboard.component.html',
  styleUrls: ['./patient-dashboard.component.css']
})

export class PatientDashboardComponent implements OnInit, OnDestroy {
  user: any;
  searchTerm: string = '';
  todayAppointments: SessionResponse[] = [];
  loadingAppointments = false;
  appointmentsError = '';
  showRequiredEmotionModal = false;
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  constructor(
    private authService: AuthService,
    private sessionService: SessionService,
    private router: Router,
    public sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
    this.loadTodayAppointments();
    this.openRequiredEmotionModalIfNeeded();
  }

  ngOnDestroy(): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  goToSpecialistSearch(): void {
    if (this.searchTerm.trim()) {
      this.router.navigate(['/patient/search-specialists'], { 
        queryParams: { q: this.searchTerm } 
      });
    }
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

  getStatusLabel(status: number): string {
    const labels: Record<number, string> = {
      1: 'patientAppointments.status.pending',
      2: 'patientAppointments.status.accepted',
      3: 'patientAppointments.status.rejected',
      4: 'patientAppointments.status.finished',
      5: 'patientAppointments.status.cancelled'
    };

    return labels[status] || 'patientDashboard.todayAppointments.unknownStatus';
  }

  getSessionTypeLabel(typeOfSession: number): string {
    switch (typeOfSession) {
      case 1: return 'patientAppointments.card.virtual';
      case 2: return 'patientAppointments.card.presential';
      default: return 'patientAppointments.card.sessionTypeUnknown';
    }
  }

  getAppointmentDate(appointment: SessionResponse): Date | null {
    return appointment.scheduleDate
      ? new Date(`${appointment.scheduleDate}T00:00:00`)
      : null;
  }

  getInitials(name: string): string {
    if (!name) {
      return '?';
    }

    return name
      .split(' ')
      .slice(0, 2)
      .map(part => part.charAt(0))
      .join('')
      .toUpperCase();
  }

  handleRequiredEmotionSaved(_: EmotionalRecord): void {
    localStorage.setItem(this.getEmotionSessionKey(), 'true');
    this.showRequiredEmotionModal = false;
    this.showToastMessage('emotionalLog.toast.saved', 'success');
  }

  handleRequiredEmotionFailed(message: string): void {
    this.showToastMessage(message || 'emotionalLog.errors.save', 'error');
  }

  get patientId(): number {
    return Number(this.user?.id || 0);
  }

  private loadTodayAppointments(): void {
    const patientId = this.user?.id;

    if (!patientId) {
      this.appointmentsError = 'patientDashboard.todayAppointments.noPatient';
      return;
    }

    this.loadingAppointments = true;
    this.appointmentsError = '';

    this.sessionService.getSessionsByPatient(patientId).subscribe({
      next: (sessions) => {
        const today = this.formatLocalDate(new Date());

        this.todayAppointments = sessions
          .filter(session => session.scheduleDate === today)
          .sort((first, second) =>
            (first.startTime || '').localeCompare(second.startTime || '')
          );
        this.loadingAppointments = false;
      },
      error: () => {
        this.todayAppointments = [];
        this.loadingAppointments = false;
        this.appointmentsError = 'patientDashboard.todayAppointments.loadError';
      }
    });
  }

  private openRequiredEmotionModalIfNeeded(): void {
    if (!this.patientId) {
      return;
    }

    this.showRequiredEmotionModal = localStorage.getItem(this.getEmotionSessionKey()) !== 'true';
  }

  private getEmotionSessionKey(): string {
    return `takecare-emotional-first-checkin-${this.patientId}`;
  }

  private showToastMessage(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 3200);
  }

  private formatLocalDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${date.getFullYear()}-${month}-${day}`;
  }
}
