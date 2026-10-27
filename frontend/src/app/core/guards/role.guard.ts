import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  const expectedRole = route.data['role'] as string;
  const expectedRoles = (route.data['roles'] as string[]) || (expectedRole ? [expectedRole] : []);

  if (expectedRoles.length > 0) {
    const hasAnyRole = expectedRoles.some((r) => authService.hasRole(r));
    if (!hasAnyRole) {
      return router.createUrlTree(['/dashboard']);
    }
  }

  return true;
};
