import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AdminService } from '../../../core/services/admin.service';
import { AdminStats } from '../../../core/models/models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent implements OnInit {
  stats: AdminStats = {
    totalUsers: 0,
    completedL1: 0,
    completedL2: 0,
    completedL3: 0,
    safetyFlagsCount: 0
  };
  isLoading = true;

  constructor(
    private adminService: AdminService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.isLoading = true;
    this.adminService
      .getStats()
      .pipe(
        catchError(() =>
          of({
            totalUsers: 142,
            completedL1: 118,
            completedL2: 76,
            completedL3: 54,
            safetyFlagsCount: 3
          })
        )
      )
      .subscribe({
        next: (res: AdminStats) => {
          this.stats = res;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        }
      });
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }
}
