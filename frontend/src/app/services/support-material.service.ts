import { Injectable } from '@angular/core';

export interface SupportMaterialItem {
  id: string;
  specialistId: number;
  title: string;
  description: string;
  fileUrl: string;
  createdDate: string;
  updatedDate: string;
}

@Injectable({
  providedIn: 'root'
})
export class SupportMaterialService {
  private readonly storageKey = 'support_materials_frontend';

  getBySpecialistId(specialistId: number): SupportMaterialItem[] {
    return this.readAll()
      .filter((material) => material.specialistId === specialistId)
      .sort((a, b) => new Date(b.updatedDate).getTime() - new Date(a.updatedDate).getTime());
  }

  save(
    specialistId: number,
    material: Omit<SupportMaterialItem, 'id' | 'specialistId' | 'createdDate' | 'updatedDate'>,
    materialId?: string
  ): SupportMaterialItem {
    const materials = this.readAll();
    const now = new Date().toISOString();
    const existingIndex = materialId
      ? materials.findIndex((item) => item.id === materialId && item.specialistId === specialistId)
      : -1;

    const nextMaterial: SupportMaterialItem = existingIndex >= 0
      ? {
          ...materials[existingIndex],
          ...material,
          updatedDate: now
        }
      : {
          id: crypto.randomUUID(),
          specialistId,
          ...material,
          createdDate: now,
          updatedDate: now
        };

    if (existingIndex >= 0) {
      materials[existingIndex] = nextMaterial;
    } else {
      materials.push(nextMaterial);
    }

    localStorage.setItem(this.storageKey, JSON.stringify(materials));
    return nextMaterial;
  }

  delete(specialistId: number, materialId: string): void {
    const nextMaterials = this.readAll().filter(
      (item) => !(item.specialistId === specialistId && item.id === materialId)
    );

    localStorage.setItem(this.storageKey, JSON.stringify(nextMaterials));
  }

  private readAll(): SupportMaterialItem[] {
    const raw = localStorage.getItem(this.storageKey);

    if (!raw) {
      return [];
    }

    try {
      const parsed = JSON.parse(raw) as SupportMaterialItem[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}
