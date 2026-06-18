import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import {
  EmotionalRecord,
  EmotionalRecordRequest,
  EmotionalRecordService
} from '../../services/emotional-record.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';

interface MoodOption {
  value: number;
  icon: string;
  labelKey: string;
  helperKey: string;
}

@Component({
  selector: 'app-patient-emotional-records',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe, SidebarComponent],
  templateUrl: './patient-emotional-records.component.html',
  styleUrls: ['./patient-emotional-records.component.css']
})
export class PatientEmotionalRecordsComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);

  patientId = 0;
  records: EmotionalRecord[] = [];
  loading = false;
  saving = false;
  errorMsg = '';
  hasRecordedToday = false;
  today = new Date();

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  readonly moodOptions: MoodOption[] = [
    {
      value: 1,
      icon: 'sentiment_very_dissatisfied',
      labelKey: 'emotionalLog.moods.veryLow',
      helperKey: 'emotionalLog.moodHelpers.veryLow'
    },
    {
      value: 2,
      icon: 'sentiment_dissatisfied',
      labelKey: 'emotionalLog.moods.low',
      helperKey: 'emotionalLog.moodHelpers.low'
    },
    {
      value: 3,
      icon: 'sentiment_neutral',
      labelKey: 'emotionalLog.moods.neutral',
      helperKey: 'emotionalLog.moodHelpers.neutral'
    },
    {
      value: 4,
      icon: 'sentiment_satisfied',
      labelKey: 'emotionalLog.moods.good',
      helperKey: 'emotionalLog.moodHelpers.good'
    },
    {
      value: 5,
      icon: 'sentiment_very_satisfied',
      labelKey: 'emotionalLog.moods.excellent',
      helperKey: 'emotionalLog.moodHelpers.excellent'
    }
  ];

  emotionalForm = this.fb.nonNullable.group({
    moodLevel: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    anxietyLevel: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    stressLevel: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    notes: ['', [Validators.maxLength(280)]]
  });

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
  }

  ngOnDestroy(): void {
    document.body.classList.remove('dashboard-active');
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  selectMood(moodLevel: number): void {
    this.emotionalForm.controls.moodLevel.setValue(moodLevel);
    this.emotionalForm.controls.moodLevel.markAsTouched();
  }

  saveRecord(): void {
    if (!this.patientId) {
      this.showToastMessage('emotionalLog.errors.noPatient', 'error');
      return;
    }

    if (this.hasRecordedToday) {
      this.showToastMessage('emotionalLog.errors.alreadyToday', 'error');
      return;
    }

    if (this.emotionalForm.invalid) {
      this.emotionalForm.markAllAsTouched();
      this.showToastMessage('emotionalLog.errors.formInvalid', 'error');
      return;
    }

    const value = this.emotionalForm.getRawValue();
    const payload: EmotionalRecordRequest = {
      moodLevel: Number(value.moodLevel),
      anxietyLevel: Number(value.anxietyLevel),
      stressLevel: Number(value.stressLevel),
      notes: value.notes.trim() || undefined
    };

    this.saving = true;

    this.emotionalRecordService.createRecord(this.patientId, payload).subscribe({
      next: saved => {
        this.records = this.sortRecords([saved, ...this.records]);
        this.hasRecordedToday = true;
        this.emotionalForm.reset({
          moodLevel: 0,
          anxietyLevel: 3,
          stressLevel: 3,
          notes: ''
        });
        this.saving = false;
        this.showToastMessage('emotionalLog.toast.saved', 'success');
      },
      error: error => {
        this.saving = false;
        const msg = error?.error?.message || '';
        // 409 = already recorded today (backend enforcement)
        if (error?.status === 409) {
          this.hasRecordedToday = true;
          this.showToastMessage('emotionalLog.errors.alreadyToday', 'error');
        } else {
          this.showToastMessage(msg || 'emotionalLog.errors.save', 'error');
        }
      }
    });
  }

  getMoodLabel(moodLevel: number | undefined): string {
    return this.moodOptions.find(option => option.value === Number(moodLevel || 0))?.labelKey || 'emotionalLog.moods.unknown';
  }

  getMoodIcon(moodLevel: number | undefined): string {
    return this.moodOptions.find(option => option.value === Number(moodLevel || 0))?.icon || 'sentiment_neutral';
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
        this.hasRecordedToday = this.checkRecordedToday(this.records);
        this.loading = false;
      },
      error: error => {
        this.records = [];
        this.loading = false;
        this.errorMsg = error?.error?.message || 'emotionalLog.errors.load';
      }
    });
  }

  private checkRecordedToday(records: EmotionalRecord[]): boolean {
    const today = new Date();
    return records.some(r => {
      const d = this.getRecordDate(r);
      return d &&
        d.getFullYear() === today.getFullYear() &&
        d.getMonth()   === today.getMonth() &&
        d.getDate()    === today.getDate();
    });
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
}
// Trigger component reload for HTML inline styles.

