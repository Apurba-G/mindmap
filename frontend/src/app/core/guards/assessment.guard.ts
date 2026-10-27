import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { AssessmentService } from '../services/assessment.service';
import { LevelStatusResponse } from '../models/models';

export const assessmentGuard: CanActivateFn = (route, state) => {
  const assessmentService = inject(AssessmentService);
  const router = inject(Router);

  // Level can be passed in route data (e.g. data: { level: 2 })
  const targetLevel = route.data['level'] ? Number(route.data['level']) : 1;

  if (targetLevel === 1) {
    return true;
  }

  return assessmentService.getLevelStatus().pipe(
    map((status: LevelStatusResponse) => {
      if (targetLevel === 2) {
        if (status.level1Status === 'COMPLETED') {
          return true;
        } else {
          return router.createUrlTree(['/dashboard']);
        }
      }

      if (targetLevel === 3) {
        if (status.level2Status === 'COMPLETED') {
          return true;
        } else {
          return router.createUrlTree(['/dashboard']);
        }
      }

      return true;
    }),
    catchError(() => {
      return of(router.createUrlTree(['/dashboard']));
    })
  );
};
