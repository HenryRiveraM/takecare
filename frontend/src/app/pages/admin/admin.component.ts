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

  filteredPatients: Patient[] = [];
  filteredSpecialists: Specialist[] = [];
  filteredValidations: any[] = [];

  activeTab: 'patients' | 'specialists' | 'validations' = 'patients';
  searchTerm: string = '';

  loadingPatients = false;
  loadingSpecialists = false;
  loadingValidations = false;
  errorMsg = '';

  pendingValidations: any[] = [];
  
  showDeleteConfirm = false;
  deleteTarget: { type: 'patient' | 'specialist'; id: number; name: string } | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadPatients();
    this.loadSpecialists();
    this.loadPendingValidations();
  }

  loadPatients(): void {
    this.loadingPatients = true;
    this.adminService.getPatients().subscribe({
      next: (data: Patient[]) => {
        this.patients = data.filter(p => p.status === true);
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
        this.specialists = data.filter(s => s.status === true);
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
        this.errorMsg = 'Error al cargar las validaciones pendientes';
        this.loadingValidations = false;
        console.error(err);
      }
    });
  }

  // Lógica para Aprobar o Rechazar
  processValidation(id: number, status: 'approved' | 'rejected'): void {
    this.adminService.validateUser(id, status).subscribe({
      next: () => {
        // Si todo sale bien, lo quitamos de la lista visual
        this.pendingValidations = this.pendingValidations.filter(u => u.id !== id);
        this.filteredValidations = this.filteredValidations.filter(u => u.id !== id);
        
        // Si fue aprobado, refrescamos la lista de pacientes para que aparezca allí
        if (status === 'approved') {
          this.loadPatients();
        }
      },
      error: (err) => {
        this.errorMsg = 'No se pudo completar la operación de validación';
        console.error(err);
      }
    });
  }

  setTab(tab: 'patients' | 'specialists' | 'validations'): void {
    this.activeTab = tab;
    this.searchTerm = '';
    this.resetFilters();
    this.errorMsg = '';
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
      // --- Lógica de búsqueda para TU sección ---
      this.filteredValidations = this.pendingValidations.filter(v => 
        v.names.toLowerCase().includes(term) || 
        v.firstLastname.toLowerCase().includes(term) ||
        v.email.toLowerCase().includes(term)
      );
    }
  }

  resetFilters(): void {
    this.filteredPatients = [...this.patients];
    this.filteredSpecialists = [...this.specialists];
    this.filteredValidations = [...this.pendingValidations];
  }

  deletePatient(id: number, fullName: string): void {
    this.showDeleteConfirm = true;
    this.deleteTarget = { type: 'patient', id, name: fullName };
  }

  deleteSpecialist(id: number, fullName: string): void {
    this.showDeleteConfirm = true;
    this.deleteTarget = { type: 'specialist', id, name: fullName };
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;

    const { type, id } = this.deleteTarget;
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
      },
      error: (err: HttpErrorResponse) => {
        this.errorMsg = `No se pudo eliminar el ${type}`;
        console.error(err);
        this.closeDeleteConfirm();
      }
    });
  }

  cancelDelete(): void {
    this.closeDeleteConfirm();
  }

  private closeDeleteConfirm(): void {
    this.showDeleteConfirm = false;
    this.deleteTarget = null;
  }

  get isLoading(): boolean {
    return this.loadingPatients || this.loadingSpecialists || this.loadingValidations;
  }
}