import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { LanguageService } from '../../services/language.service';
import { AuthService, LoginResponse } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';
import { SpecialistNotificationsService } from '../../services/specialist-notifications.service';
import { SpecialistNotificationsSidebarComponent } from '../specialist-notifications-sidebar/specialist-notifications-sidebar.component';

type NavbarMode = 'hidden' | 'public' | 'private';
type PrivateArea = 'patient' | 'specialist' | 'admin' | null;

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe, SpecialistNotificationsSidebarComponent],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
})

export class NavbarComponent implements OnInit, OnDestroy {

  navbarMode: NavbarMode = 'public';
  privateArea: PrivateArea = null;
  isDashboard: boolean = false;
  currentUrl = '';
  user: LoginResponse | null = null;
  specialistNotificationCount = 0;
  notificationsOpen = false;

  private notificationsSubscription?: Subscription;
  private specialistNotificationsInitialized = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    public languageService: LanguageService,
    private authService: AuthService,
    public sidebarService: SidebarService,
    private specialistNotificationsService: SpecialistNotificationsService,
  ) {}

  ngOnInit(): void {
    this.updateNavbarState();

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateNavbarState();
      });
  }

  ngOnDestroy(): void {
    this.notificationsSubscription?.unsubscribe();
    this.specialistNotificationsService.disconnect();
  }

  changeLanguage(event: Event): void {
    const lang = (event.target as HTMLSelectElement).value as 'es' | 'en';
    this.languageService.setLanguage(lang);
  }

  logout(): void {
    this.notificationsOpen = false;
    this.specialistNotificationsService.disconnect();
    this.authService.logout();
    this.user = null;
    this.router.navigate(['/login'], { replaceUrl: true });
  }

  toggleNotifications(): void {
    if (this.privateArea !== 'specialist' || !this.user?.id) {
      return;
    }

    this.notificationsOpen = !this.notificationsOpen;

    if (this.notificationsOpen) {
      this.specialistNotificationsService.loadNotifications(this.user.id);
      this.specialistNotificationsService.refreshUnreadCount(this.user.id);
    }
  }

  closeNotifications(): void {
    this.notificationsOpen = false;
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

    this.isDashboard = ['patient', 'specialist', 'admin'].includes(firstSegment);

    if (firstSegment === 'admin') {
      this.notificationsOpen = false;
      this.privateArea = 'admin';
      this.navbarMode = 'private';
      this.disconnectSpecialistNotifications();
      return;
    }

    if (firstSegment === 'patient' || firstSegment === 'specialist') {
      this.privateArea = firstSegment as PrivateArea;
      this.navbarMode = 'private';

      if (this.privateArea === 'specialist') {
        this.initializeSpecialistNotifications();
      } else {
        this.notificationsOpen = false;
        this.disconnectSpecialistNotifications();
      }

      return;
    }

    this.isDashboard = false;
    this.privateArea = null;
    this.notificationsOpen = false;
    this.disconnectSpecialistNotifications();
    this.navbarMode = this.shouldHidePublicNavbar() ? 'hidden' : 'public';
  }

  private shouldHidePublicNavbar(): boolean {
    let currentRoute = this.route.firstChild;

    while (currentRoute?.firstChild) {
      currentRoute = currentRoute.firstChild;
    }

    return currentRoute?.snapshot.data['showNavbar'] === false;
  }

  toggleSidebar() {
    this.sidebarService.toggle();
  }

  private initializeSpecialistNotifications(): void {
    const specialistId = this.user?.id;

    if (!specialistId) {
      return;
    }

    if (!this.specialistNotificationsInitialized) {
      this.notificationsSubscription = this.specialistNotificationsService
        .unreadCountStream()
        .subscribe((count) => {
          this.specialistNotificationCount = count;
        });

      this.specialistNotificationsInitialized = true;
    }

    this.specialistNotificationsService.initialize(specialistId);
  }

  private disconnectSpecialistNotifications(): void {
    this.specialistNotificationCount = 0;

    if (!this.specialistNotificationsInitialized) {
      return;
    }

    this.specialistNotificationsService.disconnect();
    this.specialistNotificationsService.resetState();
    this.notificationsSubscription?.unsubscribe();
    this.notificationsSubscription = undefined;
    this.specialistNotificationsInitialized = false;
  }
}
