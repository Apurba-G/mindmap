import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../core/services/auth.service';
import { PasswordStrengthComponent } from '../../shared/password-strength/password-strength.component';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PasswordStrengthComponent
  ],
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.scss']
})
export class ChangePasswordComponent implements OnInit {
  changePasswordForm!: FormGroup;
  isFirstLoginChange = false;
  isSubmitting = false;
  hideOldPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.isFirstLoginChange = this.authService.mustChangePassword();

    this.changePasswordForm = this.fb.group(
      {
        currentPassword: [
          '',
          this.isFirstLoginChange ? [] : [Validators.required]
        ],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: this.passwordMatchValidator }
    );
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const newPwd = control.get('newPassword')?.value;
    const confirm = control.get('confirmPassword')?.value;
    if (newPwd && confirm && newPwd !== confirm) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.changePasswordForm.invalid) {
      this.changePasswordForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;

    const { currentPassword, newPassword } = this.changePasswordForm.value;

    const payload = {
      currentPassword: this.isFirstLoginChange ? null : currentPassword,
      newPassword
    };

    this.http.post('/api/auth/change-password', payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.authService.setMustChangePassword(false);
        this.snackBar.open('Password successfully updated!', 'OK', { duration: 3000 });
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err?.error?.message || 'Failed to update password. Please check your current password.';
      }
    });
  }
}
