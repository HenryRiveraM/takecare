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

@Component({
  selector: 'app-patient-care-plans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent, CarePlanProgressDashboardComponent],
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

  /*private loadCarePlans(): void {
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
  }*/

  private loadCarePlans(): void {
    this.loading = true;
    this.errorMsg = '';

    // TODO: descomentar cuando puedas probar contra el backend real
    // this.carePlanService.getPatientCarePlans(this.patientId).subscribe({
    //   next: response => {
    //     this.carePlans = response.carePlans || [];
    //     this.totalCarePlans = response.totalCarePlans || 0;
    //     this.loading = false;
    //     if (this.carePlans.length) {
    //       const highlightedPlan = this.highlightedPlanId
    //         ? this.carePlans.find(plan => plan.id === this.highlightedPlanId)
    //         : null;
    //       const activePlan = this.carePlans.find(plan => String(plan.status).toUpperCase() === 'ACTIVE');
  //       this.selectPlan(highlightedPlan || activePlan || this.carePlans[0]);
  //       this.applyHighlight();
  //     }
  //   },
  //   error: error => {
  //     this.errorMsg = error?.error?.message || 'carePlans.errors.load';
  //     this.loading = false;
  //   }
  // });

  // 🔧 MOCK TEMPORAL — quitar cuando puedas probar contra el backend real
  setTimeout(() => {
    const mockPlan: CarePlan = {
      id: 1,
      specialistId: 100,
      specialistName: 'Dra. María Sánchez',
      patientId: this.patientId,
      patientName: 'Juan Pérez',
      title: 'Plan de manejo de ansiedad',
      therapeuticObjectives: 'Reducir episodios de ansiedad mediante técnicas de respiración y mindfulness.',
      generalRecommendations: 'Practicar ejercicios de respiración diariamente y mantener rutina de sueño.',
      professionalObservations: 'Buena adherencia al plan en las primeras semanas.',
      status: 'ACTIVE',
      progressPercentage: 50,
      reviewDate: '2026-06-25',
      reviewStartTime: '10:00:00',
      reviewEndTime: '11:00:00',
      createdDate: '2026-05-01T10:00:00',
      items: [
        { id: 1, title: 'Meditar 10 minutos diarios', description: 'Sesión guiada por la mañana', itemType: 'ACTIVITY', status: 'COMPLETED', dueDate: '2026-06-01', completedDate: '2026-06-01' },
        { id: 2, title: 'Registrar diario emocional', description: 'Anotar estado de ánimo cada noche', itemType: 'ACTIVITY', status: 'PENDING', dueDate: '2026-06-01', completedDate: null },
        { id: 3, title: 'Ejercicio de respiración 4-7-8', description: 'Practicar antes de dormir', itemType: 'ACTIVITY', status: 'PENDING', dueDate: '2026-06-20', completedDate: null },
        { id: 4, title: 'Identificar lista de triggers', description: 'Situaciones que generan ansiedad', itemType: 'OBJECTIVE', status: 'COMPLETED', dueDate: '2026-06-10', completedDate: '2026-06-09' }
      ]
    };

    this.carePlans = [mockPlan];
    this.totalCarePlans = 1;
    this.loading = false;
    this.selectPlan(mockPlan);
  }, 500);
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
}
