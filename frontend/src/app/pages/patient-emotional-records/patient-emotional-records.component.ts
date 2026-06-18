import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import {
  EmotionalRecord,
  EmotionalRecordService
} from '../../services/emotional-record.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';
import { EmotionalRecordModalComponent } from '../../shared/emotional-record-modal/emotional-record-modal.component';

@Component({
  selector: 'app-patient-emotional-records',
  standalone: true,
  imports: [CommonModule, TranslatePipe, SidebarComponent, EmotionalRecordModalComponent],
  templateUrl: './patient-emotional-records.component.html',
  styleUrls: ['./patient-emotional-records.component.css']
})
export class PatientEmotionalRecordsComponent implements OnInit, OnDestroy {
  patientId = 0;
  records: EmotionalRecord[] = [];
  displayedRecords: EmotionalRecord[] = [];
  loading = false;
  saving = false;
  errorMsg = '';
  today = new Date();
  showRecordModal = false;
  hasRegisteredToday = false;
  todayRegistrationTime: Date | null = null;
  
  readonly recordsPerPage = 5;
  recordsDisplayed = 0;

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  constructor(
    public sidebarService: SidebarService,
    private authService: AuthService,
    private emotionalRecordService: EmotionalRecordService
  ) {}

  ngOnInit(): void {
    document.body.classList.add('dashboard-active');
    this.patientId = Number(this.authService.getUser()?.id || 0);

    if (!this.patientId) {
      this.errorMsg = 'emotionalLog.errors.noPatient';
      return;
    }

    this.loadRecords();
    this.checkIfRegisteredToday();
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active');
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  openRecordModal(): void {
    this.showRecordModal = true;
  }

  closeRecordModal(): void {
    this.showRecordModal = false;
  }

  handleRecordSaved(saved: EmotionalRecord): void {
    this.records = this.sortRecords([saved, ...this.records]);
    this.recordsDisplayed = 0;
    this.updateDisplayedRecords();
    this.showRecordModal = false;
    this.showToastMessage('emotionalLog.toast.saved', 'success');
    localStorage.setItem(this.getEmotionSessionKey(), 'true');
    this.hasRegisteredToday = true;
    this.todayRegistrationTime = new Date();
  }

  handleRecordSaveFailed(message: string): void {
    this.showToastMessage(message || 'emotionalLog.errors.save', 'error');
  }

  getMoodLabel(moodLevel: number | undefined): string {
    const labels: Record<number, string> = {
      1: 'emotionalLog.moods.veryLow',
      2: 'emotionalLog.moods.low',
      3: 'emotionalLog.moods.neutral',
      4: 'emotionalLog.moods.good',
      5: 'emotionalLog.moods.excellent'
    };

    return labels[Number(moodLevel || 0)] || 'emotionalLog.moods.unknown';
  }

  getMoodIcon(moodLevel: number | undefined): string {
    const icons: Record<number, string> = {
      1: 'sentiment_very_dissatisfied',
      2: 'sentiment_dissatisfied',
      3: 'sentiment_neutral',
      4: 'sentiment_satisfied',
      5: 'sentiment_very_satisfied'
    };

    return icons[Number(moodLevel || 0)] || 'sentiment_neutral';
  }

  getRecordsLabel(): string {
    const count = this.records.length;
    return count === 1 ? 'emotionalLog.recordSingular' : 'emotionalLog.recordPlural';
  }

  getDayLabel(record: EmotionalRecord, index: number): string {
    const currentDate = this.getRecordDate(record);
    if (!currentDate) {
      return '';
    }

    const previousDate = index > 0 ? this.getRecordDate(this.records[index - 1]) : null;
    const isSamePreviousDay = previousDate &&
      previousDate.getFullYear() === currentDate.getFullYear() &&
      previousDate.getMonth() === currentDate.getMonth() &&
      previousDate.getDate() === currentDate.getDate();

    if (isSamePreviousDay) {
      return '';
    }

    const today = new Date();
    const isToday = currentDate.getFullYear() === today.getFullYear() &&
      currentDate.getMonth() === today.getMonth() &&
      currentDate.getDate() === today.getDate();

    return isToday ? 'emotionalLog.today' : currentDate.toLocaleDateString();
  }

  getDaypartKey(record: EmotionalRecord): string {
    const date = this.getRecordDate(record);
    if (!date) {
      return 'emotionalLog.dayparts.morning';
    }

    const hour = date.getHours();
    if (hour >= 5 && hour < 12) {
      return 'emotionalLog.dayparts.morning';
    }
    if (hour >= 12 && hour < 19) {
      return 'emotionalLog.dayparts.afternoon';
    }
    return 'emotionalLog.dayparts.night';
  }

  getAverage(field: 'moodLevel' | 'anxietyLevel' | 'stressLevel'): number {
    if (!this.records.length) {
      return 0;
    }

    const total = this.records.reduce((sum, record) => sum + Number(record[field] || 0), 0);
    return Math.round((total / this.records.length) * 10) / 10;
  }

  getRecordDate(record: EmotionalRecord): Date | null {
    const rawDate = record.createdDate || record.createdAt || record.recordDate || record.updatedAt;
    return rawDate ? new Date(rawDate) : null;
  }

  trackByRecord(index: number, record: EmotionalRecord): number | string {
    return record.id ?? `${record.moodLevel}-${record.createdDate || record.createdAt || record.recordDate || index}`;
  }

  private loadRecords(): void {
    this.loading = true;
    this.errorMsg = '';

    this.emotionalRecordService.getRecords(this.patientId).subscribe({
      next: records => {
        this.records = this.sortRecords(records);
        this.recordsDisplayed = 0;
        this.updateDisplayedRecords();
        this.loading = false;
      },
      error: error => {
        this.records = [];
        this.displayedRecords = [];
        this.recordsDisplayed = 0;
        this.loading = false;
        this.errorMsg = error?.error?.message || 'emotionalLog.errors.load';
      }
    });
  }

  loadMoreRecords(): void {
    this.recordsDisplayed += this.recordsPerPage;
    this.updateDisplayedRecords();
  }

  private updateDisplayedRecords(): void {
    const endIndex = Math.min(this.recordsDisplayed + this.recordsPerPage, this.records.length);
    this.displayedRecords = this.records.slice(0, endIndex);
  }

  private sortRecords(records: EmotionalRecord[]): EmotionalRecord[] {
    return [...records].sort((first, second) => {
      const firstDate = this.getRecordDate(first)?.getTime() || 0;
      const secondDate = this.getRecordDate(second)?.getTime() || 0;
      return secondDate - firstDate;
    });
  }

  private showToastMessage(message: string, type: 'success' | 'error'): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 3200);
  }

  private getEmotionSessionKey(): string {
    return `takecare-emotional-first-checkin-${this.patientId}`;
  }

  private checkIfRegisteredToday(): void {
    const sessionKey = this.getEmotionSessionKey();
    this.hasRegisteredToday = localStorage.getItem(sessionKey) === 'true';
    
    if (this.hasRegisteredToday && this.records.length > 0) {
      this.todayRegistrationTime = this.getTodayRegistrationTime();
    }
  }

  private getTodayRegistrationTime(): Date | null {
    const today = new Date();
    const todayRecords = this.records.filter(record => {
      const recordDate = this.getRecordDate(record);
      if (!recordDate) return false;
      return recordDate.getFullYear() === today.getFullYear() &&
             recordDate.getMonth() === today.getMonth() &&
             recordDate.getDate() === today.getDate();
    });
    
    if (todayRecords.length === 0) return null;
    return this.getRecordDate(todayRecords[0]);
  }

  hasMoreRecords(): boolean {
    return this.displayedRecords.length < this.records.length;
  }
}
// Trigger component reload for HTML inline styles.

