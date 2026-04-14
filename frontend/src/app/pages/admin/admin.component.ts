import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LocalizedDatePipe } from '../../shared/pipes/localized-date.pipe';
import {
  AdminService,
  Patient,
  Specialist,
  PendingValidationUser,
  User
} from '../../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, LocalizedDatePipe],
  templateUrl:'./admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {
  patients: Patient[] = [];
  filteredPatients: Patient[] = [];

  specialists: Specialist[] = [];
  filteredSpecialists: Specialist[] = [];

  pendingValidations: PendingValidationUser[] = [];
  filteredValidations: PendingValidationUser[] = [];

  activeTab: 'patients' | 'specialists' | 'validations' = 'patients';
  searchTerm = '';

  loadingPatients = false;
  loadingSpecialists = false;
  loadingValidations = false;
  errorMsg = '';

  notification: { message: string; type: 'success' | 'error' } | null = null;

  constructor(
    private adminService: AdminService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.errorMsg = '';

    if (this.activeTab === 'patients') {
      this.loadPatients();
      return;
    }

    if (this.activeTab === 'specialists') {
      this.loadSpecialists();
      return;
    }

    this.loadPendingValidations();
  }

  loadPatients(): void {
    this.loadingPatients = true;

    this.adminService.getPatients().subscribe({
      next: (data) => {
        this.patients = data;
        this.filteredPatients = [...this.patients];
        this.loadingPatients = false;
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg = this.translate.instant('admin.errors.loadPatients');
        this.loadingPatients = false;
        console.error(err);
      }
    });
  }

  loadSpecialists(): void {
    this.loadingSpecialists = true;

    this.adminService.getSpecialists().subscribe({
      next: (data) => {
        this.specialists = data;
        this.filteredSpecialists = [...this.specialists];
        this.loadingSpecialists = false;
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg = this.translate.instant('admin.errors.loadSpecialists');
        this.loadingSpecialists = false;
        console.error(err);
      }
    });
  }

  loadPendingValidations(): void {
    this.loadingValidations = true;

    this.adminService.getPendingValidations().subscribe({
      next: (data) => {
        this.pendingValidations = data;
        this.filteredValidations = [...data];
        this.loadingValidations = false;
      },
      error: (err) => {
        this.pendingValidations = [];
        this.filteredValidations = [];
        this.loadingValidations = false;
        this.errorMsg = this.translate.instant('admin.errors.loadValidations');
        console.error(err);
      }
    });
  }

  setTab(tab: 'patients' | 'specialists' | 'validations'): void {
    this.activeTab = tab;
    this.searchTerm = '';
    this.loadData();
  }

  onSearch(): void {
    const term = this.searchTerm.toLowerCase().trim();

    if (this.activeTab === 'patients') {
      this.filteredPatients = this.patients.filter(user => this.matchesSearch(user, term));
      return;
    }

    if (this.activeTab === 'specialists') {
      this.filteredSpecialists = this.specialists.filter(user => this.matchesSearch(user, term));
      return;
    }

    this.filteredValidations = this.pendingValidations.filter(user => this.matchesSearch(user, term));
  }

  private matchesSearch(user: User, term: string): boolean {
    const fullName = `${user.names} ${user.firstLastname} ${user.secondLastname || ''}`.toLowerCase();
    return (
      fullName.includes(term) ||
      user.email.toLowerCase().includes(term) ||
      (user.ciNumber || '').toLowerCase().includes(term)
    );
  }

  processValidation(user: PendingValidationUser, status: 'approved' | 'rejected'): void {
    this.adminService.validateUser(user.id, user.role, status).subscribe({
      next: () => {
        const roleText =
          user.role === 2
            ? this.translate.instant('admin.roles.specialist')
            : this.translate.instant('admin.roles.patient');

        const actionText =
          status === 'approved'
            ? this.translate.instant('admin.notifications.validated')
            : this.translate.instant('admin.notifications.rejected');

        this.showNotification(`${roleText} ${actionText}`);
        this.loadPendingValidations();
        this.loadPatients();
        this.loadSpecialists();
      },
      error: (err) => {
        this.errorMsg = this.translate.instant('admin.errors.processValidation');
        console.error(err);
        this.showNotification(this.translate.instant('admin.errors.processValidation'), 'error');
      }
    });
  }

  togglePatientStatus(patient: Patient): void {
    const newStatus: 0 | 1 = patient.status === 1 ? 0 : 1;

    this.adminService.updateUserStatus(patient.id, newStatus).subscribe({
      next: () => {
        patient.status = newStatus;
        this.showNotification(
          newStatus === 1
            ? this.translate.instant('admin.notifications.patientActivated')
            : this.translate.instant('admin.notifications.patientSuspended')
        );
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = this.translate.instant('admin.errors.suspendUser');
        this.showNotification(this.translate.instant('admin.errors.suspendUser'), 'error');
      }
    });
  }

  toggleSpecialistStatus(specialist: Specialist): void {
    const newStatus: 0 | 1 = specialist.status === 1 ? 0 : 1;

    this.adminService.updateUserStatus(specialist.id, newStatus).subscribe({
      next: () => {
        specialist.status = newStatus;
        this.showNotification(
          newStatus === 1
            ? this.translate.instant('admin.notifications.specialistActivated')
            : this.translate.instant('admin.notifications.specialistSuspended')
        );
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = this.translate.instant('admin.errors.suspendUser');
        this.showNotification(this.translate.instant('admin.errors.suspendUser'), 'error');
      }
    });
  }

  getAge(birthDate: string): number {
    if (!birthDate) return 0;

    const birth = new Date(birthDate);
    const today = new Date();

    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }

    return age;
  }

  isPdf(url?: string): boolean {
    return !!url && url.toLowerCase().includes('.pdf');
  }

  private showNotification(message: string, type: 'success' | 'error' = 'success'): void {
    this.notification = { message, type };
    setTimeout(() => {
      this.notification = null;
    }, 3000);
  }
}