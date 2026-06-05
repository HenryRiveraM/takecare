import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { CloudinaryUploadService } from '../../services/cloudinary-upload.service';

@Component({
  selector: 'app-register-patient',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './register-patient.component.html',
  styleUrls: ['./register-patient.component.css']
})
export class RegisterPatientComponent {

  form: FormGroup;
  submitted = false;
  loading = false;
  showPassword = false;
  showPasswordConfirm = false;

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
    private cloudinaryUploadService: CloudinaryUploadService,
    private translate: TranslateService
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
      birth_date: ['', [Validators.required, this.adultDateValidator]],
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.pattern(/^[^@\s]+@[^@\s]+\.[^@\s]+$/)
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(50),
        this.passwordStrengthValidator
      ]],
      passwordConfirm: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(50)
      ]],

      documento: [null, Validators.required],
      selfie: [null, Validators.required],
      terms: [false, Validators.requiredTrue]
    }, {
      validators: this.passwordsMatchValidator
    });
  }

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!(control && (control.touched || this.submitted) && control.invalid);
  }

  hasError(field: string, error: string): boolean {
    const control = this.form.get(field);
    return !!(control && (control.touched || this.submitted) && control.hasError(error));
  }

  hasPasswordMismatch(): boolean {
    const confirmControl = this.form.get('passwordConfirm');
    return !!(
      this.form.hasError('passwordMismatch') &&
      confirmControl?.value &&
      (confirmControl.touched || this.submitted)
    );
  }

  onFileSelected(event: Event, tipo: 'documento' | 'selfie') {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0]
    if (!file) return;

    const allowedImagesTypes = ['image/jpg','image/jpeg','image/png','image/webp'];

    if (!allowedImagesTypes.includes(file.type)){
      this.showToast(
        'warning',
        this.translate.instant('registerPatient.toast.invalidFileTitle'),
        this.translate.instant('registerPatient.toast.invalidFileMessage')
      );
      input.value = '';
      return;
    }

    if (file.size > 5 * 1024 *1024) {
      this.showToast(
        'warning',
        this.translate.instant('registerPatient.toast.fileTooLargeTitle'),
        this.translate.instant('registerPatient.toast.fileTooLargeMessage')
      );
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

  showToast(type: 'error' | 'success' | 'warning', title: string, message: string): void {
    clearTimeout(this.toastTimer);
    this.toast = { visible: true, type, title, message };
    this.toastTimer = setTimeout(() => this.closeToast(), 5000);
  }

  closeToast(): void {
    this.toast.visible = false;
  }

  private getFriendlyErrorMessage(err: any): { title: string; message: string } {
    const status = err?.status;
    const backendMessage: string = err?.error?.message || err?.error?.error || '';

    if (status === 400) {
      if (backendMessage.toLowerCase().includes('email')) {
        return {
          title: this.translate.instant('registerPatient.toast.invalidEmailTitle'),
          message: this.translate.instant('registerPatient.toast.invalidEmailMessage')
        };
      }
      if (backendMessage.toLowerCase().includes('ci') || backendMessage.toLowerCase().includes('document')) {
        return {
          title: this.translate.instant('registerPatient.toast.duplicateCiTitle'),
          message: this.translate.instant('registerPatient.toast.duplicateCiMessage')
        };
      }
      if (backendMessage.toLowerCase().includes('password')) {
        return {
          title: this.translate.instant('registerPatient.toast.invalidPasswordTitle'),
          message: this.translate.instant('registerPatient.toast.invalidPasswordMessage')
        };
      }
      return {
        title: this.translate.instant('registerPatient.toast.incompleteDataTitle'),
        message: this.translate.instant('registerPatient.toast.incompleteDataMessage')
      };
    }

    if (status === 409) {
      return {
        title: this.translate.instant('registerPatient.toast.existingAccountTitle'),
        message: this.translate.instant('registerPatient.toast.existingAccountMessage')
      };
    }

    if (status === 422) {
      return {
        title: this.translate.instant('registerPatient.toast.invalidInfoTitle'),
        message: this.translate.instant('registerPatient.toast.invalidInfoMessage')
      };
    }

    if (status >= 500) {
      return {
        title: this.translate.instant('registerPatient.toast.serverErrorTitle'),
        message: this.translate.instant('registerPatient.toast.serverErrorMessage')
      };
    }

    if (status === 0 || status == null) {
      return {
        title: this.translate.instant('registerPatient.toast.noConnectionTitle'),
        message: this.translate.instant('registerPatient.toast.noConnectionMessage')
      };
    }

    return {
      title: this.translate.instant('registerPatient.toast.genericErrorTitle'),
      message: this.translate.instant('registerPatient.toast.genericErrorMessage')
    };
  }

  passwordsMatch(): boolean {
    const password = this.form.get('password')?.value;
    const passwordConfirm = this.form.get('passwordConfirm')?.value;
    return password === passwordConfirm && password?.length > 0;
  }

  private passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const passwordConfirm = group.get('passwordConfirm')?.value;

    if (!password || !passwordConfirm) {
      return null;
    }

    return password === passwordConfirm ? null : { passwordMismatch: true };
  }

  private passwordStrengthValidator(control: AbstractControl): ValidationErrors | null {
    const value = String(control.value || '');

    if (!value) {
      return null;
    }

    const hasUppercase = /[A-ZÁÉÍÓÚÑ]/.test(value);
    const hasLowercase = /[a-záéíóúñ]/.test(value);
    const hasNumber = /\d/.test(value);

    return hasUppercase && hasLowercase && hasNumber ? null : { passwordStrength: true };
  }

  private adultDateValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;

    if (!value) {
      return null;
    }

    const birthDate = new Date(`${value}T00:00:00`);
    if (Number.isNaN(birthDate.getTime())) {
      return { invalidDate: true };
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (birthDate > today) {
      return { futureDate: true };
    }

    const adultDate = new Date(today);
    adultDate.setFullYear(adultDate.getFullYear() - 18);

    return birthDate <= adultDate ? null : { underage: true };
  }

   async onSubmit() {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.passwordsMatch()) {
      this.showToast(
        'error',
        this.translate.instant('registerPatient.toast.passwordMismatchTitle') || 'Contraseñas no coinciden',
        this.translate.instant('registerPatient.toast.passwordMismatchMessage') || 'Las contraseñas deben ser iguales'
      );
      return;
    }

    if (!this.documentoFile || !this.selfieFile)
    {
      this.showToast(
        'warning',
        this.translate.instant('registerPatient.toast.filesRequiredTitle'),
        this.translate.instant('registerPatient.toast.filesRequiredMessage')
      );
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
          this.showToast(
            'success',
            this.translate.instant('registerPatient.toast.successTitle'),
            this.translate.instant('registerPatient.toast.successMessage')
          );
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
      this.showToast(
        'error',
        this.translate.instant('registerPatient.toast.uploadErrorTitle'),
        this.translate.instant('registerPatient.toast.uploadErrorMessage')
      );
    }
  }

  cancel(){
    this.router.navigate(['/']);
  }
}
