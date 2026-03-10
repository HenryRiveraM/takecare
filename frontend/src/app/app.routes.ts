import { Routes } from '@angular/router';
// Importamos tu nuevo componente (ajusta la ruta si es necesario)
import { RegisterSpecialistComponent } from './modules/auth/pages/register-specialist/register-specialist.component';

export const routes: Routes = [
  // Esta es la ruta para entrar a tu formulario
  { 
    path: 'registro-especialista', 
    component: RegisterSpecialistComponent 
  },
  
  // Si tienes una página de inicio, se vería así:
  // { path: '', component: HomeComponent },
];
