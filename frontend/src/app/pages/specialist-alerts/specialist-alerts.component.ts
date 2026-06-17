import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import { AlertPriority, SpecialistAlert, SpecialistAlertsService } from '../../services/specialist-alerts.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';

type AlertStatusFilter = 'all' | 'pending' | 'reviewed';
type AlertPriorityFilter = 'all' | AlertPriority;

@Component({
  selector: 'app-specialist-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './specialist-alerts.component.html',
  styleUrls: ['./specialist-alerts.component.css']
})
export class SpecialistAlertsComponent implements OnInit, OnDestroy {
  specialistId = 0;
  alerts: SpecialistAlert[] = [];
  loading = false;
  errorMsg = '';
  statusFilter: AlertStatusFilter = 'pending';
  priorityFilter: AlertPriorityFilter = 'all';
  reviewingId: number | null = null;

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private alertsService: SpecialistAlertsService
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.specialistId = Number(this.authService.getUser()?.id || 0);

    if (!this.specialistId) {
      this.errorMsg = 'specialistAlerts.errors.noSpecialist';
      return;
    }

    this.loadAlerts();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active');
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  get filteredAlerts(): SpecialistAlert[] {
    return this.alerts.filter(alert => {
      const matchesStatus =
        this.statusFilter === 'all' ||
        (this.statusFilter === 'pending' && !alert.reviewed) ||
        (this.statusFilter === 'reviewed' && alert.reviewed);

      const matchesPriority = this.priorityFilter === 'all' || alert.priority === this.priorityFilter;

      return matchesStatus && matchesPriority;
    });
  }

  get highCount(): number {
    return this.alerts.filter(alert => alert.priority === 'HIGH' && !alert.reviewed).length;
  }

  get mediumCount(): number {
    return this.alerts.filter(alert => alert.priority === 'MEDIUM' && !alert.reviewed).length;
  }

  get lowCount(): number {
    return this.alerts.filter(alert => alert.priority === 'LOW' && !alert.reviewed).length;
  }

  get pendingCount(): number {
    return this.alerts.filter(alert => !alert.reviewed).length;
  }

  markAsReviewed(alert: SpecialistAlert): void {
    if (!alert.id || alert.reviewed || this.reviewingId) {
      return;
    }

    this.reviewingId = alert.id;

    this.alertsService.markAsReviewed(this.specialistId, alert.id).subscribe({
      next: updatedAlert => {
        this.alerts = this.alerts.map(item =>
          item.id === alert.id ? { ...item, ...updatedAlert, reviewed: true } : item
        );
        this.reviewingId = null;
        this.showToastMessage('specialistAlerts.toast.reviewed', 'success');
      },
      error: error => {
        this.reviewingId = null;
        this.showToastMessage(error?.error?.message || 'specialistAlerts.errors.review', 'error');
      }
    });
  }

  getPriorityLabel(priority: AlertPriority): string {
    const labels: Record<AlertPriority, string> = {
      HIGH: 'specialistAlerts.priorities.high',
      MEDIUM: 'specialistAlerts.priorities.medium',
      LOW: 'specialistAlerts.priorities.low'
    };
    return labels[priority] || labels.LOW;
  }

  getPriorityIcon(priority: AlertPriority): string {
    const icons: Record<AlertPriority, string> = {
      HIGH: 'priority_high',
      MEDIUM: 'error',
      LOW: 'info'
    };
    return icons[priority] || icons.LOW;
  }

  getAlertDate(alert: SpecialistAlert): Date | null {
    const value = alert.detectedAt || alert.createdDate || alert.createdAt || alert.reviewedAt;
    return value ? new Date(value) : null;
  }

  trackByAlert(_index: number, alert: SpecialistAlert): number {
    return alert.id;
  }

  private loadAlerts(): void {
    this.loading = true;
    this.errorMsg = '';

    this.alertsService.getAlerts(this.specialistId).subscribe({
      next: alerts => {
        this.alerts = this.sortAlerts(alerts);
        this.loading = false;
      },
      error: error => {
        this.alerts = [];
        this.loading = false;
        this.errorMsg = error?.error?.message || 'specialistAlerts.errors.load';
      }
    });
  }

  private sortAlerts(alerts: SpecialistAlert[]): SpecialistAlert[] {
    const priorityWeight: Record<AlertPriority, number> = {
      HIGH: 1,
      MEDIUM: 2,
      LOW: 3
    };

    return [...alerts].sort((first, second) => {
      if (Boolean(first.reviewed) !== Boolean(second.reviewed)) {
        return first.reviewed ? 1 : -1;
      }

      const priorityDiff = priorityWeight[first.priority] - priorityWeight[second.priority];
      if (priorityDiff !== 0) {
        return priorityDiff;
      }

      return (this.getAlertDate(second)?.getTime() || 0) - (this.getAlertDate(first)?.getTime() || 0);
    });
  }

  private showToastMessage(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 3200);
  }
}
