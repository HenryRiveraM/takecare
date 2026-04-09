import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SpecialistService {

  private readonly baseUrl =
    window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
      ? 'http://localhost:8080/api/v1/specialists'
      : 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/specialists';

  constructor(private http: HttpClient) {}

  getProfile(id: number): Observable<any> {
    console.log(`Obteniendo perfil de especialista: ${this.baseUrl}/${id}/profile`);
    return this.http.get(`${this.baseUrl}/${id}/profile`);
  }

  updateProfile(id: number, data: any): Observable<any> {
    console.log(`Actualizando perfil de especialista: ${this.baseUrl}/${id}/profile`, data);
    return this.http.put(`${this.baseUrl}/${id}/profile`, data);
  }
}