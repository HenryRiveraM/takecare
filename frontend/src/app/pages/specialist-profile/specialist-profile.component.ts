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
    bio: ''
  };

  loading = false;
  successMsg = '';

  constructor(
    private specialistService: SpecialistService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
    this.loadProfile();
  }

  loadProfile() {
    this.specialistService.getProfile(this.user.id).subscribe((data: any) => {
      this.profile = data;
    });
  }

  save() {
    this.loading = true;

    this.specialistService.updateProfile(this.user.id, this.profile).subscribe({
      next: () => {
        this.successMsg = 'Perfil actualizado correctamente';
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}