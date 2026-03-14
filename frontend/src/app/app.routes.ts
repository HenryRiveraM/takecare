import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { RegisterRoleComponent } from './pages/register-role/register-role.component';
import { RegisterPatientComponent } from './pages/register-patient/register-patient.component';

export const routes = [
    { path: '', component: HomeComponent },
    //{ path: 'login', component: LoginComponent },

    { path: 'register-role', component: RegisterRoleComponent },
    { path: 'register/patient', component: RegisterPatientComponent },

    //{ path: 'register/patient', component: RegisterPatientComponent },
    //{ path: 'register/specialist', component: RegisterSpecialistComponent }
];