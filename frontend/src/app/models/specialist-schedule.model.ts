export interface SpecialistScheduleRequest {
  scheduleDate: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

export interface SpecialistScheduleResponse {
  id: number;
  scheduleDate: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  status: number;
}

export interface SpecialistScheduleGroup {
  dayOfWeek: number;
  schedules: SpecialistScheduleResponse[];
}