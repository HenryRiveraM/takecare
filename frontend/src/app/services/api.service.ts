import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private baseUrl = 'https://tragic-vere-takecare-cebbdb2d.koyeb.app';

  constructor(private http: HttpClient) {}

  registerPatient(data: any) {
    return this.http.post(
      `${this.baseUrl}/api/v1/users/register/patient`,
      data
    );
  }

  registerSpecialist(data: any) {
    return this.http.post(
      `${this.baseUrl}/api/v1/users/register/specialist`,
      data
    );
  }

}