import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: number;
  names: string;
  email: string;
  role: number;
  accountVerified: number; // 1: aceptado, 2: pendiente, 3: rechazado
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = environment.apiUrl;
    
  constructor(private http: HttpClient) {}

  login(credentials: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(
      `${this.apiUrl}/api/v1/auth/login`,
      credentials
    );
  }

  forgotPassword(email: string): Observable<string> {
    const frontendUrl = window.location.origin;
    return this.http.post(
      `${this.apiUrl}/api/v1/auth/forgot-password`,
      { email, frontendUrl },
      { responseType: 'text' }
    );
  }

  resetPassword(token: string, newPassword: string): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/api/v1/auth/reset-password`,
      { token, newPassword },
      { responseType: 'text' }
    );
  }

  saveUser(user: LoginResponse): void {
    localStorage.setItem('user', JSON.stringify(user));
  }

  getUser(): LoginResponse | null {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  isLogged(): boolean {
    return !!this.getUser();
  }

  logout(): void {
    localStorage.removeItem('user');
  }
}