import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SpecialistService } from '../../services/specialist.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-specialist-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './specialist-profile.component.html',
  styleUrls: ['./specialist-profile.component.css']
})
export class SpecialistProfileComponent implements OnInit {

  user: any;

  profile: any = {
    names: '',
    firstLastname: '',
    secondLastname: '',
    email: '',
    biography: '',
    officeUbi: '',
    sessionCost: 0,
  };

  originalProfile: any = {};

  loading = false;
  successMsg = '';
  errorMsg = '';
  isEditing = false;

  constructor(
    private specialistService: SpecialistService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
    console.log('Usuario obtenido:', this.user);
    if (this.user?.id) {
      this.loadProfile();
    } else {
      this.errorMsg = 'No se pudo obtener el ID del usuario';
      console.error('User ID no disponible');
    }
  }

  loadProfile() {
    this.loading = true;
    this.errorMsg = '';
    console.log(`Cargando perfil del especialista ID: ${this.user.id}`);
    
    this.specialistService.getProfile(this.user.id).subscribe({
      next: (data: any) => {
        console.log('Perfil cargado:', data);

        this.profile = {
          names: data.names || '',
          firstLastname: data.firstLastname || '',
          secondLastname: data.secondLastname || '',
          email: data.email || '',
          biography: data.biography || '',
          officeUbi: data.officeUbi || '',
          sessionCost: data.sessionCost || 0
        };

        this.originalProfile = { ...this.profile };
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error cargando perfil:', error);
        this.errorMsg = 'Error al cargar el perfil. Verifica la conexión.';
        this.loading = false;
      }
    });
  }

  enableEdit(): void {
    this.isEditing = true;
    this.successMsg = '';
    this.errorMsg = '';
  }

  cancelEdit(): void {
    this.profile = { ...this.originalProfile };
    this.isEditing = false;
    this.successMsg = '';
    this.errorMsg = '';
  }

  goBackToProfile(): void {
  this.router.navigate(['/specialist']);
  }

  save() : void {
    if (!this.isEditing) return;
    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    const payload = {
      names: this.profile.names,
      firstLastname: this.profile.firstLastname,
      secondLastname: this.profile.secondLastname,
      email: this.profile.email,
      biography: this.profile.biography,
      officeUbi: this.profile.officeUbi,
      sessionCost: this.profile.sessionCost
    };

    this.specialistService.updateProfile(this.user.id, payload).subscribe({
      next: (response: any) => {
        console.log('Perfil actualizado:', response);

        this.originalProfile = { ...this.profile };
        this.successMsg = 'Perfil actualizado correctamente';
        this.isEditing = false;
        this.loading = false;

        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (error: any) => {
        console.error('Error actualizando perfil:', error);
        this.errorMsg = 'Error al actualizar el perfil';
        this.loading = false;
      }
    });
  }
}