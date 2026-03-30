import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-register-patient',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register-patient.component.html',
  styleUrls: ['./register-patient.component.css']
})
export class RegisterPatientComponent {

  form: FormGroup;

  documentoFile: File | null = null;
  selfieFile: File | null = null;

  documentoNombre = '';
  selfieNombre = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private api: ApiService
  ) {

    this.form = this.fb.group({
      names: ['', Validators.required],
      first_lastname: ['', Validators.required],
      second_lastname: [''],
      ci_number: ['', Validators.required],
      birth_date: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      terms: [false, Validators.requiredTrue]
    });

  }

  onFileSelected(event: any, tipo: string) {
    const file = event.target.files[0];
    if (!file) return;

    if (tipo === 'documento') {
      this.documentoFile = file;
      this.documentoNombre = file.name;
    }

    if (tipo === 'selfie') {
      this.selfieFile = file;
      this.selfieNombre = file.name;
    }
  }

  onSubmit() {

    if (this.form.invalid) return;

  const data = {
    names: this.form.value.names.trim(),
    firstLastname: this.form.value.first_lastname.trim(),

    ...(this.form.value.second_lastname?.trim() && {
      secondLastname: this.form.value.second_lastname.trim()
    }),

    ciNumber: String(this.form.value.ci_number).trim(),
    birthDate: this.form.value.birth_date,
    email: this.form.value.email.trim(),
    password: this.form.value.password.trim(),
    role: 1,
    selfieVerification: this.selfieNombre || 'default.jpg',
    clinicalHistory: 'Sin antecedentes'
  };

    this.api.registerPatient(data).subscribe({
      next: (res) => {
        console.log('✅ REGISTRO EXITOSO', res);
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('❌ ERROR BACKEND COMPLETO:', err);
        alert(JSON.stringify(err.error));
      }
    });
  }

  cancel() {
    this.router.navigate(['/']);
  }

}