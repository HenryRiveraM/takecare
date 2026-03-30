import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private baseUrl = 'https://tragic-vere-takecare-cebbdb2d.koyeb.app/';

  constructor(private http: HttpClient) {}

registerPatient(data: any) {
  return this.http.post(
    'https://tragic-vere-takecare-cebbdb2d.koyeb.app/api/v1/users/register/patient',
    data
  );
}

}