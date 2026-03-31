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

  activeTab: 'patients' | 'specialists' = 'patients';
  searchTerm = '';

  loading = false;
  errorMsg = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;

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
        this.errorMsg = 'Error cargando datos';
        this.loading = false;
      }
    });
  }

  setTab(tab: 'patients' | 'specialists') {
    this.activeTab = tab;
    this.searchTerm = '';
    this.loadUsers();
  }

  onSearch() {
    const term = this.searchTerm.toLowerCase();

    this.filteredUsers = this.users.filter(u =>
      u.names.toLowerCase().includes(term) ||
      u.firstLastname.toLowerCase().includes(term) ||
      u.email.toLowerCase().includes(term)
    );
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
    this.adminService.suspendUser(user.id).subscribe(() => {
      user.status = 0;
    });
  }
}