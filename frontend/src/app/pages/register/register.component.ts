import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, Subscription, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, filter, catchError } from 'rxjs/operators';

import { AuthService } from '../../core/services/auth.service';
import { UserIdService } from '../../core/services/user-id.service';
import { PasswordStrengthComponent } from '../../shared/password-strength/password-strength.component';
import { RegisterRequest, RegisterResponse } from '../../core/models/models';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatCardModule,
    MatSnackBarModule,
    PasswordStrengthComponent
  ],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit, OnDestroy {
  @ViewChild('stepper') stepper!: MatStepper;

  accountForm!: FormGroup;
  recoveryVerificationForm!: FormGroup;

  isSubmitting = false;
  isCheckingUserId = false;
  userIdAvailable: boolean | null = null;
  userIdAvailabilityMessage = '';
  suggestions: string[] = [];

  generatedRecoveryParagraph = '';
  expectedFirstSentence = '';
  expectedLastSentence = '';
  createdUserId = '';

  private destroy$ = new Subject<void>();
  private userIdChange$ = new Subject<string>();
  private nameChange$ = new Subject<string>();

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private userIdService: UserIdService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.setupUserIdCheckPipeline();
    this.setupSuggestionsPipeline();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForms(): void {
    this.accountForm = this.fb.group(
      {
        displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
        userId: [
          '',
          [
            Validators.required,
            Validators.minLength(4),
            Validators.maxLength(20),
            Validators.pattern(/^[a-z0-9._]+$/)
          ]
        ],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: this.passwordMatchValidator }
    );

    this.recoveryVerificationForm = this.fb.group({
      firstSentence: ['', [Validators.required]],
      lastSentence: ['', [Validators.required]]
    });
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirm = control.get('confirmPassword')?.value;
    if (password && confirm && password !== confirm) {
      return { passwordMismatch: true };
    }
    return null;
  }

  private setupUserIdCheckPipeline(): void {
    this.accountForm.get('userId')?.valueChanges.subscribe((val: string) => {
      this.userIdAvailable = null;
      this.userIdAvailabilityMessage = '';
      if (val && this.accountForm.get('userId')?.valid) {
        this.userIdChange$.next(val);
      }
    });

    this.userIdChange$
      .pipe(
        debounceTime(500),
        distinctUntilChanged(),
        switchMap((userId) => {
          this.isCheckingUserId = true;
          return this.userIdService.checkAvailability(userId).pipe(
            catchError(() => of({ available: false, checkedVia: 'error', message: 'Unable to check availability' }))
          );
        })
      )
      .subscribe((res) => {
        this.isCheckingUserId = false;
        this.userIdAvailable = res.available;
        this.userIdAvailabilityMessage = res.available ? 'User ID is available' : (res.message || 'User ID is already taken');
      });
  }

  private setupSuggestionsPipeline(): void {
    this.accountForm.get('displayName')?.valueChanges.subscribe((name: string) => {
      if (name && name.trim().length >= 2) {
        this.nameChange$.next(name);
      } else {
        this.suggestions = [];
      }
    });

    this.nameChange$
      .pipe(
        debounceTime(600),
        distinctUntilChanged(),
        filter((name) => name.trim().length >= 2),
        switchMap((name) =>
          this.userIdService.getSuggestions(name.trim()).pipe(
            catchError(() => of([]))
          )
        )
      )
      .subscribe((suggestions) => {
        this.suggestions = suggestions.slice(0, 5);
      });
  }

  selectSuggestion(suggestion: string): void {
    this.accountForm.get('userId')?.setValue(suggestion);
    this.accountForm.get('userId')?.markAsDirty();
  }

  onSubmitStep1(): void {
    if (this.accountForm.invalid || this.userIdAvailable === false) {
      this.accountForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const formVal = this.accountForm.value;
    const request: RegisterRequest = {
      displayName: formVal.displayName.trim(),
      userId: formVal.userId.trim().toLowerCase(),
      password: formVal.password,
      confirmPassword: formVal.confirmPassword
    };

    this.authService.register(request).subscribe({
      next: (response: RegisterResponse) => {
        this.isSubmitting = false;
        this.createdUserId = response.userId;
        this.generatedRecoveryParagraph = response.recoveryParagraph;
        this.parseExpectedSentences(response.recoveryParagraph);
        this.stepper.next();
      },
      error: (err) => {
        this.isSubmitting = false;
        const errMsg = err?.error?.message || 'Registration failed. Please verify your details.';
        this.snackBar.open(errMsg, 'Close', { duration: 5000 });
      }
    });
  }

  private parseExpectedSentences(paragraph: string): void {
    if (!paragraph) return;
    // Split sentences by dot followed by space or end of string
    const sentences = paragraph
      .split(/(?<=[.!?])\s+/)
      .map((s) => s.trim())
      .filter((s) => s.length > 0);

    if (sentences.length > 0) {
      this.expectedFirstSentence = sentences[0];
      this.expectedLastSentence = sentences[sentences.length - 1];
    } else {
      this.expectedFirstSentence = paragraph.trim();
      this.expectedLastSentence = paragraph.trim();
    }
  }

  copyParagraph(): void {
    if (!this.generatedRecoveryParagraph) return;
    navigator.clipboard.writeText(this.generatedRecoveryParagraph).then(() => {
      this.snackBar.open('Recovery paragraph copied to clipboard!', 'OK', { duration: 3000 });
    });
  }

  downloadParagraph(): void {
    if (!this.generatedRecoveryParagraph) return;
    const blob = new Blob([this.generatedRecoveryParagraph], { type: 'text/plain;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `mindmap-recovery-${this.createdUserId || 'account'}.txt`;
    a.click();
    window.URL.revokeObjectURL(url);
    this.snackBar.open('Recovery paragraph downloaded!', 'OK', { duration: 3000 });
  }

  isRecoverySentenceValid(): boolean {
    const firstTyped = (this.recoveryVerificationForm.get('firstSentence')?.value || '').trim();
    const lastTyped = (this.recoveryVerificationForm.get('lastSentence')?.value || '').trim();

    const normalize = (text: string) => text.toLowerCase().replace(/[^a-z0-9]/g, '');

    const firstValid = normalize(firstTyped) === normalize(this.expectedFirstSentence);
    const lastValid = normalize(lastTyped) === normalize(this.expectedLastSentence);

    return firstValid && lastValid;
  }

  onSubmitStep2(): void {
    if (!this.isRecoverySentenceValid()) {
      this.snackBar.open('Sentences do not match your recovery paragraph. Please check carefully.', 'Close', {
        duration: 4000
      });
      return;
    }
    this.stepper.next();
  }

  navigateToLevel1(): void {
    this.router.navigate(['/assessments/level-1']);
  }

  navigateToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
