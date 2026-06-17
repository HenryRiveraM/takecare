import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import {
  CarePlan,
  CarePlanActivityPayload,
  CarePlanItem,
  CarePlanItemType,
  CarePlanService,
  CarePlanStatus,
  CreateCarePlanPayload,
  UpdateCarePlanItemPayload,
  UpdateCarePlanPayload
} from '../../services/care-plan.service';
import { AuthService } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { CarePlanProgressDashboardComponent } from '../../shared/care-plan-progress-dashboard/care-plan-progress-dashboard.component';
import { CarePlanLogbookComponent } from '../../shared/care-plan-logbook/care-plan-logbook.component';

type FormMode = 'create' | 'edit';
type ActivityFormMode = 'create' | 'edit';

interface CarePlanForm {
  title: string;
  therapeuticObjectives: string;
  generalRecommendations: string;
  professionalObservations: string;
  reviewDate: string;
  reviewStartTime: string;
  reviewEndTime: string;
  status: CarePlanStatus;
  progressPercentage: number;
  items: CarePlanItemForm[];
}

interface CarePlanItemForm {
  title: string;
  description: string;
  itemType: CarePlanItemType;
  dueDate: string;
}

interface ActivityForm {
  title: string;
  description: string;
  dueDate: string;
}

@Component({
  selector: 'app-specialist-care-plans',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent, CarePlanProgressDashboardComponent, CarePlanLogbookComponent],
  templateUrl: './specialist-care-plans.component.html',
  styleUrls: ['./specialist-care-plans.component.css']
})
export class SpecialistCarePlansComponent implements OnInit, OnDestroy {

  specialistId = 0;
  patientId = 0;
  patientName = '';
  isPatientScoped = false;
  searchTerm = '';
  highlightedPlanId: number | null = null;

  carePlans: CarePlan[] = [];
  selectedPlan: CarePlan | null = null;
  totalCarePlans = 0;

  loading = false;
  saving = false;
  errorMsg = '';
  formError = '';

  showForm = false;
  formMode: FormMode = 'create';
  showActivityForm = false;
  activityFormMode: ActivityFormMode = 'create';
  activityForm: ActivityForm = this.emptyActivityForm();
  activityFormError = '';
  activitySaving = false;
  editingActivity: CarePlanItem | null = null;
  activityPlan: CarePlan | null = null;
  showCancelledActivities = false;
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  today = this.formatLocalDate(new Date());
  minReviewDate = this.formatLocalDate(this.addDays(new Date(), 1));
  confirmDeletePlan: CarePlan | null = null;
  confirmArchivePlan: CarePlan | null = null;
  private toastTimer: any;
  private highlightTimer: any;
  private editReviewSnapshot = {
    reviewDate: '',
    reviewStartTime: '',
    reviewEndTime: ''
  };

  readonly limits = {
    title: 150,
    therapeuticObjectives: 1000,
    generalRecommendations: 1000,
    professionalObservations: 700,
    itemTitle: 150,
    itemDescription: 500
  };

  readonly statuses: Array<{ value: CarePlanStatus; label: string }> = [
    { value: 'ACTIVE', label: 'carePlans.status.active' },
    { value: 'PAUSED', label: 'carePlans.status.paused' },
    { value: 'COMPLETED', label: 'carePlans.status.completed' },
    { value: 'CANCELLED', label: 'carePlans.status.cancelled' }
  ];

  readonly itemTypes: Array<{ value: CarePlanItemType; label: string }> = [
    { value: 'ACTIVITY', label: 'carePlans.itemTypes.activity' },
    { value: 'OBJECTIVE', label: 'carePlans.itemTypes.objective' },
    { value: 'RECOMMENDATION', label: 'carePlans.itemTypes.recommendation' }
  ];

  form: CarePlanForm = this.emptyForm();

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private carePlanService: CarePlanService,
    private route: ActivatedRoute,
    private router: Router,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.specialistId = Number(this.authService.getUser()?.id || 0);
    this.patientId = Number(this.route.snapshot.paramMap.get('patientId') || 0);
    this.isPatientScoped = this.patientId > 0;
    this.patientName = this.route.snapshot.queryParamMap.get('patientName') || 'carePlans.patientFallback';
    this.highlightedPlanId = Number(this.route.snapshot.queryParamMap.get('highlightPlanId') || 0) || null;

    if (!this.specialistId) {
      this.errorMsg = 'carePlans.errors.noSpecialist';
      return;
    }

    this.loadCarePlans();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active');
    if (this.toastTimer) clearTimeout(this.toastTimer);
    if (this.highlightTimer) clearTimeout(this.highlightTimer);
  }

  openCreateForm(): void {
    if (!this.isPatientScoped) {
      this.router.navigate(['/specialist/patients']);
      return;
    }

    this.formMode = 'create';
    this.form = this.emptyForm();
    this.formError = '';
    this.showForm = true;
  }

  openEditForm(plan: CarePlan): void {
    if (this.isPlanCompleted(plan)) {
      this.showToastMessage('carePlans.toast.completedPlanLocked', 'error');
      return;
    }

    if (!plan.therapeuticObjectives || !plan.generalRecommendations) {
      this.carePlanService.getCarePlanById(plan.id, this.specialistId).subscribe({
        next: detail => this.openEditForm(detail),
        error: error => this.showToastMessage(error?.error?.message || 'carePlans.toast.detailError', 'error')
      });
      return;
    }

    this.formMode = 'edit';
    const reviewDate = plan.reviewDate || '';
    const reviewStartTime = plan.reviewStartTime?.substring(0, 5) || '';
    const reviewEndTime = plan.reviewEndTime?.substring(0, 5) || '';
    this.editReviewSnapshot = { reviewDate, reviewStartTime, reviewEndTime };
    this.form = {
      title: plan.title || '',
      therapeuticObjectives: plan.therapeuticObjectives || '',
      generalRecommendations: plan.generalRecommendations || '',
      professionalObservations: plan.professionalObservations || '',
      reviewDate,
      reviewStartTime,
      reviewEndTime,
      status: plan.status || 'ACTIVE',
      progressPercentage: plan.progressPercentage ?? 0,
      items: []
    };
    this.selectedPlan = plan;
    this.formError = '';
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.saving = false;
    this.formError = '';
  }

  openCreateActivityForm(plan: CarePlan): void {
    if (this.isPlanCompleted(plan)) {
      this.showToastMessage('carePlans.toast.completedPlanLocked', 'error');
      return;
    }

    this.activityPlan = plan;
    this.editingActivity = null;
    this.activityFormMode = 'create';
    this.activityForm = this.emptyActivityForm();
    this.activityFormError = '';
    this.showActivityForm = true;
  }

  openEditActivityForm(plan: CarePlan, activity: CarePlanItem): void {
    if (this.isPlanCompleted(plan)) {
      this.showToastMessage('carePlans.toast.completedPlanLocked', 'error');
      return;
    }
    if (this.isActivityCompleted(activity)) {
      this.showToastMessage('carePlans.activities.toast.completedLocked', 'error');
      return;
    }

    this.activityPlan = plan;
    this.editingActivity = activity;
    this.activityFormMode = 'edit';
    this.activityForm = {
      title: activity.title || '',
      description: activity.description || '',
      dueDate: activity.dueDate || ''
    };
    this.activityFormError = '';
    this.showActivityForm = true;
  }

  closeActivityForm(): void {
    this.showActivityForm = false;
    this.activitySaving = false;
    this.activityFormError = '';
    this.editingActivity = null;
    this.activityPlan = null;
  }

  activeActivities(plan: CarePlan | null): CarePlanItem[] {
    return (plan?.items || []).filter(item => item.status === 'PENDING' || item.status === 'COMPLETED');
  }

  cancelledActivities(plan: CarePlan | null): CarePlanItem[] {
    return (plan?.items || []).filter(item => item.status === 'CANCELLED');
  }

  selectPlan(plan: CarePlan): void {
    this.carePlanService.getCarePlanById(plan.id, this.specialistId).subscribe({
      next: response => {
        this.selectedPlan = response;
        this.carePlans = this.carePlans.map(item => item.id === response.id ? response : item);
      },
      error: error => {
        this.showToastMessage(error?.error?.message || 'carePlans.toast.detailError', 'error');
      }
    });
  }

  goToPlanDetails(plan: CarePlan, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/specialist/care-plans'], {
      queryParams: { highlightPlanId: plan.id }
    });
  }

  savePlan(): void {
    if (!this.validateForm()) {
      return;
    }

    this.saving = true;

    if (this.formMode === 'create') {
      const payload: CreateCarePlanPayload = {
        title: this.form.title.trim(),
        therapeuticObjectives: this.form.therapeuticObjectives.trim(),
        generalRecommendations: this.form.generalRecommendations.trim(),
        professionalObservations: this.cleanOptional(this.form.professionalObservations),
        reviewDate: this.form.reviewDate,
        reviewStartTime: this.form.reviewStartTime,
        reviewEndTime: this.form.reviewEndTime,
        items: this.form.items.map(item => ({
          title: item.title.trim(),
          description: this.cleanOptional(item.description),
          itemType: item.itemType,
          dueDate: item.dueDate || null
        }))
      };

      this.carePlanService.createCarePlan(this.specialistId, this.patientId, payload).subscribe({
        next: plan => {
          this.saving = false;
          this.closeForm();
          this.selectedPlan = plan;
          this.loadCarePlans();
          this.showToastMessage('carePlans.toast.created', 'success');
        },
        error: error => this.handleSaveError(error, 'carePlans.toast.createError')
      });

      return;
    }

    if (!this.selectedPlan) {
      this.saving = false;
      return;
    }

    if (this.isPlanCompleted(this.selectedPlan)) {
      this.saving = false;
      this.formError = 'carePlans.messages.completedLocked';
      this.showToastMessage('carePlans.toast.completedPlanLocked', 'error');
      return;
    }

    const payload: UpdateCarePlanPayload = {
      title: this.form.title.trim(),
      therapeuticObjectives: this.form.therapeuticObjectives.trim(),
      generalRecommendations: this.form.generalRecommendations.trim(),
      professionalObservations: this.cleanOptional(this.form.professionalObservations),
      status: this.form.status
    };

    if (this.hasReviewChanged()) {
      payload.reviewDate = this.form.reviewDate;
      payload.reviewStartTime = this.form.reviewStartTime;
      payload.reviewEndTime = this.form.reviewEndTime;
    }

    this.carePlanService.updateCarePlan(this.selectedPlan.id, this.specialistId, payload).subscribe({
      next: plan => {
        this.saving = false;
        this.closeForm();
        this.selectedPlan = plan;
        this.carePlans = this.carePlans.map(item => item.id === plan.id ? plan : item);
        this.showToastMessage(
          this.hasReviewChanged() ? 'carePlans.toast.updatedWithReview' : 'carePlans.toast.updated',
          'success'
        );
      },
      error: error => this.handleSaveError(error, 'carePlans.toast.updateError')
    });
  }

  getStatusLabel(status: CarePlanStatus | string): string {
    const normalized = String(status || '').toUpperCase();
    const option = this.statuses.find(item => item.value === normalized);
    return option?.label || 'carePlans.status.unknown';
  }

  get filteredCarePlans(): CarePlan[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.carePlans;
    }

    return this.carePlans.filter(plan => {
      const statusKey = this.getStatusLabel(plan.status).toLowerCase();
      return [
        plan.title,
        plan.patientName,
        plan.status,
        statusKey
      ]
        .filter(Boolean)
        .some(value => String(value).toLowerCase().includes(term));
    });
  }

  asLocalDate(value: string | null | undefined): Date | null {
    return value ? new Date(`${value}T00:00:00`) : null;
  }

  addFormItem(): void {
    this.form.items.push({
      title: '',
      description: '',
      itemType: 'ACTIVITY',
      dueDate: ''
    });
  }

  removeFormItem(index: number): void {
    this.form.items.splice(index, 1);
  }

  saveActivity(): void {
    if (!this.validateActivityForm() || !this.activityPlan) {
      return;
    }

    this.activitySaving = true;

    const payload: CarePlanActivityPayload = {
      title: this.activityForm.title.trim(),
      description: this.cleanOptional(this.activityForm.description),
      dueDate: this.activityForm.dueDate || null
    };

    if (this.activityFormMode === 'create') {
      this.carePlanService.createActivity(this.activityPlan.id, this.specialistId, payload).subscribe({
        next: activity => {
          this.activitySaving = false;
          this.upsertActivity(this.activityPlan!.id, activity);
          this.closeActivityForm();
          this.showToastMessage('carePlans.activities.toast.created', 'success');
        },
        error: error => this.handleActivityError(error, 'carePlans.activities.toast.createError')
      });
      return;
    }

    if (!this.editingActivity) {
      this.activitySaving = false;
      return;
    }

    const updatePayload: UpdateCarePlanItemPayload = {
      ...payload,
      status: this.editingActivity.status
    };

    this.carePlanService.updateActivity(this.editingActivity.id, this.specialistId, updatePayload).subscribe({
      next: activity => {
        this.activitySaving = false;
        this.upsertActivity(this.activityPlan!.id, activity);
        this.closeActivityForm();
        this.showToastMessage('carePlans.activities.toast.updated', 'success');
      },
      error: error => this.handleActivityError(error, 'carePlans.activities.toast.updateError')
    });
  }

  cancelActivity(plan: CarePlan, activity: CarePlanItem, event?: Event): void {
    event?.stopPropagation();
    if (this.isPlanCompleted(plan)) {
      this.showToastMessage('carePlans.toast.completedPlanLocked', 'error');
      return;
    }
    if (this.isActivityCompleted(activity)) {
      this.showToastMessage('carePlans.activities.toast.completedLocked', 'error');
      return;
    }

    if (plan.status === 'CANCELLED') {
      this.showToastMessage('carePlans.activities.toast.cancelError', 'error');
      return;
    }

    if (!window.confirm(this.translate.instant('carePlans.activities.confirmCancel'))) {
      return;
    }

    const payload: UpdateCarePlanItemPayload = {
      status: 'CANCELLED'
    };

    this.carePlanService.updateActivity(activity.id, this.specialistId, payload).subscribe({
      next: updatedActivity => {
        this.upsertActivity(plan.id, updatedActivity);
        this.showToastMessage('carePlans.activities.toast.cancelled', 'success');
      },
      error: error => this.showToastMessage(error?.error?.message || 'carePlans.activities.toast.updateError', 'error')
    });
  }

  restoreActivity(plan: CarePlan, activity: CarePlanItem, event?: Event): void {
    event?.stopPropagation();
    if (this.isPlanCompleted(plan)) {
      this.showToastMessage('carePlans.toast.completedPlanLocked', 'error');
      return;
    }

    if (plan.status === 'CANCELLED') {
      this.showToastMessage('carePlans.activities.toast.restoreError', 'error');
      return;
    }

    const payload: UpdateCarePlanItemPayload = {
      status: 'PENDING'
    };

    this.carePlanService.updateActivity(activity.id, this.specialistId, payload).subscribe({
      next: updatedActivity => {
        this.upsertActivity(plan.id, updatedActivity);
        this.showToastMessage('carePlans.activities.toast.restored', 'success');
      },
      error: error => this.showToastMessage(error?.error?.message || 'carePlans.activities.toast.restoreError', 'error')
    });
  }

  openDeleteConfirm(plan: CarePlan, event?: Event): void {
    event?.stopPropagation();
    if (this.isPlanCompleted(plan)) {
      this.openArchiveConfirm(plan, event);
      return;
    }

    this.confirmDeletePlan = plan;
  }

  closeDeleteConfirm(): void {
    this.confirmDeletePlan = null;
  }

  openArchiveConfirm(plan: CarePlan, event?: Event): void {
    event?.stopPropagation();
    this.confirmArchivePlan = plan;
  }

  closeArchiveConfirm(): void {
    this.confirmArchivePlan = null;
  }

  deleteSelectedPlan(): void {
    if (!this.confirmDeletePlan) {
      return;
    }

    const planId = this.confirmDeletePlan.id;
    this.carePlanService.deleteCarePlan(planId, this.specialistId).subscribe({
      next: () => {
        this.carePlans = this.carePlans.filter(plan => plan.id !== planId);
        this.totalCarePlans = this.carePlans.length;
        if (this.selectedPlan?.id === planId) {
          this.selectedPlan = null;
          if (this.carePlans.length) {
            this.selectPlan(this.carePlans[0]);
          }
        }
        this.closeDeleteConfirm();
        this.showToastMessage('carePlans.toast.deleted', 'success');
      },
      error: () => {
        this.closeDeleteConfirm();
        this.showToastMessage('carePlans.toast.deleteError', 'error');
      }
    });
  }

  getReviewDayLabel(): string {
    if (!this.form.reviewDate) {
      return '';
    }

    const date = new Date(`${this.form.reviewDate}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const keys = [
      'carePlans.days.sunday',
      'carePlans.days.monday',
      'carePlans.days.tuesday',
      'carePlans.days.wednesday',
      'carePlans.days.thursday',
      'carePlans.days.friday',
      'carePlans.days.saturday'
    ];
    return keys[date.getDay()];
  }

  private loadCarePlans(): void {
    this.loading = true;
    this.errorMsg = '';

    const request$ = this.isPatientScoped
      ? this.carePlanService.getCarePlansByPatient(this.specialistId, this.patientId)
      : this.carePlanService.getCarePlansBySpecialist(this.specialistId);

    request$.subscribe({
      next: response => {
        const plans = response.carePlans || [];
        this.totalCarePlans = response.totalCarePlans || 0;

        if (!this.isPatientScoped && plans.length) {
          forkJoin(plans.map(plan => this.carePlanService.getCarePlanById(plan.id, this.specialistId))).subscribe({
            next: detailedPlans => {
              this.carePlans = detailedPlans;
              this.loading = false;
              this.selectedPlan = detailedPlans[0] || null;
              this.applyHighlight();
            },
            error: error => {
              this.errorMsg = error?.error?.message || 'carePlans.errors.load';
              this.loading = false;
            }
          });
          return;
        }

        this.carePlans = plans;
        this.loading = false;
        if (!this.carePlans.length) {
          this.selectedPlan = null;
        }
      },
      error: error => {
        this.errorMsg = error?.error?.message || 'carePlans.errors.load';
        this.loading = false;
      }
    });
  }

  private validateForm(): boolean {
    this.formError = '';

    if (!this.form.title.trim()) {
      this.formError = 'carePlans.validation.titleRequired';
      return false;
    }

    if (!this.form.therapeuticObjectives.trim()) {
      this.formError = 'carePlans.validation.objectivesRequired';
      return false;
    }

    if (!this.form.generalRecommendations.trim()) {
      this.formError = 'carePlans.validation.recommendationsRequired';
      return false;
    }

    if (this.formMode === 'create') {
      if (!this.form.reviewDate) {
        this.formError = 'carePlans.validation.reviewDateRequired';
        return false;
      }

      if (this.form.reviewDate < this.minReviewDate) {
        this.formError = 'carePlans.validation.reviewDateAtLeast24h';
        return false;
      }

      if (!this.form.reviewStartTime) {
        this.formError = 'carePlans.validation.reviewStartTimeRequired';
        return false;
      }

      if (!this.form.reviewEndTime) {
        this.formError = 'carePlans.validation.reviewEndTimeRequired';
        return false;
      }

      if (this.form.reviewEndTime <= this.form.reviewStartTime) {
        this.formError = 'carePlans.validation.reviewEndAfterStart';
        return false;
      }

      if (!this.hasAtLeast24HoursNotice()) {
        this.formError = 'carePlans.validation.reviewDateAtLeast24h';
        return false;
      }

      if (!this.form.items.length) {
        this.formError = 'carePlans.validation.itemsRequired';
        return false;
      }

      if (this.form.items.some(item => !item.title.trim())) {
        this.formError = 'carePlans.validation.itemTitleRequired';
        return false;
      }

      if (this.form.items.some(item => item.dueDate && item.dueDate < this.today)) {
        this.formError = 'carePlans.validation.itemDueDateNotPast';
        return false;
      }
    }

    if (this.formMode === 'edit' && this.hasReviewChanged()) {
      if (!this.form.reviewDate || !this.form.reviewStartTime || !this.form.reviewEndTime) {
        this.formError = 'carePlans.validation.reviewScheduleRequired';
        return false;
      }

      if (this.form.reviewDate < this.today) {
        this.formError = 'carePlans.validation.reviewDateNotPast';
        return false;
      }

      if (this.form.reviewEndTime <= this.form.reviewStartTime) {
        this.formError = 'carePlans.validation.reviewEndAfterStart';
        return false;
      }
    }

    return true;
  }

  archiveSelectedPlan(): void {
    if (!this.confirmArchivePlan) {
      return;
    }

    const planId = this.confirmArchivePlan.id;
    this.carePlanService.archiveCarePlan(planId, this.specialistId).subscribe({
      next: () => {
        this.carePlans = this.carePlans.filter(plan => plan.id !== planId);
        this.totalCarePlans = this.carePlans.length;
        if (this.selectedPlan?.id === planId) {
          this.selectedPlan = this.carePlans[0] || null;
        }
        this.closeArchiveConfirm();
        this.showToastMessage('carePlans.toast.archived', 'success');
      },
      error: error => {
        this.closeArchiveConfirm();
        this.showToastMessage(error?.error?.message || 'carePlans.toast.archiveError', 'error');
      }
    });
  }

  isPlanCompleted(plan: CarePlan | null): boolean {
    return String(plan?.status || '').toUpperCase() === 'COMPLETED';
  }

  canManagePlan(plan: CarePlan | null): boolean {
    return !this.isPlanCompleted(plan);
  }

  isActivityCompleted(activity: CarePlanItem | null): boolean {
    return String(activity?.status || '').toUpperCase() === 'COMPLETED';
  }

  canManageActivity(activity: CarePlanItem | null): boolean {
    return !this.isActivityCompleted(activity);
  }

  private hasReviewChanged(): boolean {
    return this.form.reviewDate !== this.editReviewSnapshot.reviewDate
      || this.form.reviewStartTime !== this.editReviewSnapshot.reviewStartTime
      || this.form.reviewEndTime !== this.editReviewSnapshot.reviewEndTime;
  }

  private handleSaveError(error: any, fallbackKey: string): void {
    this.saving = false;
    const toastKey = error?.status === 409 ? 'carePlans.toast.scheduleConflict' : fallbackKey;
    this.formError = error?.error?.message || toastKey;
    this.showToastMessage(toastKey, 'error');
  }

  private handleActivityError(error: any, fallbackKey: string): void {
    this.activitySaving = false;
    this.activityFormError = error?.error?.message || fallbackKey;
    this.showToastMessage(fallbackKey, 'error');
  }

  private validateActivityForm(): boolean {
    this.activityFormError = '';

    if (!this.activityForm.title.trim()) {
      this.activityFormError = 'carePlans.activities.validation.titleRequired';
      return false;
    }

    if (this.activityForm.dueDate && this.activityForm.dueDate < this.today) {
      this.activityFormError = 'carePlans.activities.validation.dueDateNotPast';
      return false;
    }

    return true;
  }

  private upsertActivity(planId: number, activity: CarePlanItem): void {
    const nextProgress = activity.planProgressPercentage;

    const updatePlan = (plan: CarePlan): CarePlan => {
      if (plan.id !== planId) {
        return plan;
      }

      const items = plan.items || [];
      const exists = items.some(item => item.id === activity.id);
      const nextItems = exists
        ? items.map(item => item.id === activity.id ? activity : item)
        : [...items, activity];

      return {
        ...plan,
        progressPercentage: nextProgress ?? plan.progressPercentage,
        items: nextItems
      };
    };

    this.carePlans = this.carePlans.map(updatePlan);
    if (this.selectedPlan?.id === planId) {
      this.selectedPlan = updatePlan(this.selectedPlan);
    }
  }

  private showToastMessage(messageKey: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = messageKey;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 3000);
  }

  private applyHighlight(): void {
    if (!this.highlightedPlanId || this.isPatientScoped) {
      return;
    }

    setTimeout(() => {
      const element = document.getElementById(`care-plan-${this.highlightedPlanId}`);
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

  private cleanOptional(value: string): string | null {
    const clean = value.trim();
    return clean ? clean : null;
  }

  private formatLocalDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  private addDays(date: Date, days: number): Date {
    const nextDate = new Date(date);
    nextDate.setDate(nextDate.getDate() + days);
    return nextDate;
  }

  private hasAtLeast24HoursNotice(): boolean {
    const reviewStart = new Date(`${this.form.reviewDate}T${this.form.reviewStartTime}:00`);
    if (Number.isNaN(reviewStart.getTime())) {
      return false;
    }

    return reviewStart.getTime() >= Date.now() + (24 * 60 * 60 * 1000);
  }

  private emptyForm(): CarePlanForm {
    return {
      title: '',
      therapeuticObjectives: '',
      generalRecommendations: '',
      professionalObservations: '',
      reviewDate: '',
      reviewStartTime: '',
      reviewEndTime: '',
      status: 'ACTIVE',
      progressPercentage: 0,
      items: [
        {
          title: '',
          description: '',
          itemType: 'ACTIVITY',
          dueDate: ''
        }
      ]
    };
  }

  private emptyActivityForm(): ActivityForm {
    return {
      title: '',
      description: '',
      dueDate: ''
    };
  }

  get currentSpecialistName(): string {
    return this.authService.getUser()?.names || '';
  }
}
