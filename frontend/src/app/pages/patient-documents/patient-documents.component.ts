import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { SupportMaterialItem, SupportMaterialListResponse, SupportMaterialService } from '../../services/support-material.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-patient-documents',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-documents.component.html',
  styleUrls: ['./patient-documents.component.css']
})
export class PatientDocumentsComponent implements OnInit {
  searchTerm = '';
  selectedFileType = '';
  loading = false;
  errorMsg = '';
  materials: SupportMaterialItem[] = [];
  filteredMaterials: SupportMaterialItem[] = [];
  totalDocuments = 0;
  readonly fileTypeOptions = ['PDF', 'DOCX'];

  constructor(
    public sidebarService: SidebarService,
    private supportMaterialService: SupportMaterialService
  ) {}

  ngOnInit(): void {
    this.loadMaterials();
  }

  loadMaterials(): void {
    this.loading = true;
    this.errorMsg = '';

    this.supportMaterialService
      .getCommunityMaterials(this.searchTerm, this.selectedFileType)
      .subscribe({
        next: (response: SupportMaterialListResponse) => {
          this.materials = response.materials;
          this.filteredMaterials = response.materials;
          this.totalDocuments = response.totalDocuments;
          this.loading = false;
        },
        error: () => {
          this.errorMsg = 'No se pudieron cargar los materiales disponibles en este momento.';
          this.filteredMaterials = [];
          this.totalDocuments = 0;
          this.loading = false;
        }
      });
  }

  applyFilters(): void {
    this.loadMaterials();
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedFileType = '';
    this.loadMaterials();
  }

  getFileIcon(fileType: string): string {
    switch (fileType.toUpperCase()) {
      case 'PDF':
        return 'fas fa-file-pdf';
      case 'DOCX':
      case 'DOC':
        return 'fas fa-file-word';
      default:
        return 'fas fa-file-lines';
    }
  }

  getFileToneClass(fileType: string): string {
    switch (fileType.toUpperCase()) {
      case 'PDF':
        return 'tone-pdf';
      case 'DOCX':
      case 'DOC':
        return 'tone-doc';
      default:
        return 'tone-generic';
    }
  }

  trackByMaterialId(_: number, material: SupportMaterialItem): number {
    return material.id;
  }

  getFullUrl(url?: string | null): string {
    if (!url) {
      return '';
    }

    return url.startsWith('http') ? url : `${environment.apiUrl}${url}`;
  }
}
