import { Component, OnInit, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ApiService, SpecialistRegisterRequest } from '../../services/api.service';
import { CloudinaryUploadService } from '../../services/cloudinary-upload.service';

@Component({
  selector: 'app-register-specialist',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, TranslatePipe],
  templateUrl: './register-specialist.component.html',
  styleUrl: './register-specialist.component.css'
})
export class RegisterSpecialistComponent implements OnInit {
  @ViewChild('carnetInput') carnetInput?: ElementRef<HTMLInputElement>;
  @ViewChild('certificationsInput') certificationsInput?: ElementRef<HTMLInputElement>;

  registerForm!: FormGroup;
  submitted = false;
  isDragging = false;
  isLoading: boolean = false;

  showPassword = false;
  showPasswordConfirm = false;

  fileList: { file: File, size: string }[] = [];
  carnetFile: { file: File, url: string } | null = null;

  maxDateAdult: string = (() => {
    const date = new Date();
    date.setFullYear(date.getFullYear() - 18); 
    return date.toISOString().split('T')[0];
  })();

  toast: { visible: boolean; type: 'error' | 'success' | 'warning'; title: string; message: string } = {
    visible: false,
    type: 'error',
    title: '',
    message: ''
  };
  private toastTimer: any;

  especialidadesOpciones = [
    { id: 'mental', nombre: 'Trastornos mentales', translationKey: 'patientSearch.filters.mentalHealth', seleccionado: false },
    { id: 'adicciones', nombre: 'Adicciones', translationKey: 'patientSearch.filters.addictions', seleccionado: false },
    { id: 'familia', nombre: 'Terapia familiar', translationKey: 'patientSearch.filters.familyTherapy', seleccionado: false },
    { id: 'infantil', nombre: 'Psicología infantil', translationKey: 'patientSearch.filters.childPsychology', seleccionado: false },
    { id: 'depresion', nombre: 'Depresión y ansiedad', translationKey: 'patientSearch.filters.depressionAnxiety', seleccionado: false },
    { id: 'ocupacional', nombre: 'Terapia ocupacional', translationKey: 'patientSearch.filters.occupationalTherapy', seleccionado: false }
  ];

  constructor(
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private api: ApiService,
    private router: Router,
    private cloudinaryUploadService: CloudinaryUploadService,
    private translate: TranslateService
  ){}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      nombre: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(/^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/)]],
      apellidoPaterno: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(/^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/)]],
      apellidoMaterno: ['', [
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(/^[A-Za-zÁÉÍÓÚáéíóúÑñ\s]+$/)]],
      fechaNacimiento: ['', [Validators.required, this.adultDateValidator]],
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.pattern(/^[^@\s]+@[^@\s]+\.[^@\s]+$/)
      ]],
      password: ['', [
        Validators.required, 
        Validators.minLength(8), 
        Validators.maxLength(50),
        this.passwordStrengthValidator]],
      passwordConfirm: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(50)]],
      documento: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(10),
        Validators.pattern(/^[0-9-A-Za-z\s]+$/)]],
      aceptaTerminos: [false, Validators.requiredTrue],
      aceptaComunicaciones: [false]
    },{
      validators: this.passwordsMatchValidator 
    });
  }

  get f() { return this.registerForm.controls; }

  hasError(field: string, error: string): boolean {
    const control = this.registerForm.get(field);
    return !!(control && (control.touched || this.submitted) && control.hasError(error));
  }

  hasPasswordMismatch(): boolean {
    const confirmControl = this.registerForm.get('passwordConfirm');
    return !!(
      this.registerForm.hasError('passwordMismatch') &&
      confirmControl?.value &&
      (confirmControl.touched || this.submitted)
    );
  }

  passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
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
          title: this.translate.instant('registerSpecialist.toast.invalidEmailTitle'),
          message: this.translate.instant('registerSpecialist.toast.invalidEmailMessage')
        };
      }
      if (backendMessage.toLowerCase().includes('documento') || backendMessage.toLowerCase().includes('ci')) {
        return {
          title: this.translate.instant('registerSpecialist.toast.duplicateDocumentTitle'),
          message: this.translate.instant('registerSpecialist.toast.duplicateDocumentMessage')
        };
      }
      if (backendMessage.toLowerCase().includes('password')) {
        return {
          title: this.translate.instant('registerSpecialist.toast.invalidPasswordTitle'),
          message: this.translate.instant('registerSpecialist.toast.invalidPasswordMessage')
        };
      }
      return {
        title: this.translate.instant('registerSpecialist.toast.incompleteDataTitle'),
        message: this.translate.instant('registerSpecialist.toast.incompleteDataMessage')
      };
    }

    if (status === 409) {
      return {
        title: this.translate.instant('registerSpecialist.toast.existingAccountTitle'),
        message: this.translate.instant('registerSpecialist.toast.existingAccountMessage')
      };
    }

    if (status === 422) {
      return {
        title: this.translate.instant('registerSpecialist.toast.invalidInfoTitle'),
        message: this.translate.instant('registerSpecialist.toast.invalidInfoMessage')
      };
    }

    if (status >= 500) {
      return {
        title: this.translate.instant('registerSpecialist.toast.serverErrorTitle'),
        message: this.translate.instant('registerSpecialist.toast.serverErrorMessage')
      };
    }

    if (status === 0 || status == null) {
      return {
        title: this.translate.instant('registerSpecialist.toast.noConnectionTitle'),
        message: this.translate.instant('registerSpecialist.toast.noConnectionMessage')
      };
    }

    return {
      title: this.translate.instant('registerSpecialist.toast.genericErrorTitle'),
      message: this.translate.instant('registerSpecialist.toast.genericErrorMessage')
    };
  }

  onFileSelected(event: any): void {
    const input = event.target as HTMLInputElement;
    const files = input.files || event.dataTransfer?.files;
    this.addFiles(files);
    input.value = '';
  }

  addFiles(files: FileList | null | undefined) {
    const file = files?.[0];
    if (!file) return;

    if (file.type !== 'application/pdf') {
      this.showToast('warning', this.translate.instant('registerSpecialist.toast.invalidFileTitle'), this.translate.instant('registerSpecialist.toast.invalidPdfMessage'));
      this.resetFileInput(this.certificationsInput);
      return;
    }

    if (file.size > 8 * 1024 * 1024) {
      this.showToast('warning', this.translate.instant('registerSpecialist.toast.fileTooLargeTitle'), this.translate.instant('registerSpecialist.toast.pdfTooLargeMessage'));
      this.resetFileInput(this.certificationsInput);
      return;
    }

    const size = (file.size / (1024 * 1024)).toFixed(2) + ' MB';
    this.fileList = [{ file, size }];
  }

  removeFile(index: number): void {
    this.fileList.splice(index, 1);
    this.resetFileInput(this.certificationsInput);
  }

  onCarnetSelected(event: any): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.showToast('warning', this.translate.instant('registerSpecialist.toast.invalidFileTitle'), this.translate.instant('registerSpecialist.toast.invalidIdPhotoMessage'));
      this.resetFileInput(this.carnetInput);
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.showToast('warning', this.translate.instant('registerSpecialist.toast.fileTooLargeTitle'), this.translate.instant('registerSpecialist.toast.idPhotoTooLargeMessage'));
      this.resetFileInput(this.carnetInput);
      return;
    }

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.carnetFile = { file, url: e.target.result };
      this.cd.detectChanges();
    };
    reader.readAsDataURL(file);
  }

  removeCarnet(): void {
    this.carnetFile = null;
    this.resetFileInput(this.carnetInput);
  }

  tieneEspecialidadSeleccionada(): boolean {
    return this.especialidadesOpciones.some(opt => opt.seleccionado);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    if (event.dataTransfer?.files) {
      this.addFiles(event.dataTransfer.files);
    }
  }

  private resetFileInput(input?: ElementRef<HTMLInputElement>): void {
    if (input?.nativeElement) {
      input.nativeElement.value = '';
    }
  }

  async onSubmit() {
    this.submitted = true;
    this.registerForm.markAllAsTouched();

    if (!(this.registerForm.valid && this.fileList.length > 0 && this.tieneEspecialidadSeleccionada() && this.carnetFile)) {
      this.isLoading = false;

      if (!this.tieneEspecialidadSeleccionada()) {
        this.showToast(
          'warning',
          this.translate.instant('registerSpecialist.toast.specialtyRequiredTitle'),
          this.translate.instant('registerSpecialist.toast.specialtyRequiredMessage')
        );
      } else if (this.fileList.length === 0) {
        this.showToast(
          'warning',
          this.translate.instant('registerSpecialist.toast.documentsRequiredTitle'),
          this.translate.instant('registerSpecialist.toast.documentsRequiredMessage')
        );
      } else if (!this.carnetFile) {
        this.showToast(
          'warning',
          this.translate.instant('registerSpecialist.toast.idPhotoRequiredTitle'),
          this.translate.instant('registerSpecialist.toast.idPhotoRequiredMessage')
        );
      } else {
        this.showToast(
          'warning',
          this.translate.instant('registerSpecialist.toast.formIncompleteTitle'),
          this.translate.instant('registerSpecialist.toast.formIncompleteMessage')
        );
      }

      return;
    }

    this.isLoading = true;

    try {
      const selectedSpecialties = this.especialidadesOpciones
        .filter(opt => opt.seleccionado)
        .map(opt => opt.nombre);
      const carnetUpload = await this.cloudinaryUploadService.uploadImage(
        this.carnetFile!.file,
        'specialists/ci'
      );
      const certificationUpload = await this.cloudinaryUploadService.uploadAuto(
        this.fileList[0].file,
        'specialists/certifications'
      );

      const dataParaBackend: SpecialistRegisterRequest = {
        names: this.registerForm.value.nombre.trim(),
        firstLastname: this.registerForm.value.apellidoPaterno.trim(),
        birthDate: this.registerForm.value.fechaNacimiento,
        ciNumber: String(this.registerForm.value.documento).trim(),
        email: this.registerForm.value.email.trim(),
        password: this.registerForm.value.password.trim(),
        biography: `Especialista en ${selectedSpecialties.join(', ')}`,
        certificationImg: certificationUpload.secure_url,
        ciDocumentImg: carnetUpload.secure_url,
        officeUbi: 'Por definir',
        sessionCost: 1
      };

      const secondLastname = this.registerForm.value.apellidoMaterno?.trim();
      if (secondLastname) {
        dataParaBackend.secondLastname = secondLastname;
      }

      this.api.registerSpecialist(dataParaBackend).subscribe({
        next: (res) => {
          this.isLoading = false;
          this.showToast(
            'success',
            this.translate.instant('registerSpecialist.toast.successTitle'),
            this.translate.instant('registerSpecialist.toast.successMessage')
          );
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.isLoading = false;
          console.error('❌ ERROR BACKEND COMPLETO:', err);
          const { title, message } = this.getFriendlyErrorMessage(err);
          this.showToast('error', title, message);
        }
      });

    } catch (error) {
      this.isLoading = false;
      console.error('❌ ERROR SUBIENDO ARCHIVOS A CLOUDINARY:', error);
      this.showToast(
        'error',
        this.translate.instant('registerSpecialist.toast.uploadErrorTitle'),
        this.translate.instant('registerSpecialist.toast.uploadErrorMessage')
      );
    }
  }
}
