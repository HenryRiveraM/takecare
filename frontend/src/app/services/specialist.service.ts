import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SpecialistService {

  private readonly baseUrl = environment.apiUrl;  

  constructor(private http: HttpClient) {}

  getProfile(id: number): Observable<any> {
    console.log(`Obteniendo perfil de especialista: ${this.baseUrl}/api/v1/specialist/${id}/profile`);
    return this.http.get(`${this.baseUrl}/api/v1/specialist/${id}/profile`);
  }
  
  updateProfile(id: number, data: any): Observable<any> {
    console.log(`Actualizando perfil de especialista: ${this.baseUrl}/api/v1/specialist/${id}/profile`, data);
    return this.http.put(`${this.baseUrl}/api/v1/specialist/${id}/profile`, data);
  }
}