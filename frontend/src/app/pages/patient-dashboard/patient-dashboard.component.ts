import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms'; 
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';
import { ViewChild } from '@angular/core';  
import { RatingModalComponent, RatingSubmitData } from '../../shared/rating-modal/rating-modal.component';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule,SidebarComponent,RatingModalComponent],
  templateUrl: './patient-dashboard.component.html',
  styleUrls: ['./patient-dashboard.component.css']
})

export class PatientDashboardComponent implements OnInit {
  user: any;
  searchTerm: string = '';

  constructor(private authService: AuthService, private router: Router, public sidebarService: SidebarService) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
  }

  goToSpecialistSearch(): void {
    if (this.searchTerm.trim()) {
      this.router.navigate(['/patient/search-specialists'], { 
        queryParams: { q: this.searchTerm } 
      });
    }
  }

  @ViewChild('ratingModal') ratingModal!: RatingModalComponent;

  // Para abrir el modal (cuando una cita termina):
  openRatingModal(): void {
    this.ratingModal.open(
      'appointment-123',   // id de la cita
      42,                  // id del especialista
      'Dr. Juan Pérez'     // nombre del especialista
    );
  }

  onRated(data: RatingSubmitData): void {
    console.log('Calificación enviada:', data);
    // TODO: mostrar toast de éxito
  }

  onModalClosed(): void {
    console.log('Modal cerrado');
  }
}
