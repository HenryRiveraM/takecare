import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { SupportMaterialItem, SupportMaterialService } from '../../services/support-material.service';

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

    this.supportMaterialService.getCommunityMaterials().subscribe({
      next: (materials) => {
        this.materials = materials;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.errorMsg = 'No se pudieron cargar los materiales disponibles en este momento.';
        this.filteredMaterials = [];
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    const normalizedSearch = this.searchTerm.trim().toLowerCase();

    this.filteredMaterials = this.materials.filter((material) => {
      const matchesType = !this.selectedFileType || material.fileType === this.selectedFileType;
      const matchesSearch =
        !normalizedSearch ||
        material.title.toLowerCase().includes(normalizedSearch) ||
        material.description.toLowerCase().includes(normalizedSearch) ||
        material.specialistName.toLowerCase().includes(normalizedSearch) ||
        material.fileType.toLowerCase().includes(normalizedSearch);

      return matchesType && matchesSearch;
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedFileType = '';
    this.applyFilters();
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
}
