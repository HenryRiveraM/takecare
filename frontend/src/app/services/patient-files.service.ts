import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ClinicalDocumentResponse {
  id: number;
  patientId: number;
  fileName: string;
  contentType: string;
  fileSize: number;
  uploadedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class PatientFilesService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      return new HttpHeaders();
    }
    const user = JSON.parse(storedUser);
    const userId = user?.id ? String(user.id) : '';
    let roleStr = 'PATIENT';
    if (user?.role === 2 || String(user?.role).toUpperCase() === 'SPECIALIST') {
      roleStr = 'SPECIALIST';
    } else if (user?.role === 3 || String(user?.role).toUpperCase() === 'ADMIN') {
      roleStr = 'ADMIN';
    }
    return new HttpHeaders({
      'X-User-Id': userId,
      'X-User-Role': roleStr
    });
  }

  getDocuments(patientId: number): Observable<ClinicalDocumentResponse[]> {
    return this.http.get<ClinicalDocumentResponse[]>(
      `${this.baseUrl}/api/v1/patients/clinical-docs/${patientId}`,
      { headers: this.getHeaders() }
    );
  }

  uploadDocument(patientId: number, file: File): Observable<ClinicalDocumentResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ClinicalDocumentResponse>(
      `${this.baseUrl}/api/v1/patients/clinical-docs/${patientId}`,
      formData,
      { headers: this.getHeaders() }
    );
  }

  downloadDocument(patientId: number, documentId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(
      `${this.baseUrl}/api/v1/patients/clinical-docs/${patientId}/${documentId}`,
      {
        headers: this.getHeaders(),
        responseType: 'blob',
        observe: 'response'
      }
    );
  }

  deleteDocument(patientId: number, documentId: number): Observable<any> {
    return this.http.delete(
      `${this.baseUrl}/api/v1/patients/clinical-docs/${patientId}/${documentId}`,
      {
        headers: this.getHeaders(),
        responseType: 'text'
      }
    );
  }
}
