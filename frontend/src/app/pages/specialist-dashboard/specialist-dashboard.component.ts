import { Component, OnInit, OnDestroy} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';           
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';

@Component({
  selector: 'app-specialist-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TranslatePipe,
    SidebarComponent,  
  ],
  templateUrl: './specialist-dashboard.component.html',
  styleUrls: ['./specialist-dashboard.component.css']
})
export class SpecialistDashboardComponent implements OnInit, OnDestroy {

  user: any;

  constructor(
    private authService: AuthService,
    public sidebarService: SidebarService 
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.user = this.authService.getUser();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active'); 
  }

}