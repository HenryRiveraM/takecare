import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { AuthService } from '../../services/auth.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

const ALLOWED_EXTENSIONS = ['.pdf', '.doc', '.docx'];
const ALLOWED_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
];
const MAX_SIZE_MB = 10;

export interface Resource {
  id: string;
  title: string;
  description: string;
  fileName: string;
  fileUrl: string;
  uploadedAt: Date;
}

interface ResourceForm {
  title: string;
  description: string;
}

interface FieldErrors {
  title: string;
  description: string;
  file: string;
}

@Component({
  selector: 'app-specialist-resources',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslatePipe, SidebarComponent],
  templateUrl: './specialist-resources.component.html',
  styleUrls: ['./specialist-resources.component.css']
})
export class SpecialistResourcesComponent implements OnInit {
  loading = false;
  errorMsg = '';
  showUploadPanel = false;
  isUploading = false;
  isDragging = false;

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  form: ResourceForm = { title: '', description: '' };
  selectedFile: File | null = null;
  fieldErrors: FieldErrors = { title: '', description: '', file: '' };

  resources: Resource[] = [];

  user: any;

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
    this.loadResources();
  }

  loadResources(): void {
    this.loading = true;
    this.errorMsg = '';

    setTimeout(() => {
      this.resources = []; // sin mocks
      this.loading = false;
    }, 500);
  }

  openUploadPanel(): void {
    this.showUploadPanel = true;
    this.resetForm();
  }

  closeUploadPanel(): void {
    this.showUploadPanel = false;
    this.resetForm();
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
    this.fieldErrors.file = '';
    this.selectedFile = null;

    const extension = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
      this.fieldErrors.file = 'resources.errors.invalidFormat';
      return;
    }

    if (!ALLOWED_TYPES.includes(file.type)) {
      this.fieldErrors.file = 'resources.errors.invalidType';
      return;
    }

    const sizeMB = file.size / (1024 * 1024);
    if (sizeMB > MAX_SIZE_MB) {
      this.fieldErrors.file = 'resources.errors.tooLarge';
      return;
    }

    this.selectedFile = file;
  }

  clearSelectedFile(): void {
    this.selectedFile = null;
    this.fieldErrors.file = '';
  }

  validateForm(): boolean {
    let valid = true;
    this.fieldErrors = { title: '', description: '', file: '' };

    if (!this.form.title.trim()) {
      this.fieldErrors.title = 'resources.errors.titleRequired';
      valid = false;
    }

    if (!this.form.description.trim()) {
      this.fieldErrors.description = 'resources.errors.descriptionRequired';
      valid = false;
    }

    if (!this.selectedFile) {
      this.fieldErrors.file = 'resources.errors.fileRequired';
      valid = false;
    }

    return valid;
  }

  clearFieldError(field: keyof FieldErrors): void {
    this.fieldErrors[field] = '';
  }

  uploadResource(): void {
    if (!this.validateForm()) return;

    this.isUploading = true;
    setTimeout(() => {
      const newResource: Resource = {
        id: Date.now().toString(),
        title: this.form.title.trim(),
        description: this.form.description.trim(),
        fileName: this.selectedFile!.name,
        fileUrl: '',
        uploadedAt: new Date(),
      };
      this.resources.unshift(newResource);
      this.closeUploadPanel();
      this.showToastMessage('resources.toast.success', 'success');
      this.isUploading = false;
    }, 1000);
  }

  deleteResource(resource: Resource): void {
    this.resources = this.resources.filter(r => r.id !== resource.id);
    this.showToastMessage('resources.toast.deleted', 'success');
  }

  showToastMessage(messageKey: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = messageKey;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => this.showToast = false, 3000);
  }

  getFileIcon(filename: string): string {
    if (/\.pdf$/i.test(filename))        return 'fas fa-file-pdf doc-pdf';
    if (/\.(doc|docx)$/i.test(filename)) return 'fas fa-file-word doc-word';
    return 'fas fa-file doc-generic';
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private resetForm(): void {
    this.form = { title: '', description: '' };
    this.selectedFile = null;
    this.fieldErrors = { title: '', description: '', file: '' };
    this.isDragging = false;
  }

  previewResource$: Resource | null = null;

  previewResource(resource: Resource): void {
    this.previewResource$ = resource;
    document.body.style.overflow = 'hidden';
  }

  closePreview(): void {
    this.previewResource$ = null;
    document.body.style.overflow = '';
  }

  getSafeUrl(url: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  isPdf(filename: string): boolean {
    return /\.pdf$/i.test(filename);
  }

  isWord(filename: string): boolean {
    return /\.(doc|docx)$/i.test(filename);
  }
}