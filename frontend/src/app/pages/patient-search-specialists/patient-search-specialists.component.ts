import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
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
  daysOfWeek = [
    { label: 'patientSearch.filters.monday', value: 'MONDAY' },
    { label: 'patientSearch.filters.tuesday', value: 'TUESDAY' },
    { label: 'patientSearch.filters.wednesday', value: 'WEDNESDAY' },
    { label: 'patientSearch.filters.thursday', value: 'THURSDAY' },
    { label: 'patientSearch.filters.friday', value: 'FRIDAY' },
    { label: 'patientSearch.filters.saturday', value: 'SATURDAY' },
    { label: 'patientSearch.filters.sunday', value: 'SUNDAY' }
  ];

  constructor(
    private specialistService: SpecialistService,
    private route: ActivatedRoute,
    private router: Router,
    public sidebarService: SidebarService,
    private translateService: TranslateService,
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

  getDisplayName(specialist: SpecialistDirectoryItem): string {
    return specialist.fullName?.trim() || [
      specialist.names,
      specialist.firstLastname,
      specialist.secondLastname
    ]
      .filter(Boolean)
      .join(' ')
      .trim();
  }

  getInitials(specialist: SpecialistDirectoryItem): string {
    const displayName = this.getDisplayName(specialist);
    const initials = displayName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');

    return initials || 'TC';
  }

  getSpecialtiesLabel(specialist: SpecialistDirectoryItem): string {
    const specialties = specialist.specialties?.filter(Boolean) ?? [];
    if (specialties.length > 0) {
      return specialties.map((specialty) => this.translateSpecialty(specialty)).join(', ');
    }

    return this.translateSpecialty(specialist.speciality?.trim() || '');
  }

  private loadSpecialists(): void {
    this.loading = true;
    this.errorMsg = '';

    this.specialistService.searchSpecialists(this.selectedCategory, this.selectedSchedule).subscribe({
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

    if (!term) {
      this.filteredSpecialists = [...this.specialists];
      return;
    }

    this.filteredSpecialists = this.specialists.filter(s => {
      const matchesTerm = !term || [
        this.getDisplayName(s),
        s.names,
        s.firstLastname,
        s.secondLastname,
        s.biography,
        s.speciality,
        this.getSpecialtiesLabel(s),
        s.officeUbi
      ].filter(Boolean).join(' ').toLowerCase().includes(term);

      const matchesCategory = !this.selectedCategory || 
        this.getSpecialtiesLabel(s).toLowerCase().includes(this.selectedCategory.toLowerCase());
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

  private translateSpecialty(specialty: string): string {
    const key = this.getSpecialtyTranslationKey(specialty);
    return key ? this.translateService.instant(key) : specialty;
  }

  private getSpecialtyTranslationKey(specialty: string): string | null {
    const normalized = this.normalizeText(specialty);

    const specialtyMap: Record<string, string> = {
      'trastornos mentales': 'patientSearch.filters.mentalHealth',
      'adicciones': 'patientSearch.filters.addictions',
      'terapia familiar': 'patientSearch.filters.familyTherapy',
      'psicologia infantil': 'patientSearch.filters.childPsychology',
      'depresion y ansiedad': 'patientSearch.filters.depressionAnxiety',
      'terapia ocupacional': 'patientSearch.filters.occupationalTherapy',

    };

    return specialtyMap[normalized] || null;
  }

  private normalizeText(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
