import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-register-role',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './register-role.component.html',
  styleUrls: ['./register-role.component.css']
})
export class RegisterRoleComponent {

  selectedRole: string | null = null;

  constructor(private router: Router) {}

  selectRole(role: string): void {
    this.selectedRole = role;
  }

  continue(): void {
    if (!this.selectedRole) return;
    this.router.navigate(['/register', this.selectedRole]);
  }
}