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
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      apellidoPaterno: ['', [Validators.required, Validators.minLength(3)]],
      apellidoMaterno: [''], 
      fechaNacimiento: ['', Validators.required], 
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      documento: ['', Validators.required],
      complemento: [''],
      aceptaTerminos: [false, Validators.requiredTrue], 
      aceptaComunicaciones: [false] 
    });
  }

  get f() { return this.registerForm.controls; }

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
        this.carnetFile = {
          file: file,
          url: e.target.result
        };
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

  onSubmit() {
    this.submitted = true;

    if (!(this.registerForm.valid && this.fileList.length > 0 && this.tieneEspecialidadSeleccionada() && this.carnetFile)) {
      this.isLoading = false;
      alert('Por favor, completa todos los campos obligatorios, incluyendo documentos y especialidades.');
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
      officeUbi: 'Por definir',
      sessionCost: 1
    };

    const secondLastname = this.registerForm.value.apellidoMaterno?.trim();
    if (secondLastname) {
      dataParaBackend.secondLastname = secondLastname;
    }

    console.log('%c🚀 PROCESANDO REGISTRO...', 'color: #F5A3A3; font-weight: bold;');
    console.table(dataParaBackend);

    this.api.registerSpecialist(dataParaBackend).subscribe({
      next: (res) => {
        this.isLoading = false;
        console.log('✅ REGISTRO DE ESPECIALISTA EXITOSO', res);
        alert('¡Registro enviado con éxito! El administrador revisará su perfil.');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('❌ ERROR BACKEND COMPLETO:', err);
        alert(err?.error?.message || err?.error || 'No se pudo registrar al especialista.');
      }
    });
  }
}