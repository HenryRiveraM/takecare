import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SpecialistLocationResponse, SpecialistService } from '../../services/specialist.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LocationVisibility, SpecialistLocationPreferences, SpecialistLocationPreferencesService } from '../../services/specialist-location-preferences.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SupportMaterialItem, SupportMaterialService } from '../../services/support-material.service';

interface SpecialistProfileForm {
  names: string;
  firstLastname: string;
  secondLastname: string;
  email: string;
  biography: string;
  officeUbi: string;
  sessionCost: number | null;
}

interface SupportMaterialForm {
  title: string;
  description: string;
  fileUrl: string;
}

@Component({
  selector: 'app-specialist-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, SidebarComponent],
  templateUrl: './specialist-profile.component.html',
  styleUrls: ['./specialist-profile.component.css']
})
export class SpecialistProfileComponent implements OnInit {

  user: any;

  profile: SpecialistProfileForm = {
    names: '',
    firstLastname: '',
    secondLastname: '',
    email: '',
    biography: '',
    officeUbi: '',
    sessionCost: null,
  };

  originalProfile: SpecialistProfileForm = { ...this.profile };

  locationDetails: SpecialistLocationPreferences = this.createEmptyLocationDetails();
  originalLocationDetails: SpecialistLocationPreferences = this.createEmptyLocationDetails();

  loading = false;
  successMsg = '';
  errorMsg = '';
  locationErrorMsg = '';
  isEditing = false;
  materials: SupportMaterialItem[] = [];
  isEditingMaterial = false;
  editingMaterialId: string | null = null;
  materialErrorMsg = '';
  materialSuccessMsg = '';

  materialForm: SupportMaterialForm = {
    title: '',
    description: '',
    fileUrl: ''
  };

  constructor(
    private specialistService: SpecialistService,
    private authService: AuthService,
    private router: Router,
    private translate: TranslateService,
    private specialistLocationPreferencesService: SpecialistLocationPreferencesService,
    public sidebarService: SidebarService,
    private supportMaterialService: SupportMaterialService
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
    console.log('Usuario obtenido:', this.user);
    if (this.user?.id) {
      this.loadProfile();
      this.loadMaterials();
    } else {
      this.errorMsg = 'No se pudo obtener el ID del usuario';
      console.error('User ID no disponible');
    }
  }

  loadProfile() {
    this.loading = true;
    this.errorMsg = '';
    console.log(`Cargando perfil del especialista ID: ${this.user.id}`);
    
    this.specialistService.getProfile(this.user.id).subscribe({
      next: (data: any) => {
        console.log('Perfil cargado:', data);

        this.profile = {
          names: data.names || '',
          firstLastname: data.firstLastname || '',
          secondLastname: data.secondLastname || '',
          email: data.email || '',
          biography: data.biography || '',
          officeUbi: data.officeUbi || '',
          sessionCost: data.sessionCost || 0
        };

        this.loadLocationDetails();
        this.originalProfile = { ...this.profile };
        this.loading = false;
      },
      error: (error: any) => {
        console.error('Error cargando perfil:', error);
        this.errorMsg = 'Error al cargar el perfil. Verifica la conexión.';
        this.loading = false;
      }
    });
  }

  enableEdit(): void {
    this.isEditing = true;
    this.successMsg = '';
    this.errorMsg = '';
    this.locationErrorMsg = '';
  }

  loadMaterials(): void {
    if (!this.user?.id) {
      return;
    }

    this.materials = this.supportMaterialService.getBySpecialistId(this.user.id);
  }

  startMaterialCreate(): void {
    this.isEditingMaterial = true;
    this.editingMaterialId = null;
    this.materialErrorMsg = '';
    this.materialSuccessMsg = '';
    this.materialForm = {
      title: '',
      description: '',
      fileUrl: ''
    };
  }

  startMaterialEdit(material: SupportMaterialItem): void {
    this.isEditingMaterial = true;
    this.editingMaterialId = material.id;
    this.materialErrorMsg = '';
    this.materialSuccessMsg = '';
    this.materialForm = {
      title: material.title,
      description: material.description,
      fileUrl: material.fileUrl
    };
  }

  cancelMaterialEdit(): void {
    this.isEditingMaterial = false;
    this.editingMaterialId = null;
    this.materialErrorMsg = '';
    this.materialForm = {
      title: '',
      description: '',
      fileUrl: ''
    };
  }

  saveMaterial(): void {
    if (!this.user?.id) {
      return;
    }

    if (!this.materialForm.title.trim()) {
      this.materialErrorMsg = this.translate.instant('specialistProfile.materials.validation.titleRequired');
      return;
    }

    if (!this.materialForm.description.trim()) {
      this.materialErrorMsg = this.translate.instant('specialistProfile.materials.validation.descriptionRequired');
      return;
    }

    if (!this.materialForm.fileUrl.trim()) {
      this.materialErrorMsg = this.translate.instant('specialistProfile.materials.validation.fileUrlRequired');
      return;
    }

    this.supportMaterialService.save(
      this.user.id,
      {
        title: this.materialForm.title.trim(),
        description: this.materialForm.description.trim(),
        fileUrl: this.materialForm.fileUrl.trim()
      },
      this.editingMaterialId ?? undefined
    );

    this.loadMaterials();
    this.materialSuccessMsg = this.translate.instant('specialistProfile.materials.success');
    this.cancelMaterialEdit();
  }

  deleteMaterial(materialId: string): void {
    if (!this.user?.id) {
      return;
    }

    this.supportMaterialService.delete(this.user.id, materialId);
    this.loadMaterials();
    this.materialSuccessMsg = this.translate.instant('specialistProfile.materials.deleted');
  }

  cancelEdit(): void {
    this.profile = { ...this.originalProfile };
    this.locationDetails = { ...this.originalLocationDetails };
    this.isEditing = false;
    this.successMsg = '';
    this.errorMsg = '';
    this.locationErrorMsg = '';
  }

  goBackToProfile(): void {
  this.router.navigate(['/specialist']);
  }

  save() : void {
    if (!this.isEditing) return;

    const validationError = this.validateLocationDetails();
    if (validationError) {
      this.locationErrorMsg = validationError;
      this.errorMsg = '';
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';
    this.locationErrorMsg = '';

    if (this.hasLocationData()) {
      this.specialistService.updateLocation(this.user.id, {
        addressLine: this.locationDetails.addressLine.trim(),
        city: this.locationDetails.city.trim(),
        neighborhood: this.locationDetails.neighborhood.trim(),
        reference: this.locationDetails.reference.trim(),
        visibility: this.locationDetails.visibility
      }).subscribe({
        next: (locationResponse) => {
          this.applyLocationResponse(locationResponse);
          this.updateProfileData();
        },
        error: (error: any) => {
          console.error('Error actualizando ubicación:', error);
          this.errorMsg = error?.error?.message || 'Error al actualizar la ubicación del especialista';
          this.loading = false;
        }
      });
      return;
    }

    this.profile.officeUbi = '';
    this.specialistLocationPreferencesService.clearByUserId(this.user.id);
    this.updateProfileData();
  }

  private loadLocationDetails(): void {
    const persistedLocation = this.user?.id
      ? this.specialistLocationPreferencesService.getByUserId(this.user.id)
      : null;

    const parsedOfficeLocation = this.parseOfficeLocation(this.profile.officeUbi);

    this.locationDetails = {
      addressLine: persistedLocation?.addressLine || parsedOfficeLocation.addressLine,
      city: persistedLocation?.city || parsedOfficeLocation.city,
      neighborhood: persistedLocation?.neighborhood || parsedOfficeLocation.neighborhood,
      reference: persistedLocation?.reference || parsedOfficeLocation.reference,
      visibility: persistedLocation?.visibility || parsedOfficeLocation.visibility
    };

    this.originalLocationDetails = { ...this.locationDetails };
    this.profile.officeUbi = this.buildOfficeLocationSummary(this.locationDetails);
  }

  private validateLocationDetails(): string {
    if (!this.hasLocationData()) {
      return '';
    }

    if (!this.locationDetails.addressLine.trim()) {
      return this.translate.instant('specialistProfile.validation.addressRequired');
    }

    if (!this.locationDetails.city.trim()) {
      return this.translate.instant('specialistProfile.validation.cityRequired');
    }

    return '';
  }

  private updateProfileData(): void {
    const payload = {
      names: this.profile.names.trim(),
      firstLastname: this.profile.firstLastname.trim(),
      secondLastname: this.profile.secondLastname?.trim() ? this.profile.secondLastname.trim() : null,
      email: this.profile.email.trim(),
      biography: this.profile.biography?.trim() || '',
      officeUbi: this.profile.officeUbi?.trim() || '',
      sessionCost: this.profile.sessionCost
    };

    this.specialistService.updateProfile(this.user.id, payload).subscribe({
      next: (response: any) => {
        console.log('Perfil actualizado:', response);

        this.originalProfile = { ...this.profile };
        this.originalLocationDetails = { ...this.locationDetails };
        this.successMsg = 'Perfil actualizado correctamente';
        this.isEditing = false;
        this.loading = false;

        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (error: any) => {
        console.error('Error actualizando perfil:', error);
        this.errorMsg = error?.error?.message || 'Error al actualizar el perfil';
        this.loading = false;
      }
    });
  }

  private buildOfficeLocationSummary(location: SpecialistLocationPreferences): string {
    const parts = [
      location.addressLine.trim(),
      location.city.trim(),
      location.neighborhood.trim() ? `Zona ${location.neighborhood.trim()}` : '',
      location.reference.trim() ? `Ref. ${location.reference.trim()}` : ''
    ].filter(Boolean);

    return parts.join(' | ');
  }

  private parseOfficeLocation(officeUbi: string): SpecialistLocationPreferences {
    if (!officeUbi.trim()) {
      return this.createEmptyLocationDetails();
    }

    const parts = officeUbi.includes('|||TC|||')
      ? officeUbi.split('|||TC|||').map(item => item.trim())
      : officeUbi.split('|').map(item => item.trim()).filter(Boolean);

    return {
      addressLine: parts[0] || officeUbi,
      city: parts[1] || '',
      neighborhood: this.stripPrefix(parts[2], 'Zona'),
      reference: this.stripPrefix(parts[3], 'Ref.'),
      visibility: 'private'
    };
  }

  private stripPrefix(value: string | undefined, prefix: string): string {
    return value ? value.replace(prefix, '').trim() : '';
  }

  private createEmptyLocationDetails(): SpecialistLocationPreferences {
    return {
      addressLine: '',
      city: '',
      neighborhood: '',
      reference: '',
      visibility: 'private' as LocationVisibility
    };
  }

  private hasLocationData(): boolean {
    return [
      this.locationDetails.addressLine,
      this.locationDetails.city,
      this.locationDetails.neighborhood,
      this.locationDetails.reference
    ].some(value => value.trim().length > 0);
  }

  private applyLocationResponse(response: SpecialistLocationResponse): void {
    this.locationDetails = {
      addressLine: response.addressLine || '',
      city: response.city || '',
      neighborhood: response.neighborhood || '',
      reference: response.reference || '',
      visibility: response.visibility === 'public' ? 'public' : 'private'
    };

    this.profile.officeUbi = response.officeUbi || this.buildOfficeLocationSummary(this.locationDetails);

    if (response.visibilityPersisted) {
      this.specialistLocationPreferencesService.clearByUserId(this.user.id);
    } else {
      this.specialistLocationPreferencesService.saveByUserId(this.user.id, this.locationDetails);
    }
  }
}
