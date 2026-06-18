import { Component, OnInit, OnDestroy} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';           
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SessionResponse, SessionService } from '../../services/session.service';
import { SpecialistAlertsService } from '../../services/specialist-alerts.service';

@Component({
  selector: 'app-specialist-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslatePipe,
    SidebarComponent,  
  ],
  templateUrl: './specialist-dashboard.component.html',
  styleUrls: ['./specialist-dashboard.component.css']
})
export class SpecialistDashboardComponent implements OnInit, OnDestroy {

  user: any;
  totalPatients = 0;
  todaySessions = 0;
  pendingAlerts = 0;
  upcomingSessions: SessionResponse[] = [];
  loadingSummary = false;
  summaryError = '';

  constructor(
    private authService: AuthService,
    private sessionService: SessionService,
    private alertsService: SpecialistAlertsService,
    public sidebarService: SidebarService 
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.user = this.authService.getUser();
    this.loadDashboardSummary();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active'); 
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
      1: 'appointments.status.pending',
      2: 'appointments.status.accepted',
      3: 'appointments.status.rejected',
      4: 'appointments.status.finished',
      5: 'appointments.status.cancelled'
    };

    return labels[status] || 'specialistDashboard.upcoming.unknownStatus';
  }

  getSessionTypeLabel(typeOfSession: number): string {
    switch (typeOfSession) {
      case 1: return 'appointments.sessionTypes.virtual';
      case 2: return 'appointments.sessionTypes.presential';
      default: return 'appointments.sessionTypes.unknown';
    }
  }

  getAppointmentDate(session: SessionResponse): Date | null {
    return session.scheduleDate
      ? new Date(`${session.scheduleDate}T00:00:00`)
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

  private loadDashboardSummary(): void {
    const specialistId = this.user?.id;

    if (!specialistId) {
      this.summaryError = 'specialistDashboard.upcoming.noSpecialist';
      return;
    }

    this.loadingSummary = true;
    this.summaryError = '';

    this.sessionService.getSessionsBySpecialist(specialistId).subscribe({
      next: (sessions) => {
        const today = this.formatLocalDate(new Date());
        const validCareSessions = sessions.filter(session =>
          session.status === 2 || session.status === 4
        );

        this.totalPatients = new Set(
          validCareSessions.map(session => session.patientId)
        ).size;
        this.todaySessions = validCareSessions.filter(session =>
          session.scheduleDate === today
        ).length;
        this.upcomingSessions = sessions
          .filter(session => session.status === 2 && this.isUpcoming(session))
          .sort((first, second) =>
            this.sessionStartTime(first) - this.sessionStartTime(second)
          );

        this.alertsService.getAlerts(specialistId).subscribe({
          next: (alerts) => {
            this.pendingAlerts = alerts.filter(alert => alert.status === 'OPEN' || !alert.reviewed).length;
            this.loadingSummary = false;
          },
          error: (err) => {
            console.error('Error loading alerts for summary:', err);
            this.pendingAlerts = 0;
            this.loadingSummary = false;
          }
        });
      },
      error: () => {
        this.loadingSummary = false;
        this.summaryError = 'specialistDashboard.upcoming.loadError';
      }
    });
  }

  private isUpcoming(session: SessionResponse): boolean {
    const start = this.sessionStartTime(session);
    return Number.isFinite(start) && start >= Date.now();
  }

  private sessionStartTime(session: SessionResponse): number {
    if (!session.scheduleDate) {
      return Number.NaN;
    }

    const time = session.startTime?.substring(0, 5) || '00:00';
    return new Date(`${session.scheduleDate}T${time}:00`).getTime();
  }

  private formatLocalDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${date.getFullYear()}-${month}-${day}`;
  }
}
