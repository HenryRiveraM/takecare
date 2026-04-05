import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SpecialistService } from '../../services/specialist.service';
import { AuthService } from '../../services/auth.service';

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
    email: '',
    phone: '',
    bio: '',
    biography: '',
    officeUbi: '',
    sessionCost: 0
  };

  loading = false;
  successMsg = '';
  errorMsg = '';

  constructor(
    private specialistService: SpecialistService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
    console.log('👤 Usuario obtenido:', this.user);
    if (this.user?.id) {
      this.loadProfile();
    } else {
      this.errorMsg = 'No se pudo obtener el ID del usuario';
      console.error('❌ User ID no disponible');
    }
  }

  loadProfile() {
    this.loading = true;
    console.log(`📥 Cargando perfil del especialista ID: ${this.user.id}`);
    
    this.specialistService.getProfile(this.user.id).subscribe({
      next: (data: any) => {
        console.log('✅ Perfil cargado:', data);
        this.profile = data;
        this.loading = false;
      },
      error: (error: any) => {
        console.error('❌ Error cargando perfil:', error);
        this.errorMsg = 'Error al cargar el perfil. Verifica la conexión.';
        this.loading = false;
      }
    });
  }

  save() {
    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.specialistService.updateProfile(this.user.id, this.profile).subscribe({
      next: () => {
        this.successMsg = 'Perfil actualizado correctamente';
        this.loading = false;
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (error: any) => {
        console.error('❌ Error actualizando perfil:', error);
        this.errorMsg = 'Error al actualizar el perfil';
        this.loading = false;
      }
    });
  }
}