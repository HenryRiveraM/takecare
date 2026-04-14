import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService, Patient, Specialist, PendingValidationUser, User } from '../../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
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

  showDeleteConfirm = false;
  deleteTarget: { type: 'patient' | 'specialist'; id: number; name: string } | null = null;
  notification: { message: string; type: 'success' | 'error' } | null = null;

  constructor(private adminService: AdminService) {}

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
        this.errorMsg = 'Error cargando pacientes';
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
        this.filteredValidations = [...data];
        this.loadingValidations = false;
        console.log(`✅ ${data.length} validaciones pendientes cargadas`);
      },
      error: (err) => {
        this.pendingValidations = [];
        this.filteredValidations = [];
        this.loadingValidations = false;
        this.errorMsg = 'No se pudieron cargar las validaciones pendientes';
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
        const roleText = user.role === 2 ? 'Especialista' : 'Paciente';
        this.showNotification(`${roleText} ${status === 'approved' ? 'aprobado' : 'rechazado'} correctamente`);
        this.loadPendingValidations();
        this.loadPatients();
        this.loadSpecialists();
      },
      error: (err) => {
        this.errorMsg = 'No se pudo completar la operación en el servidor';
        console.error(err);
        this.showNotification('No se pudo procesar la validación', 'error');
      }
    });
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
    this.showDeleteConfirm = false;
    this.deleteTarget = null;
  }

  confirmDelete(): void {
  if (!this.deleteTarget) return;

  const { type, id } = this.deleteTarget;
  
  const list = type === 'patient' ? this.patients : this.specialists;
  const user = list.find(u => u.id === id);
  const isCurrentlyActive = user?.status === 1;

  const service$ = type === 'patient'
    ? this.adminService.deletePatient(id)
    : this.adminService.deleteSpecialist(id);

  service$.subscribe({
    next: () => {
      const newStatus = isCurrentlyActive ? 0 : 1;
      const actionLabel = newStatus === 1 ? 'activado' : 'suspendido';

      if (type === 'patient') {
        this.patients = this.patients.map(u => u.id === id ? { ...u, status: newStatus } : u);
        this.filteredPatients = this.filteredPatients.map(u => u.id === id ? { ...u, status: newStatus } : u);
      } else {
        this.specialists = this.specialists.map(u => u.id === id ? { ...u, status: newStatus } : u);
        this.filteredSpecialists = this.filteredSpecialists.map(u => u.id === id ? { ...u, status: newStatus } : u);
      }

      this.cancelDelete();
      this.showNotification(`${type === 'patient' ? 'Paciente' : 'Especialista'} ${actionLabel} correctamente`);
    },
    error: (err: HttpErrorResponse) => {
      this.errorMsg = `No se pudo cambiar el estado del ${type === 'patient' ? 'paciente' : 'especialista'}`;
      console.error(err);
      this.cancelDelete();
      this.showNotification(`Error al procesar la solicitud`, 'error');
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
    if (!url) return false;
    return url.toLowerCase().includes('.pdf');
  }

  togglePatientStatus(patient: Patient): void {
    const newStatus: 0 | 1 = patient.status === 1 ? 0 : 1;

    this.adminService.updateUserStatus(patient.id, newStatus).subscribe({
      next: () => {
        patient.status = newStatus;
        this.showNotification(
          newStatus === 1
            ? 'Paciente activado correctamente'
            : 'Paciente suspendido correctamente'
        );
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'No se pudo cambiar el estado del paciente';
        this.showNotification('No se pudo actualizar el estado del paciente', 'error');
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
            ? 'Especialista activado correctamente'
            : 'Especialista suspendido correctamente'
        );
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'No se pudo cambiar el estado del especialista';
        this.showNotification('No se pudo actualizar el estado del especialista', 'error');
      }
    });
  }

  private showNotification(message: string, type: 'success' | 'error' = 'success'): void {
    this.notification = { message, type };
    setTimeout(() => {
      this.notification = null;
    }, 3000);
  }
}