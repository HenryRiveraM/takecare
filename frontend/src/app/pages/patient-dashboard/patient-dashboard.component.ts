import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-patient-dashboard',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './patient-dashboard.component.html',
  styleUrls: ['./patient-dashboard.component.css']
})
export class PatientDashboardComponent implements OnInit {
  user: any;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    // Esto jalará el nombre real si el login ya funciona
    this.user = this.authService.getUser();
  }
}