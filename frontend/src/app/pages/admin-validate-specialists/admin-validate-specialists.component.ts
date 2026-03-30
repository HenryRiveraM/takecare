import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-validate-specialists',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-validate-specialists.component.html',
  styleUrls: ['./admin-validate-specialists.component.css']
})
export class AdminValidateSpecialistsComponent {

  specialists = [
    {
      id: 1,
      name: 'Dr. Juan Pérez',
      email: 'juan@test.com',
      specialty: 'Psicología',
      certification_img: 'assets/images/cert1.png'
    }
  ];

  approve(id: number){
    console.log('APROBADO', id);
  }

  reject(id: number){
    console.log('RECHAZADO', id);
  }

}