import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-password-strength',
  standalone: true,
  imports: [CommonModule, MatProgressBarModule],
  templateUrl: './password-strength.component.html',
  styleUrls: ['./password-strength.component.scss']
})
export class PasswordStrengthComponent implements OnChanges {
  @Input() password = '';

  score = 0;
  strengthLabel = 'Too Weak';
  strengthColor = 'warn'; // 'warn' (red), 'accent' (orange/yellow), 'primary' (green)
  progressValue = 0;
  hints: string[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['password']) {
      this.calculateStrength(this.password || '');
    }
  }

  private calculateStrength(pwd: string): void {
    if (!pwd) {
      this.score = 0;
      this.progressValue = 0;
      this.strengthLabel = '';
      this.hints = [];
      return;
    }

    let calculatedScore = 0;
    const missing: string[] = [];

    // Rule 1: Length >= 8
    if (pwd.length >= 8) {
      calculatedScore++;
    } else {
      missing.push('At least 8 characters');
    }

    // Rule 2: Upper and lowercase
    if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) {
      calculatedScore++;
    } else {
      missing.push('Upper and lowercase letters');
    }

    // Rule 3: Numbers
    if (/\d/.test(pwd)) {
      calculatedScore++;
    } else {
      missing.push('At least one number');
    }

    // Rule 4: Special symbols
    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(pwd)) {
      calculatedScore++;
    } else {
      missing.push('At least one special symbol');
    }

    this.score = calculatedScore;
    this.progressValue = (this.score / 4) * 100;
    this.hints = missing;

    switch (this.score) {
      case 0:
      case 1:
        this.strengthLabel = 'Weak';
        this.strengthColor = 'warn';
        break;
      case 2:
        this.strengthLabel = 'Fair';
        this.strengthColor = 'accent';
        break;
      case 3:
        this.strengthLabel = 'Good';
        this.strengthColor = 'primary';
        break;
      case 4:
        this.strengthLabel = 'Strong';
        this.strengthColor = 'primary';
        break;
    }
  }
}
