import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, User } from '../../services/admin.service';

@Component({
  selector: 'app-admin-suspension',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-suspension.component.html',
  styleUrls: ['./admin-suspension.component.css']
})
export class AdminSuspensionComponent implements OnInit {

  users: User[] = [];
  filteredUsers: User[] = [];

  loading = false;
  errorMsg = '';

  searchTerm = '';
  notification: string | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers() {
    this.loading = true;

    Promise.all([
      this.adminService.getPatients().toPromise(),
      this.adminService.getSpecialists().toPromise()
    ])
    .then(([patients, specialists]) => {

      this.users = [
        ...(patients || []),
        ...(specialists || [])
      ];

      this.filteredUsers = [...this.users];
      this.loading = false;

    })
    .catch(() => {
      this.errorMsg = 'Error al cargar usuarios';
      this.loading = false;
    });
  }

  onSearch() {
    const term = this.searchTerm.toLowerCase();

    this.filteredUsers = this.users.filter(user =>
      user.names.toLowerCase().includes(term) ||
      user.email.toLowerCase().includes(term)
    );
  }

  // 🔥 LÓGICA REAL (BACKEND)
  toggleStatus(user: User) {
    this.adminService.suspendUser(user.id).subscribe({
      next: () => {

        // ⚠️ backend siempre pone status = 0
        user.status = user.status === 1 ? 0 : 1;

        this.showNotification(
          user.status === 1 ? 'Usuario activado' : 'Usuario suspendido'
        );
      },
      error: () => {
        this.errorMsg = 'Error al cambiar estado';
      }
    });
  }

  showNotification(msg: string) {
    this.notification = msg;
    setTimeout(() => this.notification = null, 2500);
  }
}