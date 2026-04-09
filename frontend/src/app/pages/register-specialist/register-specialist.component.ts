import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService, SpecialistRegisterRequest } from '../../services/api.service';

@Component({
  selector: 'app-register-specialist',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
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

  // Toast notification state
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
  private router: Router
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

  // ── Toast helpers ──────────────────────────────────────────────────────────

  showToast(type: 'error' | 'success' | 'warning', title: string, message: string) {
    clearTimeout(this.toastTimer);
    this.toast = { visible: true, type, title, message };
    this.toastTimer = setTimeout(() => this.closeToast(), 5000);
  }

  closeToast() {
    this.toast.visible = false;
  }

  /** Maps backend HTTP errors to friendly Spanish messages */
  private getFriendlyErrorMessage(err: any): { title: string; message: string } {
    const status = err?.status;
    const backendMessage: string = err?.error?.message || err?.error?.error || '';

    if (status === 400) {
      if (backendMessage.toLowerCase().includes('email')) {
        return {
          title: 'Correo inválido',
          message: 'El correo electrónico ingresado no es válido o ya está en uso. Verifica e intenta de nuevo.'
        };
      }
      if (backendMessage.toLowerCase().includes('documento') || backendMessage.toLowerCase().includes('ci')) {
        return {
          title: 'Documento duplicado',
          message: 'El número de documento ingresado ya está registrado en el sistema.'
        };
      }
      if (backendMessage.toLowerCase().includes('password')) {
        return {
          title: 'Contraseña inválida',
          message: 'La contraseña no cumple con los requisitos mínimos de seguridad (mínimo 6 caracteres).'
        };
      }
      return {
        title: 'Datos incompletos',
        message: 'Revisa que todos los campos obligatorios estén correctamente llenados antes de continuar.'
      };
    }

    if (status === 409) {
      return {
        title: 'Ya existe una cuenta',
        message: 'Ya hay un especialista registrado con ese correo o documento de identidad.'
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

  // ── File handling ──────────────────────────────────────────────────────────

  onFileSelected(event: any) {
    const files = event.target.files || event.dataTransfer?.files;
    this.addFiles(files);
  }

  addFiles(files: FileList) {
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

  removeFile(index: number) {
    this.fileList.splice(index, 1);
  }

  onCarnetSelected(event: any) {
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

  removeCarnet() {
    this.carnetFile = null;
  }

  tieneEspecialidadSeleccionada(): boolean {
    return this.especialidadesOpciones.some(opt => opt.seleccionado);
  }

  // ── Drag & drop ────────────────────────────────────────────────────────────

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
    if (event.dataTransfer?.files) {
      this.addFiles(event.dataTransfer.files);
    }
  }

  // ── Submit ──
  onSubmit() {
    this.submitted = true;
    this.registerForm.markAllAsTouched();

    if (!(this.registerForm.valid && this.fileList.length > 0 && this.tieneEspecialidadSeleccionada() && this.carnetFile)) {
      this.isLoading = false;

      if (!this.tieneEspecialidadSeleccionada()) {
        this.showToast('warning', 'Especialidad requerida', 'Selecciona al menos una especialidad para continuar.');
      } else if (this.fileList.length === 0) {
        this.showToast('warning', 'Documentos requeridos', 'Debes subir al menos un certificado o credencial en formato PDF.');
      } else if (!this.carnetFile) {
        this.showToast('warning', 'Foto del carnet requerida', 'Sube una foto del anverso de tu carnet de identidad.');
      } else {
        this.showToast('warning', 'Formulario incompleto', 'Revisa que todos los campos obligatorios estén correctamente llenados.');
      }
      
      console.log('registerForm.valid:', this.registerForm.valid);
      console.log('Especialidades seleccionadas:', this.especialidadesOpciones.filter(opt => opt.seleccionado).map(opt => opt.nombre));
      console.log('Archivos seleccionados:', this.fileList);
      console.log('Carnet seleccionado:', this.carnetFile);

      Object.keys(this.registerForm.controls).forEach(key => {
        const control = this.registerForm.get(key);
        console.log(key, {
          value: control?.value,
          valid: control?.valid,
          errors: control?.errors
        });
      });
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

    console.log('Payload enviado: ',dataParaBackend);

    this.api.registerSpecialist(dataParaBackend).subscribe({
      next: (res) => {
        this.isLoading = false;
        console.log('✅ REGISTRO DE ESPECIALISTA EXITOSO', res);
        this.showToast('success', 'Registro exitoso', 'Tu perfil será revisado y te notificaremos por correo.');
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