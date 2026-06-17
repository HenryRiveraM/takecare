import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthService } from '../../services/auth.service';
import {
  EmotionalMood,
  EmotionalRecord,
  EmotionalRecordRequest,
  EmotionalRecordService
} from '../../services/emotional-record.service';
import { SidebarService } from '../../services/sidebar.service';
import { SidebarComponent } from '../../shared/sidebar/sidebar.component';

interface MoodOption {
  value: EmotionalMood;
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

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  readonly moodOptions: MoodOption[] = [
    {
      value: 'VERY_GOOD',
      icon: 'sentiment_very_satisfied',
      labelKey: 'emotionalLog.moods.veryGood',
      helperKey: 'emotionalLog.moodHelpers.veryGood'
    },
    {
      value: 'GOOD',
      icon: 'sentiment_satisfied',
      labelKey: 'emotionalLog.moods.good',
      helperKey: 'emotionalLog.moodHelpers.good'
    },
    {
      value: 'NEUTRAL',
      icon: 'sentiment_neutral',
      labelKey: 'emotionalLog.moods.neutral',
      helperKey: 'emotionalLog.moodHelpers.neutral'
    },
    {
      value: 'SAD',
      icon: 'sentiment_dissatisfied',
      labelKey: 'emotionalLog.moods.sad',
      helperKey: 'emotionalLog.moodHelpers.sad'
    },
    {
      value: 'ANXIOUS',
      icon: 'psychology_alt',
      labelKey: 'emotionalLog.moods.anxious',
      helperKey: 'emotionalLog.moodHelpers.anxious'
    }
  ];

  emotionalForm = this.fb.nonNullable.group({
    mood: ['', Validators.required],
    energyLevel: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    anxietyLevel: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    sleepQuality: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
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

  selectMood(mood: EmotionalMood): void {
    this.emotionalForm.controls.mood.setValue(mood);
    this.emotionalForm.controls.mood.markAsTouched();
  }

  saveRecord(): void {
    if (!this.patientId) {
      this.showToastMessage('emotionalLog.errors.noPatient', 'error');
      return;
    }

    if (this.emotionalForm.invalid) {
      this.emotionalForm.markAllAsTouched();
      this.showToastMessage('emotionalLog.errors.formInvalid', 'error');
      return;
    }

    const value = this.emotionalForm.getRawValue();
    const payload: EmotionalRecordRequest = {
      mood: value.mood as EmotionalMood,
      energyLevel: Number(value.energyLevel),
      anxietyLevel: Number(value.anxietyLevel),
      sleepQuality: Number(value.sleepQuality),
      notes: value.notes.trim() || undefined
    };

    this.saving = true;

    this.emotionalRecordService.createRecord(this.patientId, payload).subscribe({
      next: saved => {
        this.records = this.sortRecords([saved, ...this.records]);
        this.emotionalForm.reset({
          mood: '',
          energyLevel: 3,
          anxietyLevel: 3,
          sleepQuality: 3,
          notes: ''
        });
        this.saving = false;
        this.showToastMessage('emotionalLog.toast.saved', 'success');
      },
      error: error => {
        this.saving = false;
        this.showToastMessage(error?.error?.message || 'emotionalLog.errors.save', 'error');
      }
    });
  }

  getMoodLabel(mood: string | undefined): string {
    return this.moodOptions.find(option => option.value === mood)?.labelKey || 'emotionalLog.moods.unknown';
  }

  getMoodIcon(mood: string | undefined): string {
    return this.moodOptions.find(option => option.value === mood)?.icon || 'sentiment_neutral';
  }

  getAverage(field: 'energyLevel' | 'anxietyLevel' | 'sleepQuality'): number {
    if (!this.records.length) {
      return 0;
    }

    const total = this.records.reduce((sum, record) => sum + Number(record[field] || 0), 0);
    return Math.round((total / this.records.length) * 10) / 10;
  }

  getRecordDate(record: EmotionalRecord): Date | null {
    const rawDate = record.createdAt || record.recordDate || record.updatedAt;
    return rawDate ? new Date(rawDate) : null;
  }

  trackByRecord(index: number, record: EmotionalRecord): number | string {
    return record.id ?? `${record.mood}-${record.createdAt || record.recordDate || index}`;
  }

  private loadRecords(): void {
    this.loading = true;
    this.errorMsg = '';

    this.emotionalRecordService.getRecords(this.patientId).subscribe({
      next: records => {
        this.records = this.sortRecords(records);
        this.loading = false;
      },
      error: error => {
        this.records = [];
        this.loading = false;
        this.errorMsg = error?.error?.message || 'emotionalLog.errors.load';
      }
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
