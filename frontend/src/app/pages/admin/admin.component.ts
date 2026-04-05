import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, User } from '../../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {

  users: User[] = [];
  filteredUsers: User[] = [];

  pendingValidations: any[] = [];
  filteredValidations: any[] = [];

  activeTab: 'patients' | 'specialists' | 'validations' = 'patients';
  searchTerm = '';

  loading = false;
  errorMsg = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadData();
  }


  loadData() {
    this.loading = true;
    this.errorMsg = '';

    if (this.activeTab === 'validations') {
      this.loadPendingValidations();
    } else {
      this.loadUsers();
    }
  }

  loadUsers() {
    const request$ =
      this.activeTab === 'patients'
        ? this.adminService.getPatients()
        : this.adminService.getSpecialists();

    request$.subscribe({
      next: (data) => {
        this.users = data;
        this.filteredUsers = data;
        this.loading = false;
      },
      error: () => {
        this.errorMsg = 'Error cargando usuarios';
        this.loading = false;
      }
    });
  }


  loadPendingValidations() {
    this.adminService.getPendingValidations().subscribe({
      next: (data) => {
        this.pendingValidations = data;
        this.filteredValidations = data;
        this.loading = false;
      },
      error: () => {
        this.errorMsg = 'Error cargando validaciones';
        this.loading = false;
      }
    });
  }

  setTab(tab: 'patients' | 'specialists' | 'validations') {
    this.activeTab = tab;
    this.searchTerm = '';
    this.loadData();
  }

  onSearch() {
    const term = this.searchTerm.toLowerCase().trim();

    if (this.activeTab === 'validations') {
      this.filteredValidations = this.pendingValidations.filter(v =>
        v.names.toLowerCase().includes(term) ||
        v.email.toLowerCase().includes(term)
      );
    } else {
      this.filteredUsers = this.users.filter(u =>
        u.names.toLowerCase().includes(term) ||
        u.firstLastname.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
      );
    }
  }

  processValidation(id: number, status: 'approved' | 'rejected') {
    this.adminService.validateUser(id, status).subscribe({
      next: () => {
        this.pendingValidations = this.pendingValidations.filter(v => v.id !== id);
        this.onSearch(); // Refresca la vista
      },
      error: () => {
        this.errorMsg = 'No se pudo procesar la validación';
      }
    });
  }

  deleteUser(user: User) {
    const request$ =
      this.activeTab === 'patients'
        ? this.adminService.deletePatient(user.id)
        : this.adminService.deleteSpecialist(user.id);

    request$.subscribe({
      next: () => {
        this.users = this.users.filter(u => u.id !== user.id);
        this.filteredUsers = this.filteredUsers.filter(u => u.id !== user.id);
      },
      error: (err: any) => {
        console.error(err);
        this.errorMsg = 'No se pudo eliminar el usuario';
      }
    });
  }

  suspendUser(user: User) {
    this.adminService.suspendUser(user.id).subscribe({
      next: () => {
        user.status = 0;
      }
    });
  }
}