import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface SupportMaterialItem {
  id: number;
  title: string;
  description: string;
  fileUrl: string;
  fileType: string;
  specialistId: number;
  specialistName: string;
  createdDate: string;
}

const MOCK_SUPPORT_MATERIALS: SupportMaterialItem[] = [
  {
    id: 1,
    title: 'Guía práctica para manejar la ansiedad diaria',
    description: 'Documento breve con ejercicios de respiración, técnicas de aterrizaje y hábitos que ayudan a reducir síntomas de ansiedad.',
    fileUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
    fileType: 'PDF',
    specialistId: 101,
    specialistName: 'Ana Torres Lima',
    createdDate: '2026-05-01T09:15:00'
  },
  {
    id: 2,
    title: 'Rutina básica de autocuidado emocional',
    description: 'Material introductorio para construir una rutina semanal de descanso, registro emocional y organización personal.',
    fileUrl: 'https://file-examples.com/wp-content/storage/2017/02/file-sample_100kB.docx',
    fileType: 'DOCX',
    specialistId: 102,
    specialistName: 'Carlos Mendoza Rios',
    createdDate: '2026-04-28T14:30:00'
  },
  {
    id: 3,
    title: 'Alimentación y bienestar mental',
    description: 'Recurso orientado a la comunidad con recomendaciones simples sobre alimentación, energía y concentración.',
    fileUrl: 'https://www.africau.edu/images/default/sample.pdf',
    fileType: 'PDF',
    specialistId: 103,
    specialistName: 'Luis Vargas Paz',
    createdDate: '2026-04-22T11:00:00'
  }
];

@Injectable({
  providedIn: 'root'
})
export class SupportMaterialService {
  // Backend verified on 2026-05-03:
  // supportmaterial currently exposes entity + repository only, with no public API.
  // This service is isolated so the data source can be swapped to HttpClient later.
  getCommunityMaterials(): Observable<SupportMaterialItem[]> {
    return of(
      [...MOCK_SUPPORT_MATERIALS].sort((a, b) =>
        new Date(b.createdDate).getTime() - new Date(a.createdDate).getTime()
      )
    );
  }

  getMaterialsBySpecialist(specialistId: number, specialistName?: string): Observable<SupportMaterialItem[]> {
    return new Observable((subscriber) => {
      this.getCommunityMaterials().subscribe({
        next: (materials) => {
          const directMatches = materials.filter((material) => material.specialistId === specialistId);

          if (directMatches.length > 0) {
            subscriber.next(directMatches);
            subscriber.complete();
            return;
          }

          if (specialistName?.trim()) {
            const normalizedName = this.normalizeText(specialistName);
            const nameMatches = materials.filter(
              (material) => this.normalizeText(material.specialistName) === normalizedName
            );

            subscriber.next(nameMatches);
            subscriber.complete();
            return;
          }

          subscriber.next([]);
          subscriber.complete();
        },
        error: (error) => subscriber.error(error)
      });
    });
  }

  private normalizeText(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
