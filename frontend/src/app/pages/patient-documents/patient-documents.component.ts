import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

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

  documents: Document[] = [];

  previewDoc: Document | null = null;

  constructor(
    public sidebarService: SidebarService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.loadMockDocuments(); // quitar cuando el backend esté listo
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
    if (!this.selectedFile) return;

    const url = URL.createObjectURL(this.selectedFile);
    const safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);

    const newDoc: Document = {
      id: Date.now().toString(),
      name: this.selectedFile.name,
      size: this.formatFileSize(this.selectedFile.size),
      type: this.selectedFile.type,
      uploadedAt: new Date(),
      url: safeUrl
    };

    this.documents.unshift(newDoc); 
    this.closeUploadPanel();

  }


  deleteDocument(doc: Document): void {
    this.documents = this.documents.filter(d => d.id !== doc.id);
  }

  previewDocument(doc: Document): void {
    this.previewDoc = doc;
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