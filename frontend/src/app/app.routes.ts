import { Routes } from '@angular/router';

// PÚBLICO
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { RecoverPasswordComponent } from './pages/recover-password/recover-password.component';

// REGISTRO
import { RegisterRoleComponent } from './pages/register-role/register-role.component';
import { RegisterPatientComponent } from './pages/register-patient/register-patient.component';
import { RegisterSpecialistComponent } from './pages/register-specialist/register-specialist.component';

// ADMIN
import { AdminComponent } from './pages/admin/admin.component';

// ESPECIALISTA
import { SpecialistDashboardComponent } from './pages/specialist-dashboard/specialist-dashboard.component';
import { SpecialistProfileComponent } from './pages/specialist-profile/specialist-profile.component';

// PACIENTE
import { PatientDashboardComponent } from './pages/patient-dashboard/patient-dashboard.component';
import { PatientProfileComponent } from './pages/patient-profile/patient-profile.component';

// OTROS
import { NotFoundComponent } from './pages/not-found/not-found.component';

export const routes: Routes = [

  // HOME
  {
    path: '',
    component: HomeComponent,
    data: { showNavbar: true }
  },

  // AUTH
  {
    path: 'login',
    component: LoginComponent,
    data: { showNavbar: false }
  },
  {
    path: 'recover-password',
    component: RecoverPasswordComponent,
    data: { showNavbar: false }
  },

  // REGISTRO
  {
    path: 'register-role',
    component: RegisterRoleComponent,
    data: { showNavbar: true }
  },
  {
    path: 'register/patient',
    component: RegisterPatientComponent,
    data: { showNavbar: true }
  },
  {
    path: 'register/specialist',
    component: RegisterSpecialistComponent,
    data: { showNavbar: true }
  },

  

  // ADMIN (PROTEGIDO)
  {
    path: 'admin',
    component: AdminComponent,
    //canActivate: [adminGuard], // comentar 
    data: { showNavbar: true }
  },

  // PACIENTE (Tu nueva ruta)
  {
    path: 'patient',
    data: { showNavbar: false },
    children: [
      {
        path: '', 
        component: PatientDashboardComponent
      },
      {
        path: 'profile',
        component: PatientProfileComponent
      }
    ]
  },

  // ESPECIALISTA 
  {
    path: 'specialist',
    data: { showNavbar: false },
    children: [
      {
        path: '',
        component: SpecialistDashboardComponent
      },
      {
        path: 'profile',
        component: SpecialistProfileComponent
      }
    ]
  },

  // NOT FOUND (Siempre al final)
  {
    path: '**',
    component: NotFoundComponent,
    data: { showNavbar: false }
  }
];