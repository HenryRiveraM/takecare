import { Injectable } from '@angular/core';

export type LocationVisibility = 'public' | 'private';

export interface SpecialistLocationPreferences {
  addressLine: string;
  city: string;
  neighborhood: string;
  reference: string;
  visibility: LocationVisibility;
}

@Injectable({
  providedIn: 'root'
})
export class SpecialistLocationPreferencesService {
  private readonly storagePrefix = 'specialist-location-preferences';

  getByUserId(userId: number): SpecialistLocationPreferences | null {
    const savedData = localStorage.getItem(this.buildKey(userId));
    return savedData ? JSON.parse(savedData) as SpecialistLocationPreferences : null;
  }

  saveByUserId(userId: number, data: SpecialistLocationPreferences): void {
    localStorage.setItem(this.buildKey(userId), JSON.stringify(data));
  }

  clearByUserId(userId: number): void {
    localStorage.removeItem(this.buildKey(userId));
  }

  private buildKey(userId: number): string {
    return `${this.storagePrefix}:${userId}`;
  }
}
