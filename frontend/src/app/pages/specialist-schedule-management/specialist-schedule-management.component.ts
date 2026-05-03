import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DayPilot, DayPilotModule } from '@daypilot/daypilot-lite-angular';

import { SpecialistScheduleService } from '../../services/specialist-schedule.service';
import {
  SpecialistScheduleRequest,
  SpecialistScheduleResponse
} from '../../models/specialist-schedule.model';

@Component({
  selector: 'app-specialist-schedule-management',
  standalone: true,
  imports: [CommonModule, FormsModule, DayPilotModule],
  templateUrl: './specialist-schedule-management.component.html',
  styleUrls: ['./specialist-schedule-management.component.css']
})
export class SpecialistScheduleManagementComponent implements OnInit {

  specialistId!: number;

  schedules: SpecialistScheduleResponse[] = [];
  events: DayPilot.EventData[] = [];

  selectedScheduleId: number | null = null;

  currentRangeStart = '';
  currentRangeEnd = '';

  form: SpecialistScheduleRequest = {
    scheduleDate: '',
    dayOfWeek: 1,
    startTime: '09:00',
    endTime: '10:00'
  };

  days = [
    { value: 1, label: 'Lunes' },
    { value: 2, label: 'Martes' },
    { value: 3, label: 'Miércoles' },
    { value: 4, label: 'Jueves' },
    { value: 5, label: 'Viernes' },
    { value: 6, label: 'Sábado' },
    { value: 7, label: 'Domingo' }
  ];

  config: DayPilot.CalendarConfig = {
    viewType: 'Week',
    startDate: '',
    businessBeginsHour: 6,
    businessEndsHour: 22,
    cellDuration: 30,
    timeRangeSelectedHandling: 'Enabled',
    eventMoveHandling: 'Disabled',
    eventResizeHandling: 'Disabled',
    eventClickHandling: 'Enabled',

    onTimeRangeSelected: async (args) => {
      const startAsString = args.start.toString();
      const endAsString = args.end.toString();

      const scheduleDate = this.extractDate(startAsString);
      const dayOfWeek = this.convertDateToDayOfWeek(scheduleDate);
      const startTime = this.extractTime(startAsString);
      const endTime = this.extractTime(endAsString);

      this.form = {
        scheduleDate,
        dayOfWeek,
        startTime,
        endTime
      };

      this.selectedScheduleId = null;
      args.control.clearSelection();
    },

    onEventClick: async (args) => {
      const scheduleId = Number(args.e.id());
      const schedule = this.schedules.find(item => item.id === scheduleId);

      if (!schedule) {
        return;
      }

      this.selectSchedule(schedule);
    }
  };

  constructor(private scheduleService: SpecialistScheduleService) {}

  ngOnInit(): void {
    this.initializeSpecialistId();
    this.initializeCurrentSevenDaysRange();
    this.loadSchedules();
  }

  private initializeSpecialistId(): void {
    const userData = localStorage.getItem('user');

    if (!userData) {
      throw new Error('No se encontró el usuario logueado en localStorage.');
    }

    const user = JSON.parse(userData);
    this.specialistId = Number(user.id);

    console.log('Specialist ID usado en Agenda:', this.specialistId);
  }

  private initializeCurrentSevenDaysRange(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const endDate = new Date(today);
    endDate.setDate(today.getDate() + 6);

    this.currentRangeStart = this.formatDate(today);
    this.currentRangeEnd = this.formatDate(endDate);

    this.config = {
      ...this.config,
      startDate: this.currentRangeStart
    };

    this.resetForm();

    console.log('Rango visible:', this.currentRangeStart, 'a', this.currentRangeEnd);
  }

  loadSchedules(): void {
    this.scheduleService.getSchedulesByDateRange(
      this.specialistId,
      this.currentRangeStart,
      this.currentRangeEnd
    ).subscribe({
      next: (response) => {
        this.schedules = response;
        this.events = this.mapSchedulesToDayPilotEvents(response);
      },
      error: (error) => {
        console.error('Error loading schedules:', error);
        alert('No se pudieron cargar los horarios.');
      }
    });
  }

  saveSchedule(): void {
    if (!this.isValidForm()) {
      alert('Verifica que la fecha y las horas sean correctas.');
      return;
    }

    if (this.selectedScheduleId) {
      this.updateSchedule();
    } else {
      this.createSchedule();
    }
  }

  createSchedule(): void {
    this.scheduleService.createSchedule(this.specialistId, this.form).subscribe({
      next: () => {
        this.resetForm();
        this.loadSchedules();
      },
      error: (error) => {
        console.error('Error creating schedule:', error);
        alert(error?.error?.message || 'No se pudo crear el horario.');
      }
    });
  }

  updateSchedule(): void {
    if (!this.selectedScheduleId) {
      return;
    }

    const selectedSchedule = this.getSelectedSchedule();

    if (!selectedSchedule || !this.canModifySchedule(selectedSchedule)) {
      alert('Solo puedes editar horarios disponibles que todavía no han empezado.');
      return;
    }

    this.scheduleService.updateSchedule(this.selectedScheduleId, this.form).subscribe({
      next: () => {
        this.resetForm();
        this.loadSchedules();
      },
      error: (error) => {
        console.error('Error updating schedule:', error);
        alert(error?.error?.message || 'No se pudo actualizar el horario.');
      }
    });
  }

  deleteSelectedSchedule(): void {
    if (!this.selectedScheduleId) {
      alert('Selecciona un horario para eliminar.');
      return;
    }

    const selectedSchedule = this.getSelectedSchedule();

    if (!selectedSchedule || !this.canModifySchedule(selectedSchedule)) {
      alert('Solo puedes eliminar horarios disponibles que todavía no han empezado.');
      return;
    }

    const confirmed = confirm('¿Seguro que deseas eliminar este horario?');

    if (!confirmed) {
      return;
    }

    this.scheduleService.deleteSchedule(this.selectedScheduleId).subscribe({
      next: () => {
        this.resetForm();
        this.loadSchedules();
      },
      error: (error) => {
        console.error('Error deleting schedule:', error);
        alert(error?.error?.message || 'No se pudo eliminar el horario.');
      }
    });
  }

  selectSchedule(schedule: SpecialistScheduleResponse): void {
    this.selectedScheduleId = schedule.id;

    this.form = {
      scheduleDate: schedule.scheduleDate,
      dayOfWeek: schedule.dayOfWeek,
      startTime: this.normalizeTime(schedule.startTime),
      endTime: this.normalizeTime(schedule.endTime)
    };
  }

  resetForm(): void {
    this.selectedScheduleId = null;

    this.form = {
      scheduleDate: this.currentRangeStart,
      dayOfWeek: this.convertDateToDayOfWeek(this.currentRangeStart),
      startTime: '09:00',
      endTime: '10:00'
    };
  }

  onScheduleDateChange(date: string): void {
    this.form.scheduleDate = date;
    this.form.dayOfWeek = this.convertDateToDayOfWeek(date);
  }

  goToNextRange(): void {
    this.changeRange(7);
  }

  goToPreviousRange(): void {
    this.changeRange(-7);
  }

  private changeRange(days: number): void {
    const start = new Date(`${this.currentRangeStart}T00:00:00`);
    start.setDate(start.getDate() + days);

    const end = new Date(start);
    end.setDate(start.getDate() + 6);

    this.currentRangeStart = this.formatDate(start);
    this.currentRangeEnd = this.formatDate(end);

    this.config = {
      ...this.config,
      startDate: this.currentRangeStart
    };

    this.resetForm();
    this.loadSchedules();
  }

  private normalizeTimeWithSeconds(time: string): string {
    if (!time) {
      return '00:00:00';
    }

    const cleanTime = time.substring(0, 8);

    if (cleanTime.length === 5) {
      return `${cleanTime}:00`;
    }

    return cleanTime;
  }

  private buildDayPilotDateTime(date: string, time: string): string {
    const normalizedTime = this.normalizeTimeWithSeconds(time);
    return `${date}T${normalizedTime}`;
  }

  private mapSchedulesToDayPilotEvents(
    schedules: SpecialistScheduleResponse[]
  ): DayPilot.EventData[] {
    return schedules.map(schedule => {
      return {
        id: schedule.id,
        text: this.buildEventText(schedule),
        start: this.buildDayPilotDateTime(schedule.scheduleDate, schedule.startTime),
        end: this.buildDayPilotDateTime(schedule.scheduleDate, schedule.endTime),
        backColor: schedule.status === 0 ? '#d9f99d' : '#fecaca',
        borderColor: schedule.status === 0 ? '#65a30d' : '#dc2626'
      };
    });
  }

  private buildEventText(schedule: SpecialistScheduleResponse): string {
    const statusText = schedule.status === 0 ? 'Disponible' : 'Reservado';
    return `${statusText} | ${this.normalizeTime(schedule.startTime)} - ${this.normalizeTime(schedule.endTime)}`;
  }

  private getSelectedSchedule(): SpecialistScheduleResponse | undefined {
    if (!this.selectedScheduleId) {
      return undefined;
    }

    return this.schedules.find(schedule => schedule.id === this.selectedScheduleId);
  }

  canModifySchedule(schedule: SpecialistScheduleResponse): boolean {
    if (schedule.status !== 0) {
      return false;
    }

    const scheduleStart = new Date(
      `${schedule.scheduleDate}T${this.normalizeTime(schedule.startTime)}:00`
    );

    return scheduleStart.getTime() > new Date().getTime();
  }

  isSelectedScheduleEditable(): boolean {
    const selectedSchedule = this.getSelectedSchedule();

    if (!selectedSchedule) {
      return false;
    }

    return this.canModifySchedule(selectedSchedule);
  }

  private isValidForm(): boolean {
    if (!this.form.scheduleDate || !this.form.dayOfWeek || !this.form.startTime || !this.form.endTime) {
      return false;
    }

    if (this.form.startTime >= this.form.endTime) {
      return false;
    }

    const scheduleStart = new Date(`${this.form.scheduleDate}T${this.form.startTime}:00`);

    return scheduleStart.getTime() > new Date().getTime();
  }

  private extractDate(dateTimeString: string): string {
    return dateTimeString.substring(0, 10);
  }

  private extractTime(dateTimeString: string): string {
    if (!dateTimeString.includes('T')) {
      return '00:00';
    }

    return dateTimeString.substring(11, 16);
  }

  private convertDateToDayOfWeek(dateString: string): number {
    const date = new Date(`${dateString}T00:00:00`);
    const jsDay = date.getDay();

    return jsDay === 0 ? 7 : jsDay;
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  normalizeTime(time: string): string {
    return time ? time.substring(0, 5) : '';
  }

  getDayLabel(dayOfWeek: number): string {
    return this.days.find(day => day.value === dayOfWeek)?.label || '';
  }
}