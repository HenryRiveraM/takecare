import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';
import {
  SpecialistPatient,
  SpecialistService
} from '../../services/specialist.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { PatientFilesService } from '../../services/patient-files.service';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'app-specialist-patients',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './specialist-patients.component.html',
  styleUrls: ['./specialist-patients.component.css']
})
export class SpecialistPatientsComponent implements OnInit, OnDestroy {

  patients: SpecialistPatient[] = [];
  totalPatients = 0;
  searchTerm = '';
  loading = false;
  errorMsg = '';

  showProfileModal = false;
  selectedPatient: any = null;
  patientDocs: any[] = [];
  previewDoc: any = null;
  loadingProfile = false;

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private specialistService: SpecialistService,
    private patientFilesService: PatientFilesService,
    private http: HttpClient,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.loadPatients();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active');
  }

  get filteredPatients(): SpecialistPatient[] {
    const term = this.searchTerm.trim().toLocaleLowerCase();

    if (!term) {
      return this.patients;
    }

    return this.patients.filter(patient =>
      patient.fullName.toLocaleLowerCase().includes(term) ||
      (patient.email || '').toLocaleLowerCase().includes(term)
    );
  }

  getInitials(fullName: string): string {
    return fullName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part.charAt(0))
      .join('')
      .toUpperCase() || '?';
  }

  asLocalDate(value: string | null): Date | null {
    return value ? new Date(`${value}T00:00:00`) : null;
  }

  private loadPatients(): void {
    const specialistId = Number(this.authService.getUser()?.id || 0);

    if (!specialistId) {
      this.errorMsg = 'specialistPatients.errors.noSpecialist';
      return;
    }

    this.loading = true;
    this.specialistService.getPatients(specialistId).subscribe({
      next: response => {
        this.patients = response.patients || [];
        this.totalPatients = response.totalPatients || 0;
        this.loading = false;
      },
      error: () => {
        this.errorMsg = 'specialistPatients.errors.load';
        this.loading = false;
      }
    });
  }

  openPatientProfile(patientId: number): void {
    this.loadingProfile = true;
    this.selectedPatient = null;
    this.patientDocs = [];
    this.previewDoc = null;
    this.showProfileModal = true;

    const apiUrl = this.specialistService['baseUrl'] || 'http://localhost:8080';
    this.http.get<any>(`${apiUrl}/api/v1/patients/${patientId}`).subscribe({
      next: (patient) => {
        this.selectedPatient = patient;
        this.loadingProfile = false;
      },
      error: (err) => {
        console.error('Error fetching patient profile details', err);
        this.loadingProfile = false;
      }
    });

    this.patientFilesService.getDocuments(patientId).subscribe({
      next: (docs) => {
        this.patientDocs = docs.map(doc => ({
          id: String(doc.id),
          name: doc.fileName,
          size: this.formatFileSize(doc.fileSize),
          type: doc.contentType,
          uploadedAt: new Date(doc.uploadedAt),
          url: ''
        }));
      },
      error: (err) => {
        console.error('Error fetching patient documents', err);
      }
    });
  }

  closeProfileModal(): void {
    this.showProfileModal = false;
    this.selectedPatient = null;
    this.patientDocs = [];
    this.previewDoc = null;
  }

  previewPatientDoc(doc: any): void {
    if (doc.url) {
      this.previewDoc = doc;
      return;
    }

    const patientId = this.selectedPatient?.id;
    if (!patientId) return;

    this.patientFilesService.downloadDocument(patientId, Number(doc.id)).subscribe({
      next: (response) => {
        const rawBlob = response.body!;
        const mimeType = response.headers.get('Content-Type') || doc.type || 'application/octet-stream';
        // Reconstruct blob with the correct MIME type so browser can render it
        const typedBlob = new Blob([rawBlob], { type: mimeType.split(';')[0].trim() });
        const objectUrl = URL.createObjectURL(typedBlob);
        doc.url = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
        this.previewDoc = doc;
      },
      error: (err) => {
        console.error('Error loading patient document preview', err);
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
}
