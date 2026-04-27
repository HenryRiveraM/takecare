import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import {
  SpecialistNotification,
  SpecialistNotificationsService
} from '../../services/specialist-notifications.service';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-specialist-notifications-sidebar',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './specialist-notifications-sidebar.component.html',
  styleUrl: './specialist-notifications-sidebar.component.css'
})
export class SpecialistNotificationsSidebarComponent implements OnChanges {
  @Input() specialistId: number | null = null;
  @Input() isOpen = false;
  @Output() closePanel = new EventEmitter<void>();

  notifications: SpecialistNotification[] = [];
  loading = false;

  constructor(
    private notificationsService: SpecialistNotificationsService,
    private languageService: LanguageService
  ) {
    this.notificationsService.notificationsStream().subscribe((notifications) => {
      this.notifications = notifications;
    });

    this.notificationsService.loadingStream().subscribe((loading) => {
      this.loading = loading;
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']?.currentValue && this.specialistId) {
      this.notificationsService.loadNotifications(this.specialistId);
      this.notificationsService.refreshUnreadCount(this.specialistId);
    }
  }

  close(): void {
    this.closePanel.emit();
  }

  toggleReadStatus(notification: SpecialistNotification): void {
    if (!this.specialistId) {
      return;
    }

    const shouldMarkAsRead = notification.status === 0;
    this.notificationsService
      .setReadStatus(notification.id, this.specialistId, shouldMarkAsRead)
      .subscribe({
        error: (error) => {
          console.error('Error updating specialist notification status:', error);
        }
      });
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
