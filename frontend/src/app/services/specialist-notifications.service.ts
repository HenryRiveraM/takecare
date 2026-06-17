import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import type { Client, IMessage, StompSubscription } from '@stomp/stompjs';

export interface SpecialistNotification {
  id: number;
  sessionId: number;
  specialistId: number;
  patientId?: number;
  carePlanId?: number | null;
  carePlanItemId?: number | null;
  description: string;
  type: number;
  status: number;
  createdDate: string;
  readDate: string | null;
}

export interface SpecialistNotificationSocketEvent {
  eventType: 'NOTIFICATION_CREATED' | 'NOTIFICATION_STATUS_UPDATED';
  unreadCount: number;
  notification: SpecialistNotification;
}

interface UnreadCountResponse {
  specialistId?: number;
  patientId?: number;
  unreadCount: number;
}

export type NotificationAudience = 'specialist' | 'patient';

@Injectable({
  providedIn: 'root'
})
export class SpecialistNotificationsService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/notifications`;
  private readonly socketUrl = `${environment.apiUrl}/ws-takecare`;

  private readonly notificationsSubject = new BehaviorSubject<SpecialistNotification[]>([]);
  private readonly unreadCountSubject = new BehaviorSubject<number>(0);
  private readonly loadingSubject = new BehaviorSubject<boolean>(false);
  private readonly loadedSubject = new BehaviorSubject<boolean>(false);

  private stompClient: Client | null = null;
  private socketSubscription: StompSubscription | null = null;
  private currentUserId: number | null = null;
  private currentAudience: NotificationAudience = 'specialist';
  private pendingSubscriptionUserId: number | null = null;
  private connectingPromise: Promise<void> | null = null;

  constructor(private http: HttpClient) {}

  notificationsStream(): Observable<SpecialistNotification[]> {
    return this.notificationsSubject.asObservable();
  }

  unreadCountStream(): Observable<number> {
    return this.unreadCountSubject.asObservable();
  }

  loadingStream(): Observable<boolean> {
    return this.loadingSubject.asObservable();
  }

  initialize(userId: number, audience: NotificationAudience = 'specialist'): void {
    if (!userId) {
      return;
    }

    if (this.currentUserId !== userId || this.currentAudience !== audience) {
      this.disconnect();
      this.resetState();
      this.currentUserId = userId;
      this.currentAudience = audience;
    }

    this.refreshUnreadCount(userId, audience);
    this.connectAndSubscribe(userId, audience);
  }

  loadNotifications(userId: number, audience: NotificationAudience = 'specialist', force = false): void {
    if (!userId) {
      return;
    }

    if (this.loadedSubject.value && !force) {
      return;
    }

    this.loadingSubject.next(true);

    this.http
      .get<SpecialistNotification[]>(`${this.apiUrl}/${audience}/${userId}`)
      .pipe(
        tap((notifications) => {
          this.notificationsSubject.next(this.mergeNotifications(notifications));
          this.loadedSubject.next(true);
          this.loadingSubject.next(false);
        }),
        catchError((error) => {
          console.error(`Error loading ${audience} notifications:`, error);
          this.loadingSubject.next(false);
          return of([]);
        })
      )
      .subscribe();
  }

  refreshUnreadCount(userId: number, audience: NotificationAudience = 'specialist'): void {
    this.http
      .get<UnreadCountResponse>(`${this.apiUrl}/${audience}/${userId}/unread-count`)
      .pipe(
        tap((response) => {
          this.unreadCountSubject.next(response.unreadCount ?? 0);
        }),
        catchError((error) => {
          console.error(`Error loading ${audience} unread notifications count:`, error);
          return of({ unreadCount: 0 });
        })
      )
      .subscribe((response) => {
        this.unreadCountSubject.next(response.unreadCount ?? 0);
      });
  }


  setReadStatus(
    notificationId: number,
    userId: number,
    read: boolean,
    audience: NotificationAudience = 'specialist'
  ): Observable<SpecialistNotification> {
    const statusRoute = audience === 'patient' ? 'patient-read-status' : 'read-status';
    const recipientPayload = audience === 'patient'
      ? { patientId: userId, read }
      : { specialistId: userId, read };

    return this.http
      .put<SpecialistNotification>(`${this.apiUrl}/${notificationId}/${statusRoute}`, recipientPayload)
      .pipe(
        tap((notification) => {
          this.upsertNotification(notification);
          this.syncUnreadCountFromList();
        })
      );
  }

  disconnect(): void {
    this.socketSubscription?.unsubscribe();
    this.socketSubscription = null;
    this.pendingSubscriptionUserId = null;

    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  resetState(): void {
    this.notificationsSubject.next([]);
    this.unreadCountSubject.next(0);
    this.loadingSubject.next(false);
    this.loadedSubject.next(false);
  }

  private connectAndSubscribe(userId: number, audience: NotificationAudience): void {
    if (!userId) {
      return;
    }

    if (this.stompClient?.connected && this.pendingSubscriptionUserId === userId) {
      return;
    }

    if (!this.stompClient && !this.connectingPromise) {
      this.connectingPromise = this.createClient().finally(() => {
        this.connectingPromise = null;
      });
    }

    if (this.stompClient?.connected) {
      this.subscribeToTopic(userId, audience);
      return;
    }

    this.pendingSubscriptionUserId = userId;
  }

  private subscribeToTopic(userId: number, audience: NotificationAudience): void {
    if (!this.stompClient?.connected) {
      this.pendingSubscriptionUserId = userId;
      return;
    }

    this.socketSubscription?.unsubscribe();
    this.socketSubscription = this.stompClient.subscribe(
      `/topic/notifications/${audience}/${userId}`,
      (message: IMessage) => this.handleSocketEvent(message)
    );

    this.pendingSubscriptionUserId = userId;
  }

  private handleSocketEvent(message: IMessage): void {
    try {
      const event = JSON.parse(message.body) as SpecialistNotificationSocketEvent;

      if (event.notification) {
        this.upsertNotification(event.notification, event.eventType === 'NOTIFICATION_CREATED');
      }

      this.unreadCountSubject.next(event.unreadCount ?? this.countUnread(this.notificationsSubject.value));
    } catch (error) {
      console.error('Error parsing notification socket event:', error);
    }
  }

  private upsertNotification(notification: SpecialistNotification, prepend = false): void {
    const current = this.notificationsSubject.value;
    const existingIndex = current.findIndex((item) => item.id === notification.id);

    if (existingIndex === -1) {
      const nextList = prepend ? [notification, ...current] : [...current, notification];
      this.notificationsSubject.next(this.sortNotifications(nextList));
      return;
    }

    const updated = [...current];
    updated[existingIndex] = notification;
    this.notificationsSubject.next(this.sortNotifications(updated));
  }

  private syncUnreadCountFromList(): void {
    this.unreadCountSubject.next(this.countUnread(this.notificationsSubject.value));
  }

  private countUnread(notifications: SpecialistNotification[]): number {
    return notifications.filter((notification) => notification.status === 0).length;
  }

  private sortNotifications(notifications: SpecialistNotification[]): SpecialistNotification[] {
    return [...notifications].sort((a, b) => {
      const left = new Date(a.createdDate).getTime();
      const right = new Date(b.createdDate).getTime();
      return right - left;
    });
  }

  private mergeNotifications(notifications: SpecialistNotification[]): SpecialistNotification[] {
    const merged = new Map<number, SpecialistNotification>();

    notifications.forEach((notification) => merged.set(notification.id, notification));
    this.notificationsSubject.value.forEach((notification) => merged.set(notification.id, notification));

    return this.sortNotifications(Array.from(merged.values()));
  }

  private async createClient(): Promise<void> {
    const [{ Client }, sockJsModule] = await Promise.all([
      import('@stomp/stompjs'),
      import('sockjs-client')
    ]);

    const SockJS = sockJsModule.default;

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.socketUrl),
      reconnectDelay: 3000,
      debug: () => {}
    });

    this.stompClient.onConnect = () => {
      if (this.currentUserId) {
        this.subscribeToTopic(this.currentUserId, this.currentAudience);
      }
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    this.stompClient.activate();
  }
}
