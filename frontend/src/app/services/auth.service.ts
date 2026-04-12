import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LoginRequest{
    email: string;
    password: string;
}

export interface LoginResponse{
    id: number;
    names: string;
    email: string;
    role: number;
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
    private apiUrl = "http://localhost:8080/api/v1/auth";

    constructor(private http: HttpClient) {}
    
    login(credentials: LoginRequest): Observable<ApiResponse<LoginResponse>> {
        return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/login`, credentials);
    }
}

