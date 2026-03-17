import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';

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

  // --- Estado de Archivos ---
  fileList: { file: File, size: string }[] = []; // Para los PDFs
  carnetFile: { file: File, url: string } | null = null; // Para la foto del carnet
  selfieArchivo: File | null = null; // Para el archivo real de la selfie
  selfiePreview: string | null = null; // Para la vista previa de la selfie

  // --- Opciones de Especialidad ---
  especialidadesOpciones = [
    { id: 'mental', nombre: 'Trastornos mentales', seleccionado: false },
    { id: 'adicciones', nombre: 'Adicciones', seleccionado: false },
    { id: 'familia', nombre: 'Terapia familiar', seleccionado: false },
    { id: 'infantil', nombre: 'Psicología infantil', seleccionado: false },
    { id: 'depresion', nombre: 'Depresión y ansiedad', seleccionado: false },
    { id: 'ocupacional', nombre: 'Terapia ocupacional', seleccionado: false }
  ];

  constructor(private fb: FormBuilder, private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      apellidoPaterno: ['', [Validators.required, Validators.minLength(3)]],
      apellidoMaterno: [''], // Opcional
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      documento: ['', Validators.required],
      complemento: [''],
      aceptaTerminos: [false, Validators.requiredTrue], // Campo obligatorio: requiredTrue asegura que el checkbox esté marcado
      aceptaComunicaciones: [false] // Campo opcional (marketing/comunicaciones)
    });
  }

  // Getter para facilitar el acceso a errores en el HTML
  get f() { return this.registerForm.controls; }

  // --- Lógica de Certificaciones (PDFs) ---
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

  // --- Lógica de Foto Carnet ---
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

  // --- Lógica de Foto Selfie ---
  onSelfieSelected(event: any) {
    const file = event.target.files[0];
    if (file && file.type.startsWith('image/')) {
      this.selfieArchivo = file;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.selfiePreview = e.target.result;
        this.cd.detectChanges();
      };
      reader.readAsDataURL(file);
    }
  }

  removeSelfie() {
    this.selfieArchivo = null;
    this.selfiePreview = null;
  }

  // --- Validaciones Auxiliares ---
  tieneEspecialidadSeleccionada(): boolean {
    return this.especialidadesOpciones.some(opt => opt.seleccionado);
  }

  // --- Drag & Drop Helpers ---
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

  // --- Envío Final ---
  onSubmit() {
    this.submitted = true;

    if (this.registerForm.valid && this.fileList.length > 0 && this.tieneEspecialidadSeleccionada() && this.carnetFile && this.selfiePreview) {
      
      // 1. OBJETO PARA LA BASE DE DATOS (El que enviarás al servidor)
      const dataParaBackend = {
        ...this.registerForm.value,
        especialidades: this.especialidadesOpciones
          .filter(opt => opt.seleccionado)
          .map(opt => opt.nombre), // Enviamos un ARRAY: ["Mental", "Adicciones"]
        
        // En la vida real, aquí enviarías los archivos reales (File), no solo el nombre
        certificaciones: this.fileList.map(f => f.file.name), 
        fotoCarnet: this.carnetFile?.file.name,
        fotoSelfie: this.selfieArchivo?.name
      };

      // 2. OBJETO SOLO PARA LA CONSOLA (Para que tú lo veas lindo en la tabla)
      const visualDebug = {
        ...dataParaBackend,
        especialidades: dataParaBackend.especialidades.join(', '),
        certificaciones: dataParaBackend.certificaciones.join(' | ')
      };

      console.log('%c🚀 DATOS LISTOS PARA EL BACKEND', 'color: #22c55e; font-weight: bold;');
      console.table(visualDebug); // Tabla limpia
      console.log('Objeto Real:', dataParaBackend); // Objeto con Arrays para el programador

    } else {
      alert('Revisa los campos obligatorios.');
    }
  }
}
