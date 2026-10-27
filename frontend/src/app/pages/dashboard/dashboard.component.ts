import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../core/services/auth.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { ProfileService } from '../../core/services/profile.service';
import { LevelStatusResponse, UserInfo, UserProfileSummary } from '../../core/models/models';
import { DisclaimerFooterComponent } from '../../shared/disclaimer-footer/disclaimer-footer.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    DisclaimerFooterComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: UserInfo | null = null;
  levelStatus: LevelStatusResponse = {
    level1Status: 'NOT_STARTED',
    level2Status: 'LOCKED',
    level3Status: 'LOCKED'
  };
  profileSummary: UserProfileSummary | null = null;
  isLoading = true;

  constructor(
    private authService: AuthService,
    private assessmentService: AssessmentService,
    private profileService: ProfileService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;

    forkJoin({
      status: this.assessmentService.getLevelStatus().pipe(
        catchError(() => of({
          level1Status: 'NOT_STARTED' as const,
          level2Status: 'LOCKED' as const,
          level3Status: 'LOCKED' as const
        }))
      ),
      profile: this.profileService.getMyProfile().pipe(
        catchError(() => of(null))
      )
    }).subscribe({
      next: ({ status, profile }) => {
        this.levelStatus = status;
        this.profileSummary = profile;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  startOrResumeAssessment(level: number): void {
    this.router.navigate([`/assessments/level-${level}`]);
  }

  viewProfile(): void {
    this.router.navigate(['/profile']);
  }
}
