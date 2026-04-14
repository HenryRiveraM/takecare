import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SpecialistDirectoryItem, SpecialistService } from '../../services/specialist.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';

@Component({
  selector: 'app-patient-search-specialists',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-search-specialists.component.html',
  styleUrl: './patient-search-specialists.component.css'
})

export class PatientSearchSpecialistsComponent implements OnInit {
  searchTerm = '';
  selectedCategory = '';
  selectedSchedule = '';

  loading = false;
  errorMsg = '';
  
  specialists: SpecialistDirectoryItem[] = [];
  filteredSpecialists: SpecialistDirectoryItem[] = [];

  constructor(
    private specialistService: SpecialistService,
    private route: ActivatedRoute,
    private router: Router,
    public sidebarService: SidebarService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.searchTerm = params.get('query') ?? '';
      this.selectedCategory = params.get('category') ?? '';
      this.selectedSchedule = params.get('schedule') ?? '';
      this.loadSpecialists();
    });
  }

  onSearch(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        query: this.searchTerm.trim() || null,
        category: this.selectedCategory || null,
        schedule: this.selectedSchedule || null
      },
      queryParamsHandling: 'merge'
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedCategory = '';
    this.selectedSchedule = '';
    this.onSearch();
  }

  trackBySpecialistId(_: number, specialist: SpecialistDirectoryItem): number {
    return specialist.id;
  }

  private loadSpecialists(): void {
    this.loading = true;
    this.errorMsg = '';

    this.specialistService.getAllSpecialists().subscribe({
      next: (apiSpecialists) => {
        this.specialists = apiSpecialists.filter(s => this.isVisibleSpecialist(s));
        this.applyAllFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error cargando especialistas:', error);
        this.errorMsg = 'No se pudieron cargar los especialistas por el momento.';
        this.loading = false;
      }
    });
  }


  private applyAllFilters(): void {
    const term = this.searchTerm.toLowerCase().trim();

    this.filteredSpecialists = this.specialists.filter(s => {
      const matchesTerm = !term || [
        s.names, s.firstLastname, s.secondLastname, s.biography, s.speciality
      ].filter(Boolean).join(' ').toLowerCase().includes(term);

      const matchesCategory = !this.selectedCategory || 
        s.speciality?.toLowerCase() === this.selectedCategory.toLowerCase();
      const matchesSchedule = !this.selectedSchedule || 
        (s as any).shift === this.selectedSchedule;

      return matchesTerm && matchesCategory && matchesSchedule;
    });
  }

  private isVisibleSpecialist(specialist: SpecialistDirectoryItem): boolean {
    const isActive = specialist.status === 1 || specialist.status === true || 
                     specialist.status === null || specialist.status === undefined;
    const isVerified = specialist.accountVerified === 1 || 
                       specialist.accountVerified === undefined || 
                       specialist.accountVerified === null;
    return isActive && isVerified;
  }
}
