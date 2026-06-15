import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import { CarePlan, CarePlanItem, CarePlanService, CarePlanStatus } from '../../services/care-plan.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';

@Component({
  selector: 'app-patient-care-plans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-care-plans.component.html',
  styleUrls: ['./patient-care-plans.component.css']
})
export class PatientCarePlansComponent implements OnInit, OnDestroy {

  patientId = 0;
  carePlans: CarePlan[] = [];
  selectedPlan: CarePlan | null = null;
  totalCarePlans = 0;
  loading = false;
  updatingItemId: number | null = null;
  errorMsg = '';

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private carePlanService: CarePlanService
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.patientId = Number(this.authService.getUser()?.id || 0);

    if (!this.patientId) {
      this.errorMsg = 'carePlans.errors.noPatient';
      return;
    }

    this.loadCarePlans();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active');
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  selectPlan(plan: CarePlan): void {
    this.carePlanService.getCarePlanById(plan.id, undefined, this.patientId).subscribe({
      next: response => {
        this.selectedPlan = response;
        this.carePlans = this.carePlans.map(item => item.id === response.id ? response : item);
      },
      error: error => this.showToastMessage(error?.error?.message || 'carePlans.toast.detailError', 'error')
    });
  }

  toggleItem(item: CarePlanItem): void {
    if (!this.selectedPlan || this.updatingItemId) {
      return;
    }

    this.updatingItemId = item.id;
    const request = item.status === 'COMPLETED'
      ? this.carePlanService.markCarePlanItemPending(item.id, this.patientId)
      : this.carePlanService.completeCarePlanItem(item.id, this.patientId);

    request.subscribe({
      next: plan => {
        this.updatingItemId = null;
        this.selectedPlan = plan;
        this.carePlans = this.carePlans.map(existing => existing.id === plan.id ? plan : existing);
        const message = item.status === 'COMPLETED'
          ? 'carePlans.toast.itemPending'
          : 'carePlans.toast.itemCompleted';
        this.showToastMessage(message, 'success');
      },
      error: error => {
        this.updatingItemId = null;
        this.showToastMessage(error?.error?.message || 'carePlans.toast.itemError', 'error');
      }
    });
  }

  getStatusLabel(status: CarePlanStatus | string): string {
    const labels: Record<string, string> = {
      ACTIVE: 'carePlans.status.active',
      PAUSED: 'carePlans.status.paused',
      COMPLETED: 'carePlans.status.completed',
      CANCELLED: 'carePlans.status.cancelled'
    };
    return labels[String(status || '').toUpperCase()] || 'carePlans.status.unknown';
  }

  asLocalDate(value: string | null | undefined): Date | null {
    return value ? new Date(`${value}T00:00:00`) : null;
  }

  private loadCarePlans(): void {
    this.loading = true;
    this.errorMsg = '';

    this.carePlanService.getPatientCarePlans(this.patientId).subscribe({
      next: response => {
        this.carePlans = response.carePlans || [];
        this.totalCarePlans = response.totalCarePlans || 0;
        this.loading = false;
        if (this.carePlans.length) {
          this.selectPlan(this.carePlans[0]);
        }
      },
      error: error => {
        this.errorMsg = error?.error?.message || 'carePlans.errors.load';
        this.loading = false;
      }
    });
  }

  private showToastMessage(messageKey: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = messageKey;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 3000);
  }
}
