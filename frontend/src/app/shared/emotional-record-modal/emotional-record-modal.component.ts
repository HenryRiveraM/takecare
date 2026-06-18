import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import {
  EmotionalRecord,
  EmotionalRecordRequest,
  EmotionalRecordService
} from '../../services/emotional-record.service';

interface MoodOption {
  value: number;
  icon: string;
  labelKey: string;
  helperKey: string;
}

@Component({
  selector: 'app-emotional-record-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './emotional-record-modal.component.html',
  styleUrls: ['./emotional-record-modal.component.css']
})
export class EmotionalRecordModalComponent {
  private readonly fb = inject(FormBuilder);

  @Input({ required: true }) patientId = 0;
  @Input() required = false;
  @Output() saved = new EventEmitter<EmotionalRecord>();
  @Output() cancelled = new EventEmitter<void>();
  @Output() failed = new EventEmitter<string>();

  saving = false;
  errorKey = '';

  readonly moodOptions: MoodOption[] = [
    { value: 1, icon: 'sentiment_very_dissatisfied', labelKey: 'emotionalLog.moods.veryLow', helperKey: 'emotionalLog.moodHelpers.veryLow' },
    { value: 2, icon: 'sentiment_dissatisfied', labelKey: 'emotionalLog.moods.low', helperKey: 'emotionalLog.moodHelpers.low' },
    { value: 3, icon: 'sentiment_neutral', labelKey: 'emotionalLog.moods.neutral', helperKey: 'emotionalLog.moodHelpers.neutral' },
    { value: 4, icon: 'sentiment_satisfied', labelKey: 'emotionalLog.moods.good', helperKey: 'emotionalLog.moodHelpers.good' },
    { value: 5, icon: 'sentiment_very_satisfied', labelKey: 'emotionalLog.moods.excellent', helperKey: 'emotionalLog.moodHelpers.excellent' }
  ];

  emotionalForm = this.fb.nonNullable.group({
    moodLevel: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    anxietyLevel: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    stressLevel: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    notes: ['', [Validators.maxLength(280)]]
  });

  constructor(private emotionalRecordService: EmotionalRecordService) {}

  selectMood(moodLevel: number): void {
    this.errorKey = '';
    this.emotionalForm.controls.moodLevel.setValue(moodLevel);
    this.emotionalForm.controls.moodLevel.markAsTouched();
  }

  close(): void {
    if (!this.required && !this.saving) {
      this.cancelled.emit();
    }
  }

  stopClose(event: Event): void {
    event.stopPropagation();
  }

  saveRecord(): void {
    if (!this.patientId) {
      this.errorKey = 'emotionalLog.errors.noPatient';
      this.failed.emit(this.errorKey);
      return;
    }

    if (this.emotionalForm.invalid) {
      this.emotionalForm.markAllAsTouched();
      this.errorKey = 'emotionalLog.errors.moodRequired';
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
    this.errorKey = '';

    this.emotionalRecordService.createRecord(this.patientId, payload).subscribe({
      next: saved => {
        this.saving = false;
        this.emotionalForm.reset({ moodLevel: 0, anxietyLevel: 3, stressLevel: 3, notes: '' });
        this.saved.emit(saved);
      },
      error: error => {
        this.saving = false;
        this.errorKey = error?.error?.message || 'emotionalLog.errors.save';
        this.failed.emit(this.errorKey);
      }
    });
  }
}
