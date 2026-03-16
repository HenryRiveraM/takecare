import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para el *ngIf
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router'; // Para el routerLink

@Component({
  selector: 'app-forgot-password',
  standalone: true, // Esto indica que es versión nueva
  imports: [CommonModule, ReactiveFormsModule, RouterModule], // ¡Importante!
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  recoveryForm: FormGroup;
  submitted = false;
  isLoading = false;
  isSent = false; //

  constructor(private fb: FormBuilder) {
    this.recoveryForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  // Getter para facilitar acceso en HTML
  get f() { return this.recoveryForm.controls; }

  onSubmit() {
    this.submitted = true;

    if (this.recoveryForm.invalid) return;

    this.isLoading = true;
    

    console.log('Enviando correo a:', this.recoveryForm.value.email);
    
    // Simulamos respuesta del servidor
    setTimeout(() => {
      this.isLoading = false;
      this.isSent = true; // Indicamos que el correo ha sido "enviado"
      alert('Si el correo existe, recibirás un enlace pronto.');
    }, 2000);
  }
}
