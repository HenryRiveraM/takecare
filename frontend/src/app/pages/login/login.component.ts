import { Component } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { AuthService, ApiResponse, LoginResponse } from '../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslatePipe],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  loading = false;
  errorMsg = '';
  loginMessageType: 'error' | 'pending' | 'rejected' | '' = '';
  loginSuccess = false;
  showPassword = false;

  loginForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private translate: TranslateService
  ) {
    this.loginForm = this.fb.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  onSubmit(): void {

    if (this.loginForm.invalid) {
      this.showLoginMessage('error', 'login.errors.completeFields');
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.clearLoginMessage();
    this.loginSuccess = false;

    const credentials = this.loginForm.getRawValue();

    this.authService.login(credentials).subscribe({

      next: (response: ApiResponse<LoginResponse>) => {
        this.loading = false;

        if (response.success && response.data) {

          console.log('LOGIN RESPONSE:', response);

          const accountVerified = Number(response.data.accountVerified);

          // accountVerified: 1 = aceptado, 2 = pendiente, 0 = rechazado
          if (accountVerified === 2) {
            this.showLoginMessage('pending', 'login.errors.pendingApproval');
            return;
          }

          if (accountVerified === 0) {
            this.showLoginMessage('rejected', 'login.errors.rejectedAccount');
            return;
          }

          if (accountVerified === 1) {
            localStorage.setItem('user', JSON.stringify(response.data));
            this.loginSuccess = true;

            setTimeout(() => {
              this.redirectByRole(response.data!.role);
            }, 800);
          }

        } else {
          this.showLoginMessage('error', 'login.errors.unknown');
        }
      },

      error: (err) => {
        this.loading = false;

        if (err.status === 401) {
          const errorMessage = String(err.error?.error || err.error?.message || '').toLowerCase();

          if (
            errorMessage.includes('suspendida') ||
            errorMessage.includes('suspended') ||
            errorMessage.includes('inactiva')
          ) {
            this.errorMsg = this.translate.instant('login.errors.suspendedAccount');
          } else if (errorMessage.includes('pendiente') || errorMessage.includes('pending')) {
            this.errorMsg = this.translate.instant('login.errors.pendingApproval');
          } else if (
            errorMessage.includes('rechazada') ||
            errorMessage.includes('rechazado') ||
            errorMessage.includes('rejected')
          ) {
            this.errorMsg = this.translate.instant('login.errors.rejectedAccount');
          } else {
            this.errorMsg = this.translate.instant('login.errors.invalidCredentials');
          }
        } else {
          this.errorMsg = this.translate.instant('login.errors.connection');
        }

        console.error('Login error:', err);
      }
    });
  }

  private showLoginMessage(
    type: 'error' | 'pending' | 'rejected',
    translationKey: string
  ): void {
    this.loginMessageType = type;
    this.errorMsg = this.translate.instant(translationKey);
  }

  private clearLoginMessage(): void {
    this.loginMessageType = '';
    this.errorMsg = '';
  }

  redirectByRole(role: number): void {

    console.log('ROLE:', role);

    if (role === 3) {
      this.router.navigate(['/admin']);
    } else if (role === 2) {
      this.router.navigate(['/specialist']);
    } else {
      this.router.navigate(['/patient']);
    }
  }
}
