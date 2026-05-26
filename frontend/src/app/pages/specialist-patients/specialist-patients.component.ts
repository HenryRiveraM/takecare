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

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private specialistService: SpecialistService
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
}
