import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {

  role: 'patient' | 'specialist' | 'admin' | null = null;

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    public sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.detectRole();
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.detectRole();
      });
  }

  detectRole(): void {
    const segments = this.router.url.split('/').filter(Boolean);
    const firstSegment = segments[0];

    if (firstSegment === 'patient') {
      this.role = 'patient';
    } else if (firstSegment === 'specialist') {
      this.role = 'specialist';
    } else if (firstSegment === 'admin') {
      this.role = 'admin';
    } else {
      this.role = null;
    }

    this.cdr.detectChanges();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}