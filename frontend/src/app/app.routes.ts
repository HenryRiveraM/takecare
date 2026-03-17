import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { RegisterRoleComponent } from './pages/register-role/register-role.component';
import { RegisterPatientComponent } from './pages/register-patient/register-patient.component';
import { RecoverPasswordComponent } from './pages/recover-password/recover-password.component';
import { RegisterSpecialistComponent } from './pages/register-specialist/register-specialist.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';


export const routes: Routes = [
  { path: '', component: HomeComponent, data: { showNavbar: true } },

  { path: 'login', component: LoginComponent, data: { showNavbar: false } },
  { path: 'forgot-password', component: RecoverPasswordComponent, data: { showNavbar: false } },

  { path: 'register-role', component: RegisterRoleComponent, data: { showNavbar: true } },
  { path: 'register/specialist', component: RegisterSpecialistComponent, data: { showNavbar: true } },
  { path: 'register/patient', component: RegisterPatientComponent, data: { showNavbar: true } },

  {path: '**', component: NotFoundComponent, data: { showNavbar: false } }
];