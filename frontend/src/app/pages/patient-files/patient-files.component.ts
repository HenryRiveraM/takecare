import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PatientFilesService } from '../../services/patient-files.service';
import { AuthService } from '../../services/auth.service';

const ALLOWED_TYPES = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'image/jpeg', 'image/jpg'];
const ALLOWED_EXTENSIONS = ['.pdf', '.doc', '.docx', '.jpg', '.jpeg'];
const MAX_SIZE_MB = 5;

export interface Document {
  id: string;
  name: string;
  size: string;
  type: string;
  uploadedAt: Date;
  url: SafeResourceUrl | string;
}

@Component({
  selector: 'app-patient-files',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-files.component.html',
  styleUrls: ['./patient-files.component.css']
})
export class PatientFilesComponent implements OnInit {

  showUploadPanel = false;
  isDragging = false;

  selectedFile: File | null = null;
  uploadError = '';

  documents: Document[] = [];

  previewDoc: Document | null = null;
  patientId = 0;

  constructor(
    public sidebarService: SidebarService,
    private sanitizer: DomSanitizer,
    private translate: TranslateService,
    private patientFilesService: PatientFilesService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.patientId = Number(this.authService.getUser()?.id || 0);
    if (this.patientId) {
      this.loadDocuments();
    }
  }

  loadDocuments(): void {
    this.patientFilesService.getDocuments(this.patientId).subscribe({
      next: (docs) => {
        this.documents = docs.map(doc => ({
          id: String(doc.id),
          name: doc.fileName,
          size: this.formatFileSize(doc.fileSize),
          type: doc.contentType,
          uploadedAt: new Date(doc.uploadedAt),
          url: ''
        }));
      },
      error: (err) => {
        console.error('Error loading documents', err);
      }
    });
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
      this.uploadError = this.translate.instant('patientFiles.errors.invalidFormat');
      return;
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      this.uploadError = this.translate.instant('patientFiles.errors.invalidType');
      return;
    }

    const sizeMB = file.size / (1024 * 1024);
    if (sizeMB > MAX_SIZE_MB) {
      this.uploadError = this.translate.instant('patientFiles.errors.tooLarge', {
        limit: MAX_SIZE_MB,
        size: sizeMB.toFixed(1)
      });
      return;
    }

    this.selectedFile = file;
  }

  clearSelectedFile(): void {
    this.selectedFile = null;
    this.uploadError = '';
  }

  uploadDocument(): void {
    if (!this.selectedFile || !this.patientId) return;

    this.patientFilesService.uploadDocument(this.patientId, this.selectedFile).subscribe({
      next: (savedDoc) => {
        const url = URL.createObjectURL(this.selectedFile!);
        const safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);

        const newDoc: Document = {
          id: String(savedDoc.id),
          name: savedDoc.fileName,
          size: this.formatFileSize(savedDoc.fileSize),
          type: savedDoc.contentType,
          uploadedAt: new Date(savedDoc.uploadedAt),
          url: safeUrl
        };

        this.documents.unshift(newDoc); 
        this.closeUploadPanel();
      },
      error: (err) => {
        console.error('Upload failed', err);
        this.uploadError = this.translate.instant('patientFiles.errors.uploadFailed') || 'Error al subir el documento';
      }
    });
  }


  deleteDocument(doc: Document): void {
    if (!this.patientId) return;

    const confirmMsg = this.translate.instant('patientFiles.actions.confirmDelete') || '¿Estás seguro de que deseas eliminar este documento?';
    if (confirm(confirmMsg)) {
      this.patientFilesService.deleteDocument(this.patientId, Number(doc.id)).subscribe({
        next: () => {
          this.documents = this.documents.filter(d => d.id !== doc.id);
        },
        error: (err) => {
          console.error('Delete failed', err);
        }
      });
    }
  }

  previewDocument(doc: Document): void {
    if (doc.url) {
      this.previewDoc = doc;
      return;
    }

    this.patientFilesService.downloadDocument(this.patientId, Number(doc.id)).subscribe({
      next: (response) => {
        const rawBlob = response.body!;
        const mimeType = response.headers.get('Content-Type') || doc.type || 'application/octet-stream';
        // Reconstruct blob with correct MIME type so browser renders it properly
        const typedBlob = new Blob([rawBlob], { type: mimeType.split(';')[0].trim() });
        const objectUrl = URL.createObjectURL(typedBlob);
        doc.url = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
        this.previewDoc = doc;
      },
      error: (err) => {
        console.error('Download/preview failed', err);
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
