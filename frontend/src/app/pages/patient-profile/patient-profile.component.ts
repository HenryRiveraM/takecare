import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { PatientService, PatientProfile } from '../../services/patient.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

@Component({
  selector: 'app-patient-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-profile.component.html',
  styleUrl: './patient-profile.component.css'
})
export class PatientProfileComponent implements OnInit {
  profileForm!: FormGroup;
  isEditing = false;
  isLoading = false;
  userDataBackup: PatientProfile | null = null;

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  showToastMessage(messageKey: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = messageKey;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 3000);
  }

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
      names: ['', [Validators.required, nameValidator() ]],
      firstLastname: ['', [Validators.required, nameValidator()]],
      secondLastname: ['', [nameValidator()]],
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
        } else {
          console.error('Error: perfil vacío');
          this.showToastMessage('patientProfile.messages.loadError', 'error');
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error de conexión:', error);
        this.isLoading = false;
        this.showToastMessage('patientProfile.messages.connectionError', 'error');
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
      
      formData.names = formData.names.trim();
      formData.firstLastname = formData.firstLastname.trim();
      formData.secondLastname = formData.secondLastname?.trim() || null;
      
      this.patientService.updateProfile(formData).subscribe({
        next: (updatedProfile: any) => {
          if (updatedProfile) {
            this.userDataBackup = formData;
            this.isEditing = false;
            this.isLoading = false;
            this.showToastMessage('patientProfile.messages.updateSuccess', 'success');
          } else {
            console.error('Error: perfil actualizado inválido');
            this.showToastMessage('patientProfile.messages.updateError', 'error');
            this.isLoading = false;
          }
        },
        error: (error: any) => {
          console.error('Error de conexión:', error);
          this.isLoading = false;
          this.showToastMessage('patientProfile.messages.updateConnectionError', 'error');
        }
      });
    } else {
      this.showToastMessage('patientProfile.messages.requiredFields', 'error');
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
    }
  }
}

export function nameValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;

    if (!value) return null; 

    if (/\d/.test(value)) {
      return { hasNumbers: true };
    }

    if (/\s{2,}/.test(value)) {
      return { doubleSpaces: true };
    }

    if (!/^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]+$/.test(value)) {
      return { invalidChars: true };
    }

    if (value.startsWith(' ') || value.endsWith(' ')) {
      return { leadingTrailingSpaces: true };
    }

    return null;
  };
}
