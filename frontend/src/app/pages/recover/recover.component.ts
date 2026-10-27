import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { RecoveryService } from '../../core/services/recovery.service';
import { PasswordStrengthComponent } from '../../shared/password-strength/password-strength.component';
import { PasswordResetResponse, RecoveryInitiateResponse } from '../../core/models/models';

@Component({
  selector: 'app-recover',
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
    MatSnackBarModule,
    PasswordStrengthComponent
  ],
  templateUrl: './recover.component.html',
  styleUrls: ['./recover.component.scss']
})
export class RecoverComponent implements OnInit {
  currentStep: 1 | 2 | 3 = 1;

  step1Form!: FormGroup;
  step2Form!: FormGroup;

  isSubmitting = false;
  errorMessage: string | null = null;
  remainingAttempts: number | null = null;

  recoveryToken = '';
  newRecoveryParagraph = '';
  recoveredUserId = '';

  constructor(
    private fb: FormBuilder,
    private recoveryService: RecoveryService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.step1Form = this.fb.group({
      userId: ['', [Validators.required]],
      paragraph: ['', [Validators.required, Validators.minLength(20)]]
    });

    this.step2Form = this.fb.group(
      {
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: this.passwordMatchValidator }
    );
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const pwd = control.get('newPassword')?.value;
    const confirm = control.get('confirmPassword')?.value;
    if (pwd && confirm && pwd !== confirm) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmitStep1(): void {
    if (this.step1Form.invalid) {
      this.step1Form.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    this.isSubmitting = true;

    const { userId, paragraph } = this.step1Form.value;
    this.recoveredUserId = userId.trim().toLowerCase();

    this.recoveryService.initiateRecovery(this.recoveredUserId, paragraph).subscribe({
      next: (res: RecoveryInitiateResponse) => {
        this.isSubmitting = false;
        this.recoveryToken = res.recoveryToken;
        this.currentStep = 2;
      },
      error: (err) => {
        this.isSubmitting = false;
        const data = err?.error;
        this.remainingAttempts = data?.remainingAttempts ?? null;
        if (this.remainingAttempts !== null) {
          this.errorMessage = `Invalid recovery paragraph. You have ${this.remainingAttempts} attempt(s) remaining before temporary lockout.`;
        } else {
          this.errorMessage = data?.message || 'Invalid recovery paragraph or user ID.';
        }
      }
    });
  }

  onSubmitStep2(): void {
    if (this.step2Form.invalid) {
      this.step2Form.markAllAsTouched();
      return;
    }

    this.errorMessage = null;
    this.isSubmitting = true;

    const { newPassword, confirmPassword } = this.step2Form.value;

    this.recoveryService.resetPassword(this.recoveryToken, newPassword, confirmPassword).subscribe({
      next: (res: PasswordResetResponse) => {
        this.isSubmitting = false;
        this.newRecoveryParagraph = res.newRecoveryParagraph;
        this.currentStep = 3;
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage = err?.error?.message || 'Password reset failed or token has expired. Please try again.';
      }
    });
  }

  copyNewParagraph(): void {
    if (!this.newRecoveryParagraph) return;
    navigator.clipboard.writeText(this.newRecoveryParagraph).then(() => {
      this.snackBar.open('New recovery paragraph copied to clipboard!', 'OK', { duration: 3000 });
    });
  }

  downloadNewParagraph(): void {
    if (!this.newRecoveryParagraph) return;
    const blob = new Blob([this.newRecoveryParagraph], { type: 'text/plain;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `mindmap-recovery-${this.recoveredUserId || 'account'}-new.txt`;
    a.click();
    window.URL.revokeObjectURL(url);
    this.snackBar.open('New recovery paragraph downloaded!', 'OK', { duration: 3000 });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
