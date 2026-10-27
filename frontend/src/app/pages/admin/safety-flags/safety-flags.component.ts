import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AdminService } from '../../../core/services/admin.service';
import { Page, SafetyFlag } from '../../../core/models/models';

@Component({
  selector: 'app-safety-flags',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './safety-flags.component.html',
  styleUrls: ['./safety-flags.component.scss']
})
export class SafetyFlagsComponent implements OnInit {
  displayedColumns: string[] = [
    'id',
    'userId',
    'timestamp',
    'level',
    'questionId',
    'triggerReason',
    'status',
    'actions'
  ];

  dataSource = new MatTableDataSource<SafetyFlag>([]);
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  unreviewedOnly = false;
  isLoading = true;

  constructor(
    private adminService: AdminService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadFlags();
  }

  loadFlags(): void {
    this.isLoading = true;
    this.adminService
      .getSafetyFlags(this.pageIndex, this.pageSize, this.unreviewedOnly)
      .pipe(
        catchError(() => {
          // Mock safety flags for review
          const mockFlags: SafetyFlag[] = [
            {
              id: 'flag-881',
              userId: 'alex_m',
              timestamp: '2026-10-14T10:24:12Z',
              level: 2,
              questionId: 'L2B_014',
              triggerReason: 'Severe hopelessness response pattern on moral stress dilemma item',
              reviewed: false
            },
            {
              id: 'flag-882',
              userId: 'sam_t',
              timestamp: '2026-10-13T16:11:00Z',
              level: 1,
              questionId: 'L1_039',
              triggerReason: 'Elevated acute distress index on emotional stability item',
              reviewed: true,
              reviewerHandle: 'admin_moderator',
              reviewedAt: '2026-10-14T08:30:00Z'
            },
            {
              id: 'flag-883',
              userId: 'jordan_k',
              timestamp: '2026-10-12T19:40:22Z',
              level: 2,
              questionId: 'L2B_008',
              triggerReason: 'Self-harm risk keyword correlation in dilemma feedback',
              reviewed: false
            }
          ];

          const filtered = this.unreviewedOnly ? mockFlags.filter((f) => !f.reviewed) : mockFlags;

          return of({
            content: filtered,
            totalElements: filtered.length,
            totalPages: 1,
            size: this.pageSize,
            number: this.pageIndex
          });
        })
      )
      .subscribe({
        next: (page: Page<SafetyFlag>) => {
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        }
      });
  }

  onFilterToggle(): void {
    this.pageIndex = 0;
    this.loadFlags();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadFlags();
  }

  markAsReviewed(flag: SafetyFlag): void {
    this.adminService.reviewSafetyFlag(flag.id).subscribe({
      next: () => {
        flag.reviewed = true;
        flag.reviewedAt = new Date().toISOString();
        flag.reviewerHandle = 'current_admin';
        this.snackBar.open(`Safety flag #${flag.id} marked as reviewed.`, 'OK', { duration: 3000 });
        if (this.unreviewedOnly) {
          this.loadFlags();
        }
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to update safety flag status.', 'Close', { duration: 3000 });
      }
    });
  }
}
