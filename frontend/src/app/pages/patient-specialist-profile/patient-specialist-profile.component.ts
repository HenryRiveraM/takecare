import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { catchError, map, of, switchMap } from 'rxjs';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { SpecialistDirectoryItem, SpecialistService } from '../../services/specialist.service';
import { SupportMaterialItem, SupportMaterialService } from '../../services/support-material.service';

interface SpecialistProfileViewModel extends SpecialistDirectoryItem {
  biography: string;
  officeUbi: string;
  sessionCost?: number;
}

@Component({
  selector: 'app-patient-specialist-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-specialist-profile.component.html',
  styleUrls: ['./patient-specialist-profile.component.css']
})
export class PatientSpecialistProfileComponent implements OnInit {
  loading = false;
  errorMsg = '';
  specialist: SpecialistProfileViewModel | null = null;
  materials: SupportMaterialItem[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private specialistService: SpecialistService,
    private supportMaterialService: SupportMaterialService,
    private translateService: TranslateService,
    public sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        map((params) => Number(params.get('id'))),
        switchMap((specialistId) => {
          if (!specialistId) {
            this.errorMsg = 'No se encontró el especialista solicitado.';
            return of(null);
          }

          this.loading = true;
          this.errorMsg = '';

          const stateSpecialist = history.state?.specialist as SpecialistDirectoryItem | undefined;

          return this.specialistService.getAllSpecialists().pipe(
            map((specialists) => {
              const directorySpecialist = specialists.find((item) => item.id === specialistId) || null;
              const seed = directorySpecialist || (stateSpecialist?.id === specialistId ? stateSpecialist : null);
              return { specialistId, seed };
            }),
            catchError(() => of({
              specialistId,
              seed: stateSpecialist?.id === specialistId ? stateSpecialist : null
            }))
          );
        })
      )
      .subscribe({
        next: (result) => {
          if (!result) {
            this.loading = false;
            return;
          }

          this.loadSpecialistProfile(result.specialistId, result.seed);
        },
        error: () => {
          this.errorMsg = 'No se pudo cargar el perfil del especialista.';
          this.loading = false;
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/patient/search-specialists']);
  }

  goToSearchWithName(): void {
    this.router.navigate(['/patient/search-specialists'], {
      queryParams: {
        query: this.getDisplayName()
      }
    });
  }

  scrollToSection(sectionId: string): void {
    if (typeof document === 'undefined') {
      return;
    }

    const target = document.getElementById(sectionId);
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  getDisplayName(): string {
    if (!this.specialist) {
      return '';
    }

    return this.specialist.fullName?.trim() || [
      this.specialist.names,
      this.specialist.firstLastname,
      this.specialist.secondLastname
    ]
      .filter(Boolean)
      .join(' ')
      .trim();
  }

  getInitials(): string {
    const initials = this.getDisplayName()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');

    return initials || 'TC';
  }

  hasVerifiedBadge(): boolean {
    return this.specialist?.accountVerified === 1 || this.specialist?.accountVerified === undefined;
  }

  getSpecialtyItems(): string[] {
    if (!this.specialist) {
      return [];
    }

    const specialties = this.specialist.specialties?.filter(Boolean) ?? [];
    if (specialties.length > 0) {
      return specialties.map((specialty) => this.translateSpecialty(specialty));
    }

    const single = this.translateSpecialty(this.specialist.speciality?.trim() || '');
    return single ? [single] : [];
  }

  getSpecialtiesLabel(): string {
    if (!this.specialist) {
      return '';
    }

    const specialties = this.specialist.specialties?.filter(Boolean) ?? [];
    if (specialties.length > 0) {
      return specialties.map((specialty) => this.translateSpecialty(specialty)).join(', ');
    }

    return this.translateSpecialty(this.specialist.speciality?.trim() || '');
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

  private loadSpecialistProfile(specialistId: number, seed: SpecialistDirectoryItem | null): void {
    this.specialistService.getProfile(specialistId).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (profileData: any) => {
        const merged = this.mergeSpecialistData(specialistId, seed, profileData);

        if (!merged) {
          this.errorMsg = 'No se pudo cargar el perfil del especialista.';
          this.loading = false;
          return;
        }

        this.specialist = merged;
        this.loadMaterials(merged);
      },
      error: () => {
        this.errorMsg = 'No se pudo cargar el perfil del especialista.';
        this.loading = false;
      }
    });
  }

  private loadMaterials(specialist: SpecialistProfileViewModel): void {
    this.supportMaterialService
      .getMaterialsBySpecialist(specialist.id, this.getDisplayName())
      .subscribe({
        next: (materials) => {
          this.materials = materials;
          this.loading = false;
        },
        error: () => {
          this.materials = [];
          this.loading = false;
        }
      });
  }

  private mergeSpecialistData(
    specialistId: number,
    seed: SpecialistDirectoryItem | null,
    profileData: any
  ): SpecialistProfileViewModel | null {
    if (!seed && !profileData) {
      return null;
    }

    return {
      id: specialistId,
      names: profileData?.names || seed?.names || '',
      firstLastname: profileData?.firstLastname || seed?.firstLastname || '',
      secondLastname: profileData?.secondLastname || seed?.secondLastname || '',
      fullName: seed?.fullName || [
        profileData?.names,
        profileData?.firstLastname,
        profileData?.secondLastname
      ].filter(Boolean).join(' ').trim(),
      email: profileData?.email || seed?.email,
      biography: profileData?.biography || seed?.biography || '',
      officeUbi: profileData?.officeUbi || seed?.officeUbi || '',
      sessionCost: profileData?.sessionCost ?? seed?.sessionCost ?? undefined,
      speciality: seed?.speciality || '',
      specialties: seed?.specialties || [],
      shift: seed?.shift,
      reputationAverage: seed?.reputationAverage,
      certificationImg: seed?.certificationImg,
      status: seed?.status,
      accountVerified: seed?.accountVerified
    };
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
      'terapia ocupacional': 'patientSearch.filters.occupationalTherapy'
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
