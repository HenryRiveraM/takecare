import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register-role',
  templateUrl: './register-role.component.html',
  styleUrls: ['./register-role.component.css']
})
export class RegisterRoleComponent {

  selectedRole: string | null = null;

  constructor(private router: Router) {}

  selectRole(role: string) {
    this.selectedRole = role;
  }

  continue() {

    if (!this.selectedRole) return;

    this.router.navigate(['/register', this.selectedRole]);

  }

}