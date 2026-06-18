import { Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { LogbookAuthorRole, LogbookNote, LogbookService } from '../../services/logbook.service';

@Component({
  selector: 'app-care-plan-logbook',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './care-plan-logbook.component.html',
  styleUrls: ['./care-plan-logbook.component.css']
})
export class CarePlanLogbookComponent implements OnChanges {

  @Input() planId!: number;
  @Input() currentUserId = 0;
  @Input() currentUserRole: LogbookAuthorRole = 'PATIENT';
  @Input() currentUserName = '';

  @ViewChild('threadContainer') private threadContainer!: ElementRef;

  loaded = false;
  loading = false;
  errorMsg = '';
  notes: LogbookNote[] = [];

  newNoteContent = '';
  saving = false;
  formError = '';

  readonly minLength = 5;
  readonly maxLength = 600;

  constructor(
    private logbookService: LogbookService,
    private translate: TranslateService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['planId']) {
      this.loaded = false;
      this.notes = [];
      if (this.planId) {
        this.loadNotes();
      }
    }
  }

  isMine(note: LogbookNote): boolean {
    return note.authorRole === this.currentUserRole && note.authorId === this.currentUserId;
  }

  getRoleLabel(role: LogbookAuthorRole): string {
    return role === 'SPECIALIST' ? 'carePlans.logbook.roleSpecialist' : 'carePlans.logbook.rolePatient';
  }

  getPlaceholder(): string {
    return this.currentUserRole === 'SPECIALIST'
      ? this.translate.instant('carePlans.logbook.placeholderSpecialist')
      : this.translate.instant('carePlans.logbook.placeholderPatient');
  }

  addNote(): void {
    this.formError = '';
    const content = this.newNoteContent.trim();

    if (content.length < this.minLength) {
      this.formError = 'carePlans.logbook.validation.tooShort';
      return;
    }

    this.saving = true;

    this.logbookService.addNote(this.planId, {
      authorId: this.currentUserId,
      authorRole: this.currentUserRole,
      authorName: this.currentUserName,
      content
    }).subscribe({
      next: note => {
        this.notes = [...this.notes, note];
        this.newNoteContent = '';
        this.saving = false;
        this.scrollToBottom();
      },
      error: () => {
        this.formError = 'carePlans.logbook.validation.addError';
        this.saving = false;
      }
    });
  }

  private loadNotes(): void {
    this.loading = true;
    this.errorMsg = '';

    this.logbookService.getNotesByPlan(this.planId).subscribe({
      next: notes => {
        this.notes = notes;
        this.loaded = true;
        this.loading = false;
        this.scrollToBottom();
      },
      error: () => {
        this.errorMsg = 'carePlans.logbook.loadError';
        this.loading = false;
      }
    });
  }

  private scrollToBottom(): void {
    try {
      setTimeout(() => {
        if (this.threadContainer) {
          this.threadContainer.nativeElement.scrollTop = this.threadContainer.nativeElement.scrollHeight;
        }
      }, 50);
    } catch (err) {}
  }
}