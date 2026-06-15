import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { UserRole, UserStaffType } from '../models/auth.model';

export const authGuard: CanActivateFn = (route, _state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const requiredRoles = route.data?.['roles'] as UserRole[] | undefined;
  const requiredStaffTypes = route.data?.['staffTypes'] as UserStaffType[] | undefined;

  const hasRoleConstraint = requiredRoles && requiredRoles.length > 0;
  const hasStaffTypeConstraint = requiredStaffTypes && requiredStaffTypes.length > 0;

  if (hasRoleConstraint || hasStaffTypeConstraint) {
    const roleMatch = hasRoleConstraint && auth.hasAnyRole(requiredRoles!);
    const staffMatch = hasStaffTypeConstraint && auth.hasAnyStaffType(requiredStaffTypes!);
    if (!roleMatch && !staffMatch) {
      return router.createUrlTree(['/dashboard']);
    }
  }

  return true;
};
