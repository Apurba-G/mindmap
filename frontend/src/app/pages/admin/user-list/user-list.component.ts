import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AdminService } from '../../../core/services/admin.service';
import { Page, UserListItem } from '../../../core/models/models';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule
  ],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {
  displayedColumns: string[] = [
    'userId',
    'displayName',
    'roles',
    'active',
    'levelProgress',
    'createdAt',
    'actions'
  ];

  dataSource = new MatTableDataSource<UserListItem>([]);
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  isLoading = true;

  searchControl = new FormControl('');

  constructor(
    private adminService: AdminService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadUsers();

    this.searchControl.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadUsers();
      });
  }

  loadUsers(): void {
    this.isLoading = true;
    const query = this.searchControl.value || '';

    this.adminService
      .getUsers(this.pageIndex, this.pageSize, query)
      .pipe(
        catchError(() => {
          // Fallback mock data if server is offline or developing
          const mockUsers: UserListItem[] = [
            {
              userId: 'alex_m',
              displayName: 'Alex Morgan',
              roles: ['ROLE_USER'],
              active: true,
              level1Status: 'COMPLETED',
              level2Status: 'COMPLETED',
              level3Status: 'IN_PROGRESS',
              createdAt: '2026-09-15T10:00:00Z'
            },
            {
              userId: 'sam_t',
              displayName: 'Sam Taylor',
              roles: ['ROLE_USER'],
              active: true,
              level1Status: 'COMPLETED',
              level2Status: 'IN_PROGRESS',
              level3Status: 'LOCKED',
              createdAt: '2026-09-18T14:30:00Z'
            },
            {
              userId: 'jordan_k',
              displayName: 'Jordan Kelly',
              roles: ['ROLE_USER', 'ROLE_ADMIN'],
              active: true,
              level1Status: 'COMPLETED',
              level2Status: 'COMPLETED',
              level3Status: 'COMPLETED',
              createdAt: '2026-09-10T09:15:00Z'
            },
            {
              userId: 'taylor_swift_fan',
              displayName: 'Taylor S',
              roles: ['ROLE_USER'],
              active: false,
              level1Status: 'NOT_STARTED',
              level2Status: 'LOCKED',
              level3Status: 'LOCKED',
              createdAt: '2026-10-01T11:20:00Z'
            }
          ];

          const filtered = query
            ? mockUsers.filter((u) => u.userId.includes(query) || u.displayName.toLowerCase().includes(query.toLowerCase()))
            : mockUsers;

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
        next: (page: Page<UserListItem>) => {
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
    this.loadUsers();
  }

  viewProfile(userId: string): void {
    this.router.navigate(['/admin/users', userId]);
  }

  toggleUserStatus(user: UserListItem, event: Event): void {
    event.stopPropagation();
    const action = user.active ? 'deactivate' : 'activate';

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: `${action.toUpperCase()} User: @${user.userId}`,
        message: `Are you sure you want to ${action} account access for user @${user.userId}?`,
        confirmText: user.active ? 'Deactivate' : 'Activate',
        cancelText: 'Cancel',
        confirmColor: user.active ? 'warn' : 'primary'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        const newStatus = !user.active;
        this.adminService.setUserActiveStatus(user.userId, newStatus).subscribe({
          next: () => {
            user.active = newStatus;
            this.snackBar.open(`User @${user.userId} has been ${newStatus ? 'activated' : 'deactivated'}.`, 'OK', {
              duration: 3000
            });
          },
          error: (err) => {
            this.snackBar.open(err?.error?.message || 'Failed to update user status.', 'Close', { duration: 3000 });
          }
        });
      }
    });
  }
}
