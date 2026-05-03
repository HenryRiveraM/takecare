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
import { adminGuard } from './guards/admin.guard';
import { guestGuard } from './guards/guest.guard';

// ESPECIALISTA
import { SpecialistDashboardComponent } from './pages/specialist-dashboard/specialist-dashboard.component';
import { SpecialistProfileComponent } from './pages/specialist-profile/specialist-profile.component';
import { specialistGuard } from './guards/specialist.guard';
import { SpecialistScheduleManagementComponent } from './pages/specialist-schedule-management/specialist-schedule-management.component';
import { SpecialistAppointmentsComponent } from './pages/specialist-appointments/specialist-appointments.component';

// PACIENTE
import { PatientDashboardComponent } from './pages/patient-dashboard/patient-dashboard.component';
import { PatientProfileComponent } from './pages/patient-profile/patient-profile.component';
import { patientGuard } from './guards/patient.guard';
import { PatientSearchSpecialistsComponent } from './pages/patient-search-specialists/patient-search-specialists.component';
import { PatientDocumentsComponent } from './pages/patient-documents/patient-documents.component';
import { PatientSpecialistProfileComponent } from './pages/patient-specialist-profile/patient-specialist-profile.component';


// OTROS
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { PatientAppointmentsComponent } from './pages/patient-appointments/patient-appointments.component';

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
    canActivate: [guestGuard],
    data: { showNavbar: false }
  },
  {
    path: 'recover-password',
    component: RecoverPasswordComponent,
    canActivate: [guestGuard],
    data: { showNavbar: false }
  },

  // REGISTRO
  {
    path: 'register-role',
    component: RegisterRoleComponent,
    canActivate: [guestGuard],
    data: { showNavbar: true }
  },
  {
    path: 'register/patient',
    component: RegisterPatientComponent,
    canActivate: [guestGuard],
    data: { showNavbar: true }
  },
  {
    path: 'register/specialist',
    component: RegisterSpecialistComponent,
    canActivate: [guestGuard],
    data: { showNavbar: true }
  },

  

  // ADMIN (PROTEGIDO)
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [adminGuard],
    data: { showNavbar: true }
  },

  // PACIENTE (Tu nueva ruta)
  {
    path: 'patient',
    //canActivate: [patientGuard],
    data: { showNavbar: false },
    children: [
      {
        path: '', 
        component: PatientDashboardComponent
      },
      {
        path: 'profile',
        component: PatientProfileComponent
      },
      {
        path: 'search-specialists',
        component: PatientSearchSpecialistsComponent
      },
      {
        path: 'specialists/:id',
        component: PatientSpecialistProfileComponent
      },
      { 
        path: 'documents', 
        component: PatientDocumentsComponent 
      },
      {
        path: 'appointments',
        component: PatientAppointmentsComponent
      }
    ]
  },

  // ESPECIALISTA 
  {
    path: 'specialist',
    //canActivate: [specialistGuard],
    data: { showNavbar: false },
    children: [
      {
        path: '',
        component: SpecialistDashboardComponent
      },
      {
        path: 'schedule',
        component: SpecialistScheduleManagementComponent
      },
      {
        path: 'profile',
        component: SpecialistProfileComponent
      },
      { 
        path: 'appointments', 
        component: SpecialistAppointmentsComponent 
      },
    ]
  },

  // NOT FOUND (Siempre al final)
  {
    path: '**',
    component: NotFoundComponent,
    data: { showNavbar: false }
  }
];
