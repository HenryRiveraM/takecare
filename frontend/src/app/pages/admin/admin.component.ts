import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminService, User, Specialist } from '../../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {
  // Datos generales (hotfix)
  users: User[] = [];
  filteredUsers: User[] = [];

  // Datos separados (release)
  patients: User[] = [];
  filteredPatients: User[] = [];
  specialists: Specialist[] = [];
  filteredSpecialists: Specialist[] = [];

  pendingValidations: any[] = [];
  filteredValidations: any[] = [];

  activeTab: 'patients' | 'specialists' | 'validations' = 'patients';
  searchTerm = '';

  loading = false;
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

    if (this.activeTab === 'validations') {
      this.loadPendingValidations();
    } else if (this.activeTab === 'patients') {
      this.loadPatients();
    } else {
      this.loadSpecialists();
    }
  }

  loadPatients(): void {
    this.loadingPatients = true;
    this.adminService.getPatients().subscribe({
      next: (data: User[]) => {
        this.patients = data.filter(patient => this.isActiveUser(patient.status));
        this.filteredPatients = [...this.patients];
        this.users = this.patients; // Mantener sincronizado con versión genérica
        this.filteredUsers = this.filteredPatients;
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
      next: (data: Specialist[]) => {
        this.specialists = data.filter(specialist => this.isActiveUser(specialist.status));
        this.filteredSpecialists = [...this.specialists];
        this.users = this.specialists as any; // Mantener sincronizado con versión genérica
        this.filteredUsers = this.filteredSpecialists as any;
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
      next: (data: Specialist[]) => {
        // Filtramos especialistas con cuenta NO verificada (accountVerified === 2 o null)
        this.pendingValidations = data.filter(specialist => 
          specialist.accountVerified === 2 || specialist.accountVerified === null
        );
        this.filteredValidations = [...this.pendingValidations];
        this.loadingValidations = false;
        console.log(`✅ ${this.pendingValidations.length} validaciones pendientes cargadas`);
      },
      error: (err) => {
        this.pendingValidations = [];
        this.filteredValidations = [];
        this.loadingValidations = false;
        console.warn('No se pudieron cargar las validaciones pendientes', err);
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

    if (this.activeTab === 'validations') {
      this.filteredValidations = this.pendingValidations.filter(v =>
        v.names.toLowerCase().includes(term) ||
        v.email.toLowerCase().includes(term)
      );
    } else if (this.activeTab === 'patients') {
      this.filteredPatients = this.patients.filter(u =>
        u.names.toLowerCase().includes(term) ||
        u.firstLastname.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
      );
    } else {
      this.filteredSpecialists = this.specialists.filter(u =>
        u.names.toLowerCase().includes(term) ||
        u.firstLastname.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
      );
    }
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
        this.showNotification(`${label} respaldado correctamente`, 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg = `No se pudo respaldar el ${type === 'patient' ? 'paciente' : 'especialista'}`;
        console.error(err);
        this.closeDeleteConfirm();
        this.showNotification(`No se pudo respaldar el ${label.toLowerCase()}`, 'error');
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

  suspendUser(user: User): void {
    this.adminService.suspendUser(user.id).subscribe({
      next: () => {
        user.status = 0;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = 'No se pudo suspender el usuario';
      }
    });
  }

  private showNotification(msg: string, type: 'success' | 'error' = 'success'): void {
    this.notification = { message: msg, type };
    setTimeout(() => {
      this.notification = null;
    }, 3000);
  }
}