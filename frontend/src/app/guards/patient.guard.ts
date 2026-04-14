import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const patientGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userData = localStorage.getItem('user');

  if (!userData) {
    router.navigate(['/login'], { replaceUrl: true });
    return false;
  }

  const user = JSON.parse(userData);
  if (user.role !== 1) {
    router.navigate(['/login'], { replaceUrl: true });
    return false;
  }

  return true;
};
