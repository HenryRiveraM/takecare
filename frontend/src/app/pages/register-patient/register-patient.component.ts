import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { CloudinaryUploadService } from '../../services/cloudinary-upload.service';

@Component({
  selector: 'app-register-patient',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './register-patient.component.html',
  styleUrls: ['./register-patient.component.css']
})
export class RegisterPatientComponent {

  form: FormGroup;
  submitted = false;
  loading = false;

  documentoFile: File | null = null;
  selfieFile: File | null = null;

  maxDateAdult: string = (() => {
    const date = new Date();
    date.setFullYear(date.getFullYear() - 18); // Restamos 18 años a hoy
    return date.toISOString().split('T')[0];
  })();

  documentoNombre = '';
  selfieNombre = '';

  toast: { visible: boolean; type: 'error' | 'success' | 'warning'; title: string; message: string } = {
    visible: false,
    type: 'error',
    title: '',
    message: ''
  };
  private toastTimer: any;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private api: ApiService,
    private cloudinaryUploadService: CloudinaryUploadService
  ) {
    this.form = this.fb.group({
      names: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(/^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/)
      ]],
      first_lastname: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(/^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/)
      ]],
      second_lastname: ['', [
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(/^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/)
      ]],
      ci_number: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(10),
        Validators.pattern(/^[0-9A-Za-z-]+$/)
      ]],
      birth_date: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(50)
      ]],

      documento: [null, Validators.required],
      selfie: [null, Validators.required],
      terms: [false, Validators.requiredTrue]
    });
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && control.touched && control.invalid);
  }

  onFileSelected(event: Event, tipo: 'documento' | 'selfie') {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0]
    if (!file) return;

    const allowedImagesTypes = ['image/jpg','image/jpeg','image/png','image/webp'];

    if (!allowedImagesTypes.includes(file.type)){
      this.showToast('warning', 'Archivo no valido', 'Solo se permiten imágenes en el formate jpg, jpeg, png y webp');
      input.value = '';
      return;
    }

    if (file.size > 5 * 1024 *1024) {
      this.showToast('warning','Archivo demasiado grande', 'La imagen es demasiado grande, no debe superar los 5MB');
      input.value = '';
      return;
    }

    if (tipo === 'documento') {
      this.documentoFile = file;
      this.documentoNombre = file.name;
      this.form.patchValue({ documento: file.name});
      this.form.get('documento')?.markAsTouched();
      this.form.get('documento')?.updateValueAndValidity();
    }

    if (tipo === 'selfie') {
      this.selfieFile = file;
      this.selfieNombre = file.name;
      this.form.patchValue({ selfie: file.name });
      this.form.get('selfie')?.markAsTouched();
      this.form.get('selfie')?.updateValueAndValidity();
    }
  }

  showToast(type: 'error' | 'success' | 'warning', title: string, message: string) {
    clearTimeout(this.toastTimer);
    this.toast = { visible: true, type, title, message };
    this.toastTimer = setTimeout(() => this.closeToast(), 5000);
  }

  closeToast() {
    this.toast.visible = false;
  }

  private getFriendlyErrorMessage(err: any): { title: string; message: string } {
    const status = err?.status;
    const backendMessage: string = err?.error?.message || err?.error?.error || '';

    if (status === 400) {
      if (backendMessage.toLowerCase().includes('email')) {
        return {
          title: 'Correo inválido',
          message: 'El correo electrónico ingresado no es válido o ya está registrado. Verifica e intenta de nuevo.'
        };
      }
      if (backendMessage.toLowerCase().includes('ci') || backendMessage.toLowerCase().includes('document')) {
        return {
          title: 'CI duplicado',
          message: 'El número de CI ingresado ya está registrado en el sistema.'
        };
      }
      if (backendMessage.toLowerCase().includes('password')) {
        return {
          title: 'Contraseña inválida',
          message: 'La contraseña no cumple con los requisitos mínimos de seguridad.'
        };
      }
      return {
        title: 'Datos incompletos',
        message: 'Algunos campos no son válidos. Revisa que toda la información esté correctamente ingresada.'
      };
    }

    if (status === 409) {
      return {
        title: 'Cuenta existente',
        message: 'Ya existe una cuenta con ese correo electrónico o número de CI. ¿Quizás ya tienes una cuenta?'
      };
    }

    if (status === 422) {
      return {
        title: 'Información inválida',
        message: 'Algunos datos no tienen el formato esperado. Revisa la fecha de nacimiento y el correo.'
      };
    }

    if (status >= 500) {
      return {
        title: 'Error del servidor',
        message: 'Ocurrió un problema en nuestros servidores. Por favor, intenta de nuevo en unos minutos.'
      };
    }

    if (status === 0 || status == null) {
      return {
        title: 'Sin conexión',
        message: 'No se pudo conectar con el servidor. Verifica tu conexión a internet e intenta de nuevo.'
      };
    }

    return {
      title: 'Error al registrarse',
      message: 'Ocurrió un error inesperado. Por favor, intenta de nuevo más tarde.'
    };
  }

   async onSubmit() {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.documentoFile || !this.selfieFile)
    {
      this.showToast('warning', 'Archivos faltantes', 'Debes subir la fotografia de tu documento CI y una selfie para verificar tu identidad');
      return;
    }

    this.loading = true;

    try{
      const ciUpload = await this.cloudinaryUploadService.uploadImage(
        this.documentoFile, 'patients/ci'
      );

      const selfieUpload = await this.cloudinaryUploadService.uploadImage(
        this.selfieFile,'patients/selfies'
      );

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
      ciDocumentImg: ciUpload.url,
      selfieVerification: selfieUpload.url,
      clinicalHistory: 'Sin antecedentes'
      };

      this.api.registerPatient(data).subscribe({
        next: (res) => {
          this.loading = false;
          console.log('✅ REGISTRO EXITOSO', res);
          this.showToast('success', '¡Cuenta creada!', 'Tu registro fue exitoso. Redirigiendo al inicio de sesión...');
          setTimeout(() => this.router.navigate(['/login']), 2000);
        },
        error: (err) => {
          this.loading = false;
          console.error('❌ ERROR BACKEND COMPLETO:', err);
          const { title, message } = this.getFriendlyErrorMessage(err);
          this.showToast('error', title, message);
        }
      });
    } catch(error) {
      console.error('X Error subiendo archivos a Firebase:', error);
      this.showToast('error', 'Error al subir archivos', 'Ocurrió un error al subir tus archivos. Intenta de nuevo.');
    }
  }

  cancel(){
    this.router.navigate(['/']);
  }
}