import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

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

  constructor(private fb: FormBuilder) {
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
    
    setTimeout(() => {
      this.isLoading = false;
      this.isSent = true;
    }, 2000);
  }
}
