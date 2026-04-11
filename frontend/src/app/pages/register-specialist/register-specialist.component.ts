import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ApiService, SpecialistRegisterRequest } from '../../services/api.service';

@Component({
  selector: 'app-register-specialist',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, TranslatePipe],
  templateUrl: './register-specialist.component.html',
  styleUrl: './register-specialist.component.css'
})
export class RegisterSpecialistComponent implements OnInit {
  registerForm!: FormGroup;
  submitted = false;
  isDragging = false;
  isLoading: boolean = false;

  fileList: { file: File, size: string }[] = [];
  carnetFile: { file: File, url: string } | null = null;

  toast: { visible: boolean; type: 'error' | 'success' | 'warning'; title: string; message: string } = {
    visible: false,
    type: 'error',
    title: '',
    message: ''
  };
  private toastTimer: any;

  especialidadesOpciones = [
    { id: 'mental', nombre: 'Trastornos mentales', seleccionado: false },
    { id: 'adicciones', nombre: 'Adicciones', seleccionado: false },
    { id: 'familia', nombre: 'Terapia familiar', seleccionado: false },
    { id: 'infantil', nombre: 'Psicología infantil', seleccionado: false },
    { id: 'depresion', nombre: 'Depresión y ansiedad', seleccionado: false },
    { id: 'ocupacional', nombre: 'Terapia ocupacional', seleccionado: false }
  ];

  constructor(
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private api: ApiService,
    private router: Router,
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
      fechaNacimiento: ['', Validators.required],
      email: ['', [
        Validators.required, 
        Validators.email]],
      password: ['', [
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
    });
  }

  get f() { return this.registerForm.controls; }

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
    const files = event.target.files || event.dataTransfer?.files;
    this.addFiles(files);
  }

  addFiles(files: FileList): void {
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (file.type === 'application/pdf') {
        const size = (file.size / (1024 * 1024)).toFixed(2) + ' MB';
        if (!this.fileList.find(f => f.file.name === file.name)) {
          this.fileList.push({ file, size });
        }
      }
    }
  }

  removeFile(index: number): void {
    this.fileList.splice(index, 1);
  }

  onCarnetSelected(event: any): void {
    const file = event.target.files[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.carnetFile = { file, url: e.target.result };
        this.cd.detectChanges();
      };
      reader.readAsDataURL(file);
    }
  }

  removeCarnet(): void {
    this.carnetFile = null;
  }

  tieneEspecialidadSeleccionada(): boolean {
    return this.especialidadesOpciones.some(opt => opt.seleccionado);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    if (event.dataTransfer?.files) {
      this.addFiles(event.dataTransfer.files);
    }
  }

  onSubmit(): void {
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

    const selectedSpecialties = this.especialidadesOpciones
      .filter(opt => opt.seleccionado)
      .map(opt => opt.nombre);

    const dataParaBackend: SpecialistRegisterRequest = {
      names: this.registerForm.value.nombre.trim(),
      firstLastname: this.registerForm.value.apellidoPaterno.trim(),
      birthDate: this.registerForm.value.fechaNacimiento,
      ciNumber: String(this.registerForm.value.documento).trim(),
      email: this.registerForm.value.email.trim(),
      password: this.registerForm.value.password.trim(),
      biography: `Especialista en ${selectedSpecialties.join(', ')}`,
      certificationImg: this.fileList.map(fileItem => fileItem.file.name).join(', '),
      ciDocumentImg: this.carnetFile?.file.name || '',
      officeUbi: 'Por definir',
      sessionCost: 1
    };

    const secondLastname = this.registerForm.value.apellidoMaterno?.trim();
    if (secondLastname) {
      dataParaBackend.secondLastname = secondLastname;
    }

    console.log('Payload enviado: ', dataParaBackend);

    this.api.registerSpecialist(dataParaBackend).subscribe({
      next: (res) => {
        this.isLoading = false;
        console.log('✅ REGISTRO DE ESPECIALISTA EXITOSO', res);
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
  }
}