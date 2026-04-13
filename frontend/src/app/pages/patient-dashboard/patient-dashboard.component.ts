import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms'; // 👈 Importante para el buscador
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { SidebarService } from '../../services/sidebar.service';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, TranslateModule,SidebarComponent],
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
}
