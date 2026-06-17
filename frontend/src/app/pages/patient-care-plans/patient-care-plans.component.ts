import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { CarePlan, CarePlanItem, CarePlanService, CarePlanStatus } from '../../services/care-plan.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { CarePlanProgressDashboardComponent } from '../../shared/care-plan-progress-dashboard/care-plan-progress-dashboard.component';
import { CarePlanLogbookComponent } from '../../shared/care-plan-logbook/care-plan-logbook.component';

@Component({
  selector: 'app-patient-care-plans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent, CarePlanProgressDashboardComponent, CarePlanLogbookComponent],
  templateUrl: './patient-care-plans.component.html',
  styleUrls: ['./patient-care-plans.component.css']
})
export class PatientCarePlansComponent implements OnInit, OnDestroy {

  patientId = 0;
  carePlans: CarePlan[] = [];
  selectedPlan: CarePlan | null = null;
  selectedPlanId: number | null = null;
  highlightedPlanId: number | null = null;
  totalCarePlans = 0;
  loading = false;
  updatingItemId: number | null = null;
  errorMsg = '';

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;
  private highlightTimer: any;
  private queryParamsSubscription?: Subscription;

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private carePlanService: CarePlanService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.patientId = Number(this.authService.getUser()?.id || 0);
    this.queryParamsSubscription = this.route.queryParamMap.subscribe(params => {
      this.highlightedPlanId = Number(params.get('highlightPlanId') || 0) || null;
      if (this.highlightedPlanId && this.carePlans.length) {
        const highlightedPlan = this.carePlans.find(plan => plan.id === this.highlightedPlanId);
        if (highlightedPlan) {
          this.selectPlan(highlightedPlan);
        }
        this.applyHighlight();
      }
    });

    if (!this.patientId) {
      this.errorMsg = 'carePlans.errors.noPatient';
      return;
    }

    this.loadCarePlans();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active');
    if (this.toastTimer) clearTimeout(this.toastTimer);
    if (this.highlightTimer) clearTimeout(this.highlightTimer);
    this.queryParamsSubscription?.unsubscribe();
  }

  selectPlan(plan: CarePlan): void {
    this.selectedPlan = plan;
    this.selectedPlanId = plan.id;
  }

  selectPlanById(planId: number | string | null): void {
    const nextPlanId = Number(planId || 0);
    const plan = this.carePlans.find(item => item.id === nextPlanId);
    if (plan) {
      this.selectPlan(plan);
    }
  }

  toggleItem(item: CarePlanItem): void {
    if (!this.selectedPlan || this.updatingItemId || !this.canModifySelectedPlan()) {
      return;
    }

    this.updatingItemId = item.id;
    const request = item.status === 'COMPLETED'
      ? this.carePlanService.markActivityPending(item.id, this.patientId)
      : this.carePlanService.completeActivity(item.id, this.patientId);

    request.subscribe({
      next: response => {
        this.updatingItemId = null;
        this.applyActivityProgress(item.id, response.status, response.completedDate, response.planProgressPercentage);
        const message = item.status === 'COMPLETED'
          ? 'carePlans.activities.toast.pending'
          : 'carePlans.activities.toast.completed';
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

  activeActivities(plan: CarePlan | null): CarePlanItem[] {
    return (plan?.items || []).filter(item => item.status === 'PENDING' || item.status === 'COMPLETED');
  }

  completedActivities(plan: CarePlan | null): number {
    return this.activeActivities(plan).filter(item => item.status === 'COMPLETED').length;
  }

  pendingActivities(plan: CarePlan | null): number {
    return this.activeActivities(plan).filter(item => item.status === 'PENDING').length;
  }

  getProgressPercentage(plan: CarePlan | null): number {
    const progress = Number(plan?.progressPercentage ?? 0);

    if (!Number.isFinite(progress)) {
      return 0;
    }

    return Math.min(100, Math.max(0, Math.round(progress)));
  }

  getSpecialistName(plan: CarePlan | null): string {
    return plan?.specialistName || 'carePlans.notAvailable';
  }

  canModifySelectedPlan(): boolean {
    return String(this.selectedPlan?.status || '').toUpperCase() === 'ACTIVE';
  }

  getPlanLockMessage(): string {
    const status = String(this.selectedPlan?.status || '').toUpperCase();
    const messages: Record<string, string> = {
      PAUSED: 'carePlans.followUpLocked.paused',
      COMPLETED: 'carePlans.followUpLocked.completed',
      CANCELLED: 'carePlans.followUpLocked.cancelled'
    };
    return messages[status] || '';
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
          const highlightedPlan = this.highlightedPlanId
            ? this.carePlans.find(plan => plan.id === this.highlightedPlanId)
            : null;
          const activePlan = this.carePlans.find(plan => String(plan.status).toUpperCase() === 'ACTIVE');
          this.selectPlan(highlightedPlan || activePlan || this.carePlans[0]);
          this.applyHighlight();
        }
      },
      error: error => {
        this.errorMsg = error?.error?.message || 'carePlans.errors.load';
        this.loading = false;
      }
    });
  }

  private applyActivityProgress(
    activityId: number,
    status: CarePlanItem['status'],
    completedDate: string | null,
    progressPercentage: number
  ): void {
    if (!this.selectedPlan) {
      return;
    }

    const updatedPlan = {
      ...this.selectedPlan,
      progressPercentage,
      items: (this.selectedPlan.items || []).map(item =>
        item.id === activityId ? { ...item, status, completedDate } : item
      )
    };

    this.selectedPlan = updatedPlan;
    this.carePlans = this.carePlans.map(plan =>
      plan.id === updatedPlan.id ? updatedPlan : plan
    );
  }

  private showToastMessage(messageKey: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = messageKey;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 3000);
  }

  private applyHighlight(): void {
    if (!this.highlightedPlanId) {
      return;
    }

    setTimeout(() => {
      const element = document.getElementById(`patient-care-plan-${this.highlightedPlanId}`);
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);

    if (this.highlightTimer) clearTimeout(this.highlightTimer);
    this.highlightTimer = setTimeout(() => {
      this.highlightedPlanId = null;
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { highlightPlanId: null },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    }, 3000);
  }

  get currentPatientName(): string {
    return this.authService.getUser()?.names || '';
  }
}
