import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  const userData = localStorage.getItem('user');

  if (!userData) {
    return true;
  }

  const user = JSON.parse(userData);

  if (user.role === 3) {
    router.navigate(['/admin'], { replaceUrl: true });
    return false;
  }

  if (user.role === 2) {
    router.navigate(['/specialist'], { replaceUrl: true });
    return false;
  }

  router.navigate(['/patient'], { replaceUrl: true });
  return false;
};
