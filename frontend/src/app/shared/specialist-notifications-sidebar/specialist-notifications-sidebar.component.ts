import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import {
  NotificationAudience,
  SpecialistNotification,
  SpecialistNotificationsService
} from '../../services/specialist-notifications.service';
import { LanguageService } from '../../services/language.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-specialist-notifications-sidebar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './specialist-notifications-sidebar.component.html',
  styleUrl: './specialist-notifications-sidebar.component.css'
})
export class SpecialistNotificationsSidebarComponent implements OnChanges {
  @Input() userId: number | null = null;
  @Input() audience: NotificationAudience = 'specialist';
  @Input() isOpen = false;
  @Output() closePanel = new EventEmitter<void>();

  notifications: SpecialistNotification[] = [];
  loading = false;

  constructor(
    private notificationsService: SpecialistNotificationsService,
    private languageService: LanguageService,
    private router: Router
  ) {
    this.notificationsService.notificationsStream().subscribe((notifications) => {
      this.notifications = notifications.filter(n => n.status === 0);
    });

    this.notificationsService.loadingStream().subscribe((loading) => {
      this.loading = loading;
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']?.currentValue && this.userId) {
      this.notificationsService.loadNotifications(this.userId, this.audience);
      this.notificationsService.refreshUnreadCount(this.userId, this.audience);
    }
  }

  close(): void {
    this.closePanel.emit();
  }

  markAsReadAndRedirect(notification: SpecialistNotification): void {
    if (!this.userId) {
      return;
    }

    this.notificationsService
      .setReadStatus(notification.id, this.userId, true, this.audience)
      .subscribe({
        next: () => {
          if (this.isCarePlanNotification(notification)) {
            this.router.navigate([this.getCarePlanRoute()], {
              queryParams: { highlightPlanId: notification.carePlanId }
            });
          } else {
            const targetRoute = this.audience === 'patient' ? '/patient/appointments' : '/specialist/appointments';
            this.router.navigate([targetRoute]);
          }
          this.close();
        },
        error: (error) => {
          console.error('Error updating notification status:', error);
        }
      });
  }

  get translationKey(): string {
    return this.audience === 'patient' ? 'patientNotifications' : 'specialistNotifications';
  }

  getActionLabel(notification: SpecialistNotification): string {
    if (this.isCarePlanNotification(notification)) {
      return this.translationKey + '.actions.viewPlan';
    }

    return this.translationKey + '.actions.viewAppointment';
  }

  isCarePlanNotification(notification: SpecialistNotification): boolean {
    return !!notification.carePlanId;
  }

  private getCarePlanRoute(): string {
    return this.audience === 'patient' ? '/patient/care-plans' : '/specialist/care-plans';
  }

  trackByNotificationId(_: number, notification: SpecialistNotification): number {
    return notification.id;
  }

  formatDate(value: string): string {
    if (!value) {
      return '';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    const locale = this.languageService.getCurrentLanguage() === 'en' ? 'en-US' : 'es-BO';

    return new Intl.DateTimeFormat(locale, {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  }
}
