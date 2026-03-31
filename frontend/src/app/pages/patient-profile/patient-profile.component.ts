import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

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
  userDataBackup: any; // Para guardar el estado anterior

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadUserData();
  }

  initForm() {
    this.profileForm = this.fb.group({
      names: ['', Validators.required],
      first_lastname: ['', Validators.required],
      second_lastname: [''],
      ci_number: [{ value: '', disabled: true }], // El CI suele ser estático
      birth_date: ['', Validators.required],
      email: [{ value: '', disabled: true }],
      phone: [''], // Campo extra útil para contacto
      bio: ['']
    });
  }

  loadUserData() {
    // Aquí simularás o usarás el servicio que tus compañeros subieron
    // Por ahora, cargamos datos de ejemplo
    const mockData = {
      fullName: 'Usuario Take Care',
      email: 'usuario@ejemplo.com',
      bio: 'Buscando equilibrio y bienestar mental.',
      phone: '+51 987 654 321'
    };

    this.profileForm.patchValue(mockData);
    this.userDataBackup = mockData; 
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing) {
      this.profileForm.patchValue(this.userDataBackup);
    }
  }

  onSave() {
    if (this.profileForm.valid) {
      this.isLoading = true;
      
      console.log('Enviando a HU08-BE:', this.profileForm.getRawValue());
      
      setTimeout(() => {
        this.userDataBackup = this.profileForm.getRawValue(); 
        this.isEditing = false;
        this.isLoading = false;
        alert('¡Perfil actualizado con éxito!');
      }, 1500);
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      console.log('Archivo seleccionado:', file.name);
    }
  }
}