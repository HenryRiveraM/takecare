import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SupportMaterialItem {
  id: number;
  title: string;
  description: string;
  fileName?: string;
  contentType?: string;
  fileUrl: string;
  previewUrl?: string | null;
  downloadUrl?: string | null;
  fileType: string;
  fileSize?: number;
  specialistId: number;
  specialistName: string;
  createdDate: string;
}

export interface SupportMaterialListResponse {
  totalDocuments: number;
  materials: SupportMaterialItem[];
}

@Injectable({
  providedIn: 'root'
})
export class SupportMaterialService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getCommunityMaterials(search?: string, type?: string): Observable<SupportMaterialListResponse> {
    let params = new HttpParams();

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    if (type?.trim()) {
      params = params.set('type', type.trim());
    }

    return this.http.get<SupportMaterialListResponse>(
      `${this.baseUrl}/api/v1/support-materials`,
      { params }
    );
  }

  getMaterialsBySpecialist(specialistId: number): Observable<SupportMaterialListResponse> {
    return this.http.get<SupportMaterialListResponse>(
      `${this.baseUrl}/api/v1/specialists/${specialistId}/support-materials`
    );
  }
}
