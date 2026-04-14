import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { PatientService, PatientProfile } from '../../services/patient.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { NavbarComponent } from '../../shared/navbar/navbar.component';

@Component({
  selector: 'app-patient-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe,SidebarComponent,NavbarComponent],
  templateUrl: './patient-profile.component.html',
  styleUrl: './patient-profile.component.css'
})
export class PatientProfileComponent implements OnInit {
  profileForm!: FormGroup;
  isEditing = false;
  isLoading = false;
  userDataBackup: PatientProfile | null = null;

  constructor(
    private fb: FormBuilder,
    private patientService: PatientService,
    private router: Router,
    private translate: TranslateService,
    public sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadUserData();
  }

  goBackToProfile(): void {
    this.router.navigate(['/patient']);
  }

  initForm(): void {
    this.profileForm = this.fb.group({
      names: ['', Validators.required],
      firstLastname: ['', Validators.required],
      secondLastname: [''],
      ciNumber: [{ value: '', disabled: true }],
      email: [{ value: '', disabled: true }],
      clinicalHistory: ['']
    });
  }

  loadUserData(): void {
    this.isLoading = true;
    
    this.patientService.getProfile().subscribe({
      next: (profile) => {
        if (profile) {
          this.profileForm.patchValue({
            names: profile.names,
            firstLastname: profile.firstLastname,
            secondLastname: profile.secondLastname || '',
            ciNumber: profile.ciNumber || '',
            email: profile.email,
            clinicalHistory: profile.clinicalHistory || ''
          });
          this.userDataBackup = profile;
          console.log('Datos cargados:', profile);
        } else {
          console.error('Error: perfil vacío');
          alert(this.translate.instant('patientProfile.messages.loadError'));
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error de conexión:', error);
        this.isLoading = false;
        alert(this.translate.instant('patientProfile.messages.connectionError'));
      }
    });
  }

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
    if (!this.isEditing && this.userDataBackup) {
      this.profileForm.patchValue(this.userDataBackup);
    }
  }

  onSave(): void {
    if (this.profileForm.valid) {
      this.isLoading = true;
      const formData = this.profileForm.getRawValue();
      
      this.patientService.updateProfile(formData).subscribe({
        next: (updatedProfile: any) => {
          if (updatedProfile) {
            this.userDataBackup = formData;
            this.isEditing = false;
            this.isLoading = false;
            alert(this.translate.instant('patientProfile.messages.updateSuccess'));
          } else {
            console.error('Error: perfil actualizado inválido');
            alert(this.translate.instant('patientProfile.messages.updateError'));
            this.isLoading = false;
          }
        },
        error: (error: any) => {
          console.error('Error de conexión:', error);
          this.isLoading = false;
          alert(this.translate.instant('patientProfile.messages.updateConnectionError'));
        }
      });
    } else {
      alert(this.translate.instant('patientProfile.messages.requiredFields'));
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      console.log('Archivo seleccionado:', file.name);
    }
  }
}