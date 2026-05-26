import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { LanguageService } from '../../services/language.service';
import { AuthService, LoginResponse } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';
import { NotificationAudience, SpecialistNotificationsService } from '../../services/specialist-notifications.service';
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
  notificationCount = 0;
  notificationsOpen = false;

  private notificationsSubscription?: Subscription;
  private notificationsInitialized = false;

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
    if ((this.privateArea !== 'specialist' && this.privateArea !== 'patient') || !this.user?.id) {
      return;
    }

    this.notificationsOpen = !this.notificationsOpen;

    if (this.notificationsOpen) {
      const audience = this.privateArea as NotificationAudience;
      this.specialistNotificationsService.loadNotifications(this.user.id, audience, true);
      this.specialistNotificationsService.refreshUnreadCount(this.user.id, audience);
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

      this.initializeNotifications(firstSegment as NotificationAudience);

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

  private initializeNotifications(audience: NotificationAudience): void {
    const userId = this.user?.id;

    if (!userId) {
      return;
    }

    if (!this.notificationsInitialized) {
      this.notificationsSubscription = this.specialistNotificationsService
        .unreadCountStream()
        .subscribe((count) => {
          this.notificationCount = count;
        });

      this.notificationsInitialized = true;
    }

    this.specialistNotificationsService.initialize(userId, audience);
  }

  private disconnectSpecialistNotifications(): void {
    this.notificationCount = 0;

    if (!this.notificationsInitialized) {
      return;
    }

    this.specialistNotificationsService.disconnect();
    this.specialistNotificationsService.resetState();
    this.notificationsSubscription?.unsubscribe();
    this.notificationsSubscription = undefined;
    this.notificationsInitialized = false;
  }
}
