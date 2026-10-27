import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { AssessmentService } from '../../core/services/assessment.service';
import { LevelStatusResponse, UserInfo, UserProfileSummary } from '../../core/models/models';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';
import { DisclaimerFooterComponent } from '../../shared/disclaimer-footer/disclaimer-footer.component';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    DisclaimerFooterComponent
  ],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit {
  currentUser: UserInfo | null = null;
  profileSummary: UserProfileSummary | null = null;
  levelStatus: LevelStatusResponse = {
    level1Status: 'NOT_STARTED',
    level2Status: 'LOCKED',
    level3Status: 'LOCKED'
  };

  isLoading = true;
  isExporting = false;
  isDeleting = false;

  constructor(
    private authService: AuthService,
    private profileService: ProfileService,
    private assessmentService: AssessmentService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadProfileData();
  }

  loadProfileData(): void {
    this.isLoading = true;
    forkJoin({
      profile: this.profileService.getMyProfile().pipe(catchError(() => of(null))),
      status: this.assessmentService.getLevelStatus().pipe(
        catchError(() =>
          of({
            level1Status: 'NOT_STARTED' as const,
            level2Status: 'LOCKED' as const,
            level3Status: 'LOCKED' as const
          })
        )
      )
    }).subscribe({
      next: ({ profile, status }) => {
        this.profileSummary = profile;
        this.levelStatus = status;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  exportData(): void {
    this.isExporting = true;
    this.profileService.exportData().subscribe({
      next: (blob: Blob) => {
        this.isExporting = false;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `mindmap-profile-export-${this.currentUser?.userId || 'user'}.json`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.snackBar.open('Data exported successfully!', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.isExporting = false;
        this.snackBar.open(err?.error?.message || 'Failed to export data.', 'Close', { duration: 3000 });
      }
    });
  }

  confirmDeleteAccount(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '460px',
      data: {
        title: 'Delete Account Permanently',
        message:
          'Are you sure you want to delete your account? All assessment responses, profile summaries, and cryptographic keys will be irreversibly erased in compliance with privacy regulations.',
        confirmText: 'Delete Everything',
        cancelText: 'Cancel',
        confirmColor: 'warn',
        icon: 'delete_forever'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteAccount();
      }
    });
  }

  private deleteAccount(): void {
    this.isDeleting = true;
    this.profileService.deleteAccount().subscribe({
      next: () => {
        this.isDeleting = false;
        this.snackBar.open('Account and associated data deleted.', 'OK', { duration: 3000 });
        this.authService.logout();
      },
      error: (err) => {
        this.isDeleting = false;
        this.snackBar.open(err?.error?.message || 'Failed to delete account.', 'Close', { duration: 4000 });
      }
    });
  }
}
