import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { CarePlan, CarePlanItem } from '../../services/care-plan.service';
import { EmotionalRecordService, EmotionalSummary } from '../../services/emotional-record.service';

interface TaskSummary {
  completed: number;
  overdue: number;
  pending: number;
  total: number;
}

type EmotionLevel = 'good' | 'warning' | 'critical';

@Component({
  selector: 'app-care-plan-progress-dashboard',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './care-plan-progress-dashboard.component.html',
  styleUrls: ['./care-plan-progress-dashboard.component.css']
})
export class CarePlanProgressDashboardComponent implements OnChanges {

  @Input() plan: CarePlan | null = null;
  @Input() patientId = 0;

  loadingEmotional = false;
  emotionalSummary: EmotionalSummary | null = null;
  emotionalErrorMsg = '';

  readonly emotionalScaleMax = 10;

  constructor(private emotionalRecordService: EmotionalRecordService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['patientId'] && this.patientId) {
      this.loadEmotionalSummary();
    }
  }

  get taskSummary(): TaskSummary {
    const items: CarePlanItem[] = this.plan?.items || [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    let completed = 0;
    let overdue = 0;
    let pending = 0;

    items.forEach(item => {
      if (item.status === 'COMPLETED') {
        completed++;
        return;
      }

      if (item.status === 'CANCELLED') {
        return;
      }

      if (item.dueDate) {
        const due = new Date(`${item.dueDate}T00:00:00`);
        if (!Number.isNaN(due.getTime()) && due < today) {
          overdue++;
          return;
        }
      }

      pending++;
    });

    return { completed, overdue, pending, total: completed + overdue + pending };
  }

  taskPercentage(value: number): number {
    const total = this.taskSummary.total;
    if (!total) {
      return 0;
    }
    return Math.round((value / total) * 100);
  }

  getRiskLevel(value: number | undefined): EmotionLevel {
    const ratio = (value ?? 0) / this.emotionalScaleMax;
    if (ratio <= 0.34) return 'good';
    if (ratio <= 0.67) return 'warning';
    return 'critical';
  }

  getMoodLevel(value: number | undefined): EmotionLevel {
    const ratio = (value ?? 0) / this.emotionalScaleMax;
    if (ratio <= 0.34) return 'critical';
    if (ratio <= 0.67) return 'warning';
    return 'good';
  }

  private loadEmotionalSummary(): void {
    this.loadingEmotional = true;
    this.emotionalErrorMsg = '';

    this.emotionalRecordService.getEmotionalSummary(this.patientId).subscribe({
      next: summary => {
        this.emotionalSummary = summary;
        this.loadingEmotional = false;
      },
      error: () => {
        this.emotionalErrorMsg = 'carePlans.dashboard.emotionalError';
        this.loadingEmotional = false;
      }
    });
  }
}