import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.model';

export const authGuard: CanActivateFn = (route, _state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const requiredRoles = route.data?.['roles'] as UserRole[] | undefined;
  if (requiredRoles && requiredRoles.length > 0 && !auth.hasAnyRole(requiredRoles)) {
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
