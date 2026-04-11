import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

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

  constructor(
    private fb: FormBuilder,
    private translate: TranslateService
  ) {
    this.recoveryForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  get f() {
    return this.recoveryForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.recoveryForm.invalid) {
      return;
    }

    this.isLoading = true;

    console.log('Enviando correo a:', this.recoveryForm.value.email);
    
    setTimeout(() => {
      this.isLoading = false;
      this.isSent = true;
      alert(this.translate.instant('recoverPassword.alertMessage'));
    }, 2000);
  }
}