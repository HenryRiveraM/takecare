import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SpecialistDirectoryItem, SpecialistService } from '../../services/specialist.service';

@Component({
  selector: 'app-patient-search-specialists',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe],
  templateUrl: './patient-search-specialists.component.html',
  styleUrl: './patient-search-specialists.component.css'
})
export class PatientSearchSpecialistsComponent implements OnInit {
  searchTerm = '';
  loading = false;
  errorMsg = '';
  specialists: SpecialistDirectoryItem[] = [];
  filteredSpecialists: SpecialistDirectoryItem[] = [];

  constructor(
    private specialistService: SpecialistService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.searchTerm = params.get('query')?.trim() ?? '';
      this.loadSpecialists();
    });
  }

  onSearch(): void {
    const query = this.searchTerm.trim();
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: query ? { query } : {},
      queryParamsHandling: ''
    });
  }

  trackBySpecialistId(_: number, specialist: SpecialistDirectoryItem): number {
    return specialist.id;
  }

  private loadSpecialists(): void {
    this.loading = true;
    this.errorMsg = '';

    this.specialistService.getAllSpecialists().subscribe({
      next: (specialists) => {
        this.specialists = specialists.filter(specialist => this.isVisibleSpecialist(specialist));
        this.filteredSpecialists = this.filterSpecialists(this.searchTerm);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error cargando especialistas:', error);
        this.errorMsg = 'No se pudieron cargar los especialistas por el momento.';
        this.loading = false;
      }
    });
  }

  private filterSpecialists(term: string): SpecialistDirectoryItem[] {
    const normalizedTerm = term.trim().toLowerCase();

    if (!normalizedTerm) {
      return [...this.specialists];
    }

    return this.specialists.filter((specialist) => {
      const searchableFields = [
        specialist.names,
        specialist.firstLastname,
        specialist.secondLastname,
        specialist.biography,
        specialist.officeUbi,
        specialist.speciality
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return searchableFields.includes(normalizedTerm);
    });
  }

  private isVisibleSpecialist(specialist: SpecialistDirectoryItem): boolean {
    const isActive =
      specialist.status === 1 ||
      specialist.status === true ||
      specialist.status === null ||
      specialist.status === undefined;
    const isVerified =
      specialist.accountVerified === 1 ||
      specialist.accountVerified === undefined ||
      specialist.accountVerified === null;

    return isActive && isVerified;
  }
}
