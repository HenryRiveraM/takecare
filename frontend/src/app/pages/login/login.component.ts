import { Component } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  loading = false;
  errorMsg = '';
  loginSuccess = false;

  loginForm: any;

  constructor(
    private fb: FormBuilder,
    private router: Router
  ) {
    this.loginForm = this.fb.nonNullable.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(){

    if(this.loginForm.invalid){
      this.errorMsg = 'Completa todos los campos correctamente';
      return;
    }

    this.loading = true;
    this.errorMsg = '';

    const { email, password } = this.loginForm.getRawValue();

    setTimeout(() => {

      if(email !== 'test@test.com' || password !== '123456'){
        this.errorMsg = 'Correo o contraseña incorrectos';
        this.loading = false;
        return;
      }

      this.loginSuccess = true;

      localStorage.setItem('token', 'fake-token');
      localStorage.setItem('user', JSON.stringify({
        id: 1,
        email,
        role: 1
      }));

      setTimeout(() => {
        this.router.navigate(['/']);
      }, 800);

    }, 1000);
  }
}