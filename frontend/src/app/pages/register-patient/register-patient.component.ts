import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

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

  constructor(private fb: FormBuilder, private router: Router) {

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

    const formData = new FormData();


    formData.append('names', this.form.value.names);
    formData.append('first_lastname', this.form.value.first_lastname);
    formData.append('second_lastname', this.form.value.second_lastname || '');
    formData.append('ci_number', this.form.value.ci_number);
    formData.append('birth_date', this.form.value.birth_date);
    formData.append('email', this.form.value.email);
    formData.append('password', this.form.value.password);


    formData.append('role', '1');


    if (this.documentoFile) {
      formData.append('ci_document_img', this.documentoFile);
    }

    if (this.selfieFile) {
      formData.append('selfie_verification', this.selfieFile);
    }

    console.log('🚀 DATA FINAL PARA BACKEND:', formData);


  }

  cancel() {
    this.router.navigate(['/']);
  }

}