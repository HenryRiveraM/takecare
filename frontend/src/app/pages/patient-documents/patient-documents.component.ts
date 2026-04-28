import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '../../services/auth.service';
import { ClinicalDocumentService } from '../../services/clinical-document.service';

const ALLOWED_TYPES = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'image/jpeg', 'image/jpg'];
const ALLOWED_EXTENSIONS = ['.pdf', '.doc', '.docx', '.jpg', '.jpeg'];
const MAX_SIZE_MB = 5;

export interface Document {
  id: number;
  name: string;
  size: string;
  type: string;
  uploadedAt: Date;
  url: SafeResourceUrl | string;
}

@Component({
  selector: 'app-patient-documents',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-documents.component.html',
  styleUrls: ['./patient-documents.component.css']
})
export class PatientDocumentsComponent implements OnInit {

  showUploadPanel = false;
  isDragging = false;

  selectedFile: File | null = null;
  uploadError = '';
  documents: any[] = [];
  previewDoc: any | null = null;

  constructor(
    public sidebarService: SidebarService,
    private sanitizer: DomSanitizer,
    private docService: ClinicalDocumentService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    //mocks de prueba
    //this.loadMockDocuments();
    this.loadDocuments();
  }

  loadDocuments(): void {
    const user = this.authService.getUser();
    if (user) {
      this.docService.getDocuments(user.id).subscribe({
        next: (data) => this.documents = data,
        error: (err) => console.error("Error cargando archivos", err)
      });
    }
  }

  openUploadPanel(): void {
    this.showUploadPanel = true;
    this.clearSelectedFile();
  }

  closeUploadPanel(): void {
    this.showUploadPanel = false;
    this.clearSelectedFile();
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(): void {
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.processFile(file);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.processFile(file);
  }

  processFile(file: File): void {
    this.uploadError = '';
    this.selectedFile = null;

    const extension = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
      this.uploadError = `Formato no permitido. Solo se aceptan: PDF, Word, JPG.`;
      return;
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      this.uploadError = `Tipo de archivo no válido.`;
      return;
    }

    const sizeMB = file.size / (1024 * 1024);
    if (sizeMB > MAX_SIZE_MB) {
      this.uploadError = `El archivo supera el límite de ${MAX_SIZE_MB}MB. (${sizeMB.toFixed(1)}MB)`;
      return;
    }

    this.selectedFile = file;
  }

  clearSelectedFile(): void {
    this.selectedFile = null;
    this.uploadError = '';
  }

  uploadDocument(): void {
    const user = this.authService.getUser();
    if (!this.selectedFile || !user) return;

    this.docService.uploadDocument(user.id, this.selectedFile).subscribe({
      next: () => {
        this.loadDocuments();
        this.closeUploadPanel();
        this.selectedFile = null;
      },
      error: (err) => this.uploadError = "Error al subir a Railway"
    });
  }


  deleteDocument(doc: Document): void {
    const user = this.authService.getUser();
    if (!user) return;
    if (confirm(`¿Eliminar ${doc.name}?`)) {
      this.docService.deleteDocument(user.id, doc.id).subscribe(() => {
        this.loadDocuments();
      });
    }
  }
  
  previewDocument(doc: Document): void {
    const user = this.authService.getUser();
    if (!user || !doc.id) return;

    this.docService.downloadDocument(user.id, doc.id).subscribe({
      next: (archivoRecibido: Blob) => {
        const objectUrl = URL.createObjectURL(archivoRecibido);

        this.previewDoc = {
          ...doc,
          url: this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl)
        };
      },
      error: (err) => {
        console.error("No se pudo obtener el archivo del servidor", err);
      }
    });
  }

  closePreview(): void {
    this.previewDoc = null;
  }

  isImage(filename: string): boolean {
    return /\.(jpg|jpeg)$/i.test(filename);
  }

  isPdf(filename: string): boolean {
    return /\.pdf$/i.test(filename);
  }

  isWord(filename: string): boolean {
    return /\.(doc|docx)$/i.test(filename);
  }

  getFileIcon(filename: string): string {
    if (this.isPdf(filename))   return 'fas fa-file-pdf doc-pdf';
    if (this.isWord(filename))  return 'fas fa-file-word doc-word';
    if (this.isImage(filename)) return 'fas fa-file-image doc-image';
    return 'fas fa-file doc-generic';
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private loadMockDocuments(): void {
    this.documents = [
      {
        id: '1',
        name: 'Resultados_laboratorio.pdf',
        size: '1.2 MB',
        type: 'application/pdf',
        uploadedAt: new Date('2025-03-10'),
        url: ''
      },
      {
        id: '2',
        name: 'Receta_medica.jpg',
        size: '450 KB',
        type: 'image/jpeg',
        uploadedAt: new Date('2025-03-15'),
        url: ''
      },
      {
        id: '3',
        name: 'Informe_psicologico.docx',
        size: '320 KB',
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        uploadedAt: new Date('2025-04-01'),
        url: ''
      }
    ];
  }
}