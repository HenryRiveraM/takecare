import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private baseUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

registerPatient(data: any) {
  return this.http.post(
    'http://localhost:8080/api/v1/users/register/patient',
    data
  );
}

}