import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService, SpecialistRegisterRequest } from '../../services/api.service';
import { CloudinaryUploadService } from '../../services/cloudinary-upload.service';

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

  maxDateAdult: string = (() => {
    const date = new Date();
    date.setFullYear(date.getFullYear() - 18); 
    return date.toISOString().split('T')[0];
  })();

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
  private router: Router,
  private cloudinaryUploadService: CloudinaryUploadService
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
    const file = files[0];
    if (!file) return;

    if (file.type !== 'application/pdf') {
      this.showToast('warning', 'Archivo inválido', 'Solo se permite un archivo PDF.');
      return;
    }

    if (file.size > 8 * 1024 * 1024) {
      this.showToast('warning', 'Archivo muy grande', 'El PDF no debe superar los 8 MB.');
      return;
    }

    const size = (file.size / (1024 * 1024)).toFixed(2) + ' MB';
    this.fileList = [{ file, size }];
  }

  removeFile(index: number) {
    this.fileList.splice(index, 1);
  }

  onCarnetSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.showToast('warning', 'Archivo inválido', 'El carnet debe ser una imagen.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.showToast('warning', 'Archivo muy grande', 'La imagen del carnet no debe superar los 5 MB.');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.carnetFile = { file, url: e.target.result };
      this.cd.detectChanges();
    };
    reader.readAsDataURL(file);
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

  async onSubmit() {
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

    try {
      const selectedSpecialties = this.especialidadesOpciones
        .filter(opt => opt.seleccionado)
        .map(opt => opt.nombre);

      console.log('📤 Iniciando subida de carnet a Cloudinary...');
      const carnetUpload = await this.cloudinaryUploadService.uploadImage(
        this.carnetFile!.file,
        'specialists/ci'
      );
      console.log('✅ Carnet subido correctamente:', carnetUpload);

      console.log('📤 Iniciando subida de certificación PDF a Cloudinary...');
      const certificationUpload = await this.cloudinaryUploadService.uploadAuto(
        this.fileList[0].file,
        'specialists/certifications'
      );
      console.log('✅ Certificación subida correctamente:', certificationUpload);

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

      console.log('Payload enviado: ', dataParaBackend);

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

    } catch (error) {
      this.isLoading = false;
      console.error('❌ ERROR SUBIENDO ARCHIVOS A CLOUDINARY:', error);
      this.showToast('error', 'Error al subir archivos', 'No se pudo subir el carnet o el PDF de certificación.');
    }
  }
}