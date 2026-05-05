import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

export interface RatingEmoji {
  icon: string;
  label: string;
}

export interface RatingSubmitData {
  appointmentId: string;
  specialistId: number;
  score: number;
  comment: string;
  isAnonymous: boolean;
}

@Component({
  selector: 'app-rating-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './rating-modal.component.html',
  styleUrls: ['./rating-modal.component.css']
})
export class RatingModalComponent implements OnInit {
  @Input() appointmentId: string = '';
  @Input() specialistId: number = 0;
  @Input() specialistName: string = '';

  @Output() rated = new EventEmitter<RatingSubmitData>();
  @Output() closed = new EventEmitter<void>();

  isVisible = false;
  isSubmitting = false;
  alreadyRated = false;
  scoreError = false;

  selectedScore: number | null = null;
  comment = '';
  isAnonymous = false;

  emojis: RatingEmoji[] = [
    { icon: '😞', label: 'rating.emojis.terrible' },
    { icon: '😕', label: 'rating.emojis.bad' },
    { icon: '😐', label: 'rating.emojis.regular' },
    { icon: '😊', label: 'rating.emojis.good' },
    { icon: '😄', label: 'rating.emojis.excellent' }
  ];

  ngOnInit(): void {}

  open(appointmentId: string, specialistId: number, specialistName: string): void {
    this.appointmentId  = appointmentId;
    this.specialistId   = specialistId;
    this.specialistName = specialistName;

    this.checkAlreadyRated();
    this.isVisible = true;
    document.body.style.overflow = 'hidden'; 
  }

  close(): void {
    this.isVisible = false;
    document.body.style.overflow = '';
    this.resetForm();
    this.closed.emit();
  }

  onOverlayClick(): void {
    if (!this.isSubmitting) this.close();
  }

  checkAlreadyRated(): void {
    const rated = localStorage.getItem(`rated_${this.appointmentId}`);
    this.alreadyRated = !!rated;
  }

  selectScore(score: number): void {
    this.selectedScore = score;
    this.scoreError = false;
  }

  getScoreLabel(score: number): string {
    const labels: { [key: number]: string } = {
      1: 'rating.emojis.terrible',
      2: 'rating.emojis.bad',
      3: 'rating.emojis.regular',
      4: 'rating.emojis.good',
      5: 'rating.emojis.excellent'
    };
    return labels[score] || '';
  }

  submit(): void {
    if (!this.selectedScore) {
      this.scoreError = true;
      return;
    }

    this.isSubmitting = true;

    const data: RatingSubmitData = {
      appointmentId: this.appointmentId,
      specialistId:  this.specialistId,
      score:         this.selectedScore,
      comment:       this.comment.trim(),
      isAnonymous:   this.isAnonymous
    };

    setTimeout(() => {
      this.onSubmitSuccess(data);
    }, 1000);
  }

  private onSubmitSuccess(data: RatingSubmitData): void {
    localStorage.setItem(`rated_${this.appointmentId}`, 'true');

    this.rated.emit(data);
    this.isSubmitting = false;
    this.alreadyRated = true;
    setTimeout(() => this.close(), 2000);
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .slice(0, 2)
      .map(n => n[0])
      .join('')
      .toUpperCase();
  }

  private resetForm(): void {
    this.selectedScore = null;
    this.comment       = '';
    this.isAnonymous   = false;
    this.scoreError    = false;
    this.alreadyRated  = false;
    this.isSubmitting  = false;
  }
}
