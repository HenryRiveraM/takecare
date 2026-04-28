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
  specialistId: number;
  unreadCount: number;
}

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
  private currentSpecialistId: number | null = null;
  private pendingSubscriptionSpecialistId: number | null = null;
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

  initialize(specialistId: number): void {
    if (!specialistId) {
      return;
    }

    if (this.currentSpecialistId !== specialistId) {
      this.resetState();
      this.currentSpecialistId = specialistId;
    }

    this.refreshUnreadCount(specialistId);
    this.connectAndSubscribe(specialistId);
  }

  loadNotifications(specialistId: number, force = false): void {
    if (!specialistId) {
      return;
    }

    if (this.loadedSubject.value && !force) {
      return;
    }

    this.loadingSubject.next(true);

    this.http
      .get<SpecialistNotification[]>(`${this.apiUrl}/specialist/${specialistId}`)
      .pipe(
        tap((notifications) => {
          this.notificationsSubject.next(this.sortNotifications(notifications));
          this.loadedSubject.next(true);
          this.loadingSubject.next(false);
        }),
        catchError((error) => {
          console.error('Error loading specialist notifications:', error);
          this.loadingSubject.next(false);
          return of([]);
        })
      )
      .subscribe();
  }

  refreshUnreadCount(specialistId: number): void {
    this.http
      .get<UnreadCountResponse>(`${this.apiUrl}/specialist/${specialistId}/unread-count`)
      .pipe(
        tap((response) => {
          this.unreadCountSubject.next(response.unreadCount ?? 0);
        }),
        catchError((error) => {
          console.error('Error loading unread notifications count:', error);
          return of({ specialistId, unreadCount: 0 });
        })
      )
      .subscribe((response) => {
        this.unreadCountSubject.next(response.unreadCount ?? 0);
      });
  }

  setReadStatus(notificationId: number, specialistId: number, read: boolean): Observable<SpecialistNotification> {
    return this.http
      .put<SpecialistNotification>(`${this.apiUrl}/${notificationId}/read-status`, {
        specialistId,
        read
      })
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
    this.pendingSubscriptionSpecialistId = null;

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

  private connectAndSubscribe(specialistId: number): void {
    if (!specialistId) {
      return;
    }

    if (this.stompClient?.connected && this.pendingSubscriptionSpecialistId === specialistId) {
      return;
    }

    if (!this.stompClient && !this.connectingPromise) {
      this.connectingPromise = this.createClient().finally(() => {
        this.connectingPromise = null;
      });
    }

    if (this.stompClient?.connected) {
      this.subscribeToTopic(specialistId);
      return;
    }

    this.pendingSubscriptionSpecialistId = specialistId;
  }

  private subscribeToTopic(specialistId: number): void {
    if (!this.stompClient?.connected) {
      this.pendingSubscriptionSpecialistId = specialistId;
      return;
    }

    this.socketSubscription?.unsubscribe();
    this.socketSubscription = this.stompClient.subscribe(
      `/topic/notifications/specialist/${specialistId}`,
      (message: IMessage) => this.handleSocketEvent(message)
    );

    this.pendingSubscriptionSpecialistId = specialistId;
  }

  private handleSocketEvent(message: IMessage): void {
    try {
      const event = JSON.parse(message.body) as SpecialistNotificationSocketEvent;

      if (event.notification) {
        this.upsertNotification(event.notification, event.eventType === 'NOTIFICATION_CREATED');
      }

      this.unreadCountSubject.next(event.unreadCount ?? this.countUnread(this.notificationsSubject.value));
    } catch (error) {
      console.error('Error parsing specialist notification socket event:', error);
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
      if (this.currentSpecialistId) {
        this.subscribeToTopic(this.currentSpecialistId);
      }
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers['message'], frame.body);
    };

    this.stompClient.activate();
  }
}
