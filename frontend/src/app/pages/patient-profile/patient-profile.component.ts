import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PatientService, PatientProfile } from '../../services/patient.service';

@Component({
  selector: 'app-patient-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadUserData();
  }

  initForm() {
    this.profileForm = this.fb.group({
      names: ['', Validators.required],
      firstLastname: ['', Validators.required],
      secondLastname: [''],
      ciNumber: [{ value: '', disabled: true }], 
      birthDate: ['', Validators.required],
      email: [{ value: '', disabled: true }],
      clinicalHistory: ['']
    });
  }

  loadUserData() {
    this.isLoading = true;
    
    this.patientService.getProfile().subscribe({
      next: (profile) => {
        if (profile) {
          this.profileForm.patchValue({
            names: profile.names,
            firstLastname: profile.firstLastname,
            secondLastname: profile.secondLastname || '',
            ciNumber: profile.ciNumber || '',
            birthDate: profile.birthDate,
            email: profile.email,
            clinicalHistory: profile.clinicalHistory || ''
          });
          this.userDataBackup = profile;
          console.log('✅ Datos cargados:', profile);
        } else {
          console.error('❌ Error: perfil vacío');
          alert('Error al cargar el perfil.');
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error de conexión:', error);
        this.isLoading = false;
        alert('Error de conexión. Verifica tu internet e intenta de nuevo.');
      }
    });
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing && this.userDataBackup) {
      this.profileForm.patchValue(this.userDataBackup);
    }
  }

  onSave() {
    // Validar que el formulario esté completo
    if (this.profileForm.valid) {
      this.isLoading = true;  // Mostrar spinner
      
      // Obtener todos los valores del formulario (incluyendo disabled)
      const formData = this.profileForm.getRawValue();
      
      // Enviar al backend
      this.patientService.updateProfile(formData).subscribe({
        next: (updatedProfile: any) => {
          if (updatedProfile) {
            this.userDataBackup = formData;
            this.isEditing = false;
            this.isLoading = false;
            alert('✅ ¡Perfil actualizado con éxito!');
          } else {
            console.error('Error: perfil actualizado inválido');
            alert('❌ Error al actualizar el perfil.');
            this.isLoading = false;
          }
        },
        error: (error: any) => {
          console.error('Error de conexión:', error);
          this.isLoading = false;
          alert('❌ Error al actualizar el perfil. Intenta de nuevo.');
        }
      });
    } else {
      // Formulario inválido
      alert('Por favor, completa todos los campos requeridos.');
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      console.log('Archivo seleccionado:', file.name);
    }
  }
}