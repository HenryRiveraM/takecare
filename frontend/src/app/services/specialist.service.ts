import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class SpecialistService {

  private apiUrl = 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/specialists';

  constructor(private http: HttpClient) {}

  getProfile(id: number) {
    return this.http.get(`${this.apiUrl}/${id}/profile`);
  }

  updateProfile(id: number, data: any) {
    return this.http.put(`${this.apiUrl}/${id}/profile`, data);
  }
}