import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Patient, Specialist } from '../../services/admin.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {
  patients: Patient[] = [];
  specialists: Specialist[] = [];
  pendingValidations: any[] = [];

  filteredPatients: Patient[] = [];
  filteredSpecialists: Specialist[] = [];
  filteredValidations: any[] = [];

  activeTab: 'patients' | 'specialists' | 'validations' = 'patients';
  searchTerm: string = '';

  loadingPatients = false;
  loadingSpecialists = false;
  loadingValidations = false;
  errorMsg = '';

  showDeleteConfirm = false;
  deleteTarget: { type: 'patient' | 'specialist'; id: number; name: string } | null = null;
  notification: { message: string, type: 'success' | 'error' } | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.loadPatients();
    this.loadSpecialists();
    this.loadPendingValidations();
  }

  loadPatients(): void {
    this.loadingPatients = true;
    this.adminService.getPatients().subscribe({
      next: (data: Patient[]) => {
        this.patients = data.filter(patient => this.isActiveUser(patient.status));
        this.filteredPatients = [...this.patients];
        this.loadingPatients = false;
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg = 'No se pudieron cargar los pacientes';
        this.loadingPatients = false;
        console.error(err);
      }
    });
  }

  loadSpecialists(): void {
    this.loadingSpecialists = true;
    this.adminService.getSpecialists().subscribe({
      next: (data: Specialist[]) => {
        this.specialists = data.filter(specialist => this.isActiveUser(specialist.status));
        this.filteredSpecialists = [...this.specialists];
        this.loadingSpecialists = false;
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg = 'No se pudieron cargar los especialistas';
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
        this.filteredValidations = [...this.pendingValidations];
        this.loadingValidations = false;
      },
      error: (err) => {
        this.pendingValidations = [];
        this.filteredValidations = [];
        this.loadingValidations = false;
        console.warn('No se pudieron cargar las validaciones pendientes', err);
      }
    });
  }

  processValidation(id: number, status: 'approved' | 'rejected'): void {
    const user = this.pendingValidations.find(u => u.id === id);
    if (!user) return;

    this.adminService.validateUser(id, status).subscribe({
      next: () => {
        const roleText = user.role === 'specialist' ? 'Especialista' : 'Paciente';
        this.showNotification(`${roleText} ${status === 'approved' ? 'validado' : 'rechazado'} correctamente`);
        this.loadPendingValidations();
        if (status === 'approved') {
          user.role === 'specialist' ? this.loadSpecialists() : this.loadPatients();
        }
      },
      error: (err) => {
        this.errorMsg = 'No se pudo completar la operación en el servidor';
        console.error(err);
      }
    });
  }

  showNotification(msg: string, type: 'success' | 'error' = 'success') {
    this.notification = { message: msg, type };
    setTimeout(() => {
      this.notification = null;
    }, 3000);
  }

  setTab(tab: 'patients' | 'specialists' | 'validations'): void {
    this.activeTab = tab;
    this.searchTerm = '';
    this.onSearch(); 
    this.resetFilters();
    this.errorMsg = '';
  }

  resetFilters(): void {
    this.filteredPatients = [...this.patients];
    this.filteredSpecialists = [...this.specialists];
    this.filteredValidations = [...this.pendingValidations];
  }

  onSearch(): void {
    const term = this.searchTerm.toLowerCase().trim();

    if (this.activeTab === 'patients') {
      this.filteredPatients = this.patients.filter(patient =>
        patient.names.toLowerCase().includes(term) ||
        patient.firstLastname.toLowerCase().includes(term) ||
        (patient.secondLastname?.toLowerCase().includes(term) ?? false) ||
        patient.email.toLowerCase().includes(term)
      );
    } else if (this.activeTab === 'specialists') {
      this.filteredSpecialists = this.specialists.filter(specialist =>
        specialist.names.toLowerCase().includes(term) ||
        specialist.firstLastname.toLowerCase().includes(term) ||
        (specialist.secondLastname?.toLowerCase().includes(term) ?? false) ||
        specialist.email.toLowerCase().includes(term)
      );
    } else if (this.activeTab === 'validations') {
      const term = this.searchTerm.toLowerCase().trim(); // Usar el mismo formato
      this.filteredValidations = this.pendingValidations.filter(v => 
        v.names.toLowerCase().includes(term) || 
        v.firstLastname.toLowerCase().includes(term) ||
        v.email.toLowerCase().includes(term)
      );
    }
  }

  deletePatient(id: number, fullName: string): void {
    this.errorMsg = '';
    this.deleteTarget = { type: 'patient', id, name: fullName };
    this.showDeleteConfirm = true;
  }

  deleteSpecialist(id: number, fullName: string): void {
    this.errorMsg = '';
    this.deleteTarget = { type: 'specialist', id, name: fullName };
    this.showDeleteConfirm = true;
  }

  cancelDelete(): void {
    this.closeDeleteConfirm();
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;

    const { type, id } = this.deleteTarget;
    const label = type === 'patient' ? 'Paciente' : 'Especialista';
    const service$ = type === 'patient'
      ? this.adminService.deletePatient(id)
      : this.adminService.deleteSpecialist(id);

    service$.subscribe({
      next: () => {
        if (type === 'patient') {
          this.patients = this.patients.filter(item => item.id !== id);
          this.filteredPatients = this.filteredPatients.filter(item => item.id !== id);
        } else {
          this.specialists = this.specialists.filter(item => item.id !== id);
          this.filteredSpecialists = this.filteredSpecialists.filter(item => item.id !== id);
        }

        this.closeDeleteConfirm();
        this.showNotification(`${label} suspendido correctamente`, 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg = `No se pudo suspender el ${type === 'patient' ? 'paciente' : 'especialista'}`;
        console.error(err);
        this.closeDeleteConfirm();
        this.showNotification(`No se pudo suspender el ${label.toLowerCase()}`, 'error');
      }
    });
  }

  private isActiveUser(status: number | boolean | null | undefined): boolean {
    return status !== 0 && status !== false;
  }

  private closeDeleteConfirm(): void {
    this.showDeleteConfirm = false;
    this.deleteTarget = null;
  }

  get isLoading(): boolean {
    return this.loadingPatients || this.loadingSpecialists || this.loadingValidations;
  }
}