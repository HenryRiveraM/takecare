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

          console.log('LOGIN RESPONSE:', response);

          if (response.data.accountVerified === 2) {
            this.errorMsg = 'Tu cuenta aún está en revisión. Debes esperar la aprobación del administrador.';
            return;
          }

          if (response.data.accountVerified === 0) {
            this.errorMsg = 'Tu cuenta fue rechazada. Comunícate con soporte o vuelve a registrarte.';
            return;
          }

          localStorage.setItem('user', JSON.stringify(response.data));
          this.loginSuccess = true;

          setTimeout(() => {
            this.redirectByRole(response.data!.role);
          }, 800);

        } else {
          this.errorMsg = response.error ?? 'Error desconocido';
        }
      },

      error: (err) => {
        this.loading = false;

        if (err.status === 401) {
          this.errorMsg = 'Correo o contraseña incorrectos';
        } else if (err.status === 403) {
          this.errorMsg = err?.error?.error || 'Tu cuenta aún no está habilitada para iniciar sesión';
        } else {
          this.errorMsg = 'Error de conexión con el servidor';
        }

        console.error('Login error:', err);
      }
    });
  }

  redirectByRole(role: number) {

    console.log('ROLE:', role);

    if (role === 3) {
      this.router.navigate(['/admin']); 
    } else if (role === 2) {
      this.router.navigate(['/specialist']); 
    } else {
      this.router.navigate(['/patient'])
    }

  }
}