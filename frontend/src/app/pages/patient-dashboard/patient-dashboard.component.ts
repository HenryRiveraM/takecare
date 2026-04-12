import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [RouterModule, CommonModule, TranslatePipe],
  templateUrl: './patient-dashboard.component.html',
  styleUrls: ['./patient-dashboard.component.css']
})
export class PatientDashboardComponent implements OnInit {
  user: any;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.user = this.authService.getUser();
  }
}