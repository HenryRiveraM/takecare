import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
 
export interface Resource {
  id: string;
  title: string;
  description: string;
  fileName: string;
  fileUrl: string;
  uploadedAt: Date;
}
 
@Injectable({
  providedIn: 'root'
})
export class SpecialistResourcesService {
 
  private readonly apiUrl = environment.apiUrl;
 
  constructor(private http: HttpClient) {}
 
  getResources(specialistId: number): Observable<Resource[]> {
    return this.http.get<Resource[]>(
      `${this.apiUrl}/api/v1/specialists/${specialistId}/orientation-materials`
    );
  }
 
  uploadResource(specialistId: number, title: string, description: string, file: File): Observable<Resource> {
    const formData = new FormData();
    formData.append('title', title);
    formData.append('description', description);
    formData.append('file', file);
    return this.http.post<Resource>(
      `${this.apiUrl}/api/v1/specialists/${specialistId}/orientation-materials`,
      formData
    );
  }
 
  deleteResource(specialistId: number, resourceId: string): Observable<string> {
    return this.http.delete(
      `${this.apiUrl}/api/v1/specialists/${specialistId}/orientation-materials/${resourceId}`,
      { responseType: 'text' }
    );
  }
}