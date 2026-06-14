import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-recover-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslatePipe],
  templateUrl: './recover-password.component.html',
  styleUrls: ['./recover-password.component.css']
})
export class RecoverPasswordComponent {
  recoveryForm: FormGroup;
  submitted = false;
  isLoading = false;
  isSent = false;
  errorMsg = '';

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.recoveryForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  get f() {
    return this.recoveryForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMsg = '';

    if (this.recoveryForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.authService.forgotPassword(this.recoveryForm.value.email).subscribe({
      next: () => {
        this.isLoading = false;
        this.isSent = true;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMsg = err.error || 'No se pudo enviar el correo de recuperación';
      }
    });
  }
}
