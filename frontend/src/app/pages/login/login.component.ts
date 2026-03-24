import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginRequest, ApiResponse, LoginResponse } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
 
@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string | null = null;
  isLoading: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ){
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }
  
  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = null;

      const credentials: LoginRequest = this.loginForm.value;
      this.authService.login(credentials).subscribe({
        next: (response: ApiResponse<LoginResponse>) => {
          this.isLoading = false;
          if (response.success && response.data) {
            localStorage.setItem('user', JSON.stringify(response.data));
            this.router.navigate(['/dashboard']);
          } else {
            this.errorMessage = response.error ?? 'Error desconocido';
          }
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage = 'Error de conexión. Intenta de nuevo';
          console.error('Login error:', err);
        }   
      });
    }else {
      this.errorMessage = 'Por favor, completa todos los campos correctamente.';
    }
  }
}