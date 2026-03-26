import { Component } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { AuthService, LoginRequest, ApiResponse, LoginResponse } from '../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loading = false;
  errorMsg = '';
  loginSuccess = false;

  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.errorMsg = 'Completa todos los campos correctamente';
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMsg = '';
    this.loginSuccess = false;

    const credentials = this.loginForm.getRawValue();

    this.authService.login(credentials).subscribe({
      next: (response: ApiResponse<LoginResponse>) => {
        this.loading = false;
        if (response.success && response.data) {
          localStorage.setItem('user', JSON.stringify(response.data));
          this.loginSuccess = true;
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 800);
        } else {
          this.errorMsg = response.error ?? 'Error desconocido';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = 'Error de conexión. Intenta de nuevo';
        console.error('Login error:', err);
      }
    });
  }
}