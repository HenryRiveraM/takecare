import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ClinicalDocumentService {
  private readonly apiUrl = `${environment.apiUrl}/api/v1/patients/clinical-docs`;

  constructor(private http: HttpClient) {}

  getDocuments(patientId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${patientId}`).pipe(
      map(docs => docs.map(doc => ({
        id: doc.id,
        name: doc.fileName,    
        size: this.formatBytes(doc.fileSize),
        type: doc.contentType,
        uploadedAt: new Date(doc.uploadedAt),
        url: `${this.apiUrl}/${patientId}/${doc.id}` 
      })))
    );
  }

  uploadDocument(patientId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/${patientId}`, formData);
  }

  deleteDocument(patientId: number, documentId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${patientId}/${documentId}`);
  }

  private formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  downloadDocument(patientId: number, documentId: number): Observable<Blob> {
  return this.http.get(`${this.apiUrl}/${patientId}/${documentId}`, {
    responseType: 'blob'
  });
}
}