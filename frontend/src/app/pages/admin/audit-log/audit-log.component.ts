import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AdminService } from '../../../core/services/admin.service';
import { AuditLogEntry, Page } from '../../../core/models/models';

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss']
})
export class AuditLogComponent implements OnInit {
  displayedColumns: string[] = ['timestamp', 'adminHandle', 'targetUser', 'action', 'ipAddress', 'details'];
  dataSource = new MatTableDataSource<AuditLogEntry>([]);
  totalElements = 0;
  pageSize = 15;
  pageIndex = 0;
  isLoading = true;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.isLoading = true;
    this.adminService
      .getAuditLogs(this.pageIndex, this.pageSize)
      .pipe(
        catchError(() => {
          // Mock data fallback
          const mockLogs: AuditLogEntry[] = [
            {
              id: 'log-101',
              timestamp: '2026-10-14T14:15:22Z',
              adminHandle: 'admin_security',
              targetUser: 'alex_m',
              action: 'PROFILE_VIEW',
              ipAddress: '192.168.1.102',
              details: 'Reviewed Level 2 cognitive stress assessment scores'
            },
            {
              id: 'log-102',
              timestamp: '2026-10-14T13:45:10Z',
              adminHandle: 'admin_root',
              targetUser: 'system',
              action: 'SCORING_RULE_UPDATE',
              ipAddress: '192.168.1.101',
              details: 'Updated parameters for DarkTriad_SubclinicalScore rule'
            },
            {
              id: 'log-103',
              timestamp: '2026-10-14T11:20:05Z',
              adminHandle: 'admin_moderator',
              targetUser: 'flag-882',
              action: 'SAFETY_FLAG_REVIEW',
              ipAddress: '10.0.4.15',
              details: 'Reviewed and dismissed false positive question response'
            },
            {
              id: 'log-104',
              timestamp: '2026-10-14T09:12:44Z',
              adminHandle: 'admin_security',
              targetUser: 'taylor_swift_fan',
              action: 'USER_DEACTIVATED',
              ipAddress: '192.168.1.102',
              details: 'Account deactivated per policy violation report'
            },
            {
              id: 'log-105',
              timestamp: '2026-10-13T18:30:19Z',
              adminHandle: 'system_cron',
              targetUser: 'global',
              action: 'DATA_PRUNING',
              ipAddress: '127.0.0.1',
              details: 'Pruned unreferenced expired recovery tokens'
            }
          ];

          return of({
            content: mockLogs,
            totalElements: mockLogs.length,
            totalPages: 1,
            size: this.pageSize,
            number: this.pageIndex
          });
        })
      )
      .subscribe({
        next: (page: Page<AuditLogEntry>) => {
          this.dataSource.data = page.content;
          this.totalElements = page.totalElements;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        }
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadLogs();
  }
}
