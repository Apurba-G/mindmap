import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../core/services/auth.service';
import { LoginResponse } from '../../core/models/models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  hidePassword = true;
  isLoading = false;
  errorMessage: string | null = null;
  accountLockedMessage: string | null = null;
  mustChangePasswordMessage: string | null = null;
  returnUrl = '/dashboard';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // If already authenticated, redirect to dashboard or returnUrl
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';

    this.loginForm = this.fb.group({
      userId: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    this.accountLockedMessage = null;
    this.mustChangePasswordMessage = null;
    this.isLoading = true;

    const { userId, password } = this.loginForm.value;

    this.authService.login(userId.trim().toLowerCase(), password).subscribe({
      next: (response: LoginResponse) => {
        this.isLoading = false;
        if (response.mustChangePassword) {
          this.router.navigate(['/change-password']);
        } else if (response.roles && (response.roles.includes('ROLE_ADMIN') || response.roles.includes('ADMIN'))) {
          // If admin, navigate to admin dashboard or returnUrl if specified
          this.router.navigateByUrl(this.returnUrl !== '/dashboard' ? this.returnUrl : '/admin');
        } else {
          this.router.navigateByUrl(this.returnUrl);
        }
      },
      error: (err) => {
        this.isLoading = false;
        const errData = err?.error;
        if (err?.status === 423 || errData?.lockedUntil) {
          const unlockTime = errData.lockedUntil ? new Date(errData.lockedUntil).toLocaleTimeString() : '15 minutes';
          this.accountLockedMessage = `Account is locked due to multiple failed attempts. Please try again after ${unlockTime}.`;
        } else if (err?.status === 403 && errData?.mustChangePassword) {
          this.mustChangePasswordMessage = 'You must change your initial temporary password before accessing the platform.';
          this.router.navigate(['/change-password']);
        } else {
          this.errorMessage = errData?.message || 'Invalid user ID or password. Please try again.';
        }
      }
    });
  }
}
