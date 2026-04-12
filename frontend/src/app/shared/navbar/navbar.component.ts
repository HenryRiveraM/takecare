import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';
import { AuthService } from '../../services/auth.service';

type NavbarMode = 'hidden' | 'public' | 'private';
type PrivateArea = 'patient' | 'specialist' | 'admin' | null;

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
})
export class NavbarComponent implements OnInit {

  navbarMode: NavbarMode = 'public';
  privateArea: PrivateArea = null;
  currentUrl = '';
  user: { names?: string } | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    public languageService: LanguageService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.updateNavbarState();

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateNavbarState();
      });
  }

  changeLanguage(event: Event): void {
    const lang = (event.target as HTMLSelectElement).value as 'es' | 'en';
    this.languageService.setLanguage(lang);
  }

  logout(): void {
    this.authService.logout();
    this.user = null;
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  getPrivateHomeRoute(): string {
    if (this.privateArea === 'admin') {
      return '/admin';
    }

    if (this.privateArea === 'specialist') {
      return '/specialist';
    }

    return '/patient';
  }

  private updateNavbarState(): void {
    this.user = this.authService.getUser();
    this.currentUrl = this.router.url.split('?')[0];

    const segments = this.currentUrl.split('/').filter(Boolean);
    const firstSegment = segments[0] ?? '';
    const secondSegment = segments[1] ?? '';

    if (firstSegment === 'admin') {
      this.privateArea = 'admin';
      this.navbarMode = 'private';
      return;
    }

    if (firstSegment === 'patient' || firstSegment === 'specialist') {
      this.privateArea = firstSegment as PrivateArea;
      this.navbarMode = 'private';
      return;
    }

    this.privateArea = null;
    this.navbarMode = this.shouldHidePublicNavbar() ? 'hidden' : 'public';
  }

  private shouldHidePublicNavbar(): boolean {
    let currentRoute = this.route.firstChild;

    while (currentRoute?.firstChild) {
      currentRoute = currentRoute.firstChild;
    }

    return currentRoute?.snapshot.data['showNavbar'] === false;
  }
}
