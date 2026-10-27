import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AssessmentService } from '../../core/services/assessment.service';
import { QuestionDto, SessionResponse, SubmitAnswerRequest, SubmitAnswerResponse } from '../../core/models/models';
import { SafetyAlertDialogComponent } from '../../shared/safety-alert-dialog/safety-alert-dialog.component';
import { ContentWarningDialogComponent } from '../../shared/content-warning-dialog/content-warning-dialog.component';
import { DisclaimerFooterComponent } from '../../shared/disclaimer-footer/disclaimer-footer.component';

@Component({
  selector: 'app-assessment-level',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatRadioModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    DisclaimerFooterComponent
  ],
  templateUrl: './assessment-level.component.html',
  styleUrls: ['./assessment-level.component.scss']
})
export class AssessmentLevelComponent implements OnInit, OnDestroy {
  level = 1;
  sessionId = '';
  currentQuestion: QuestionDto | null = null;
  currentQuestionIndex = 0;
  totalQuestions = 0;
  selectedOptionId: string | null = null;

  isLoading = true;
  isSubmitting = false;
  isCompleted = false;

  // Content warning tracking
  showInlineWarning = false;
  warningAcknowledged = false;
  currentPart: string | undefined = undefined;

  // Question Timing
  private questionStartTime = 0;
  timeLimitSeconds = 0;
  timeLeftSeconds = 0;
  private timerInterval: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private assessmentService: AssessmentService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Determine level from route data or URL
    const routeLevel = this.route.snapshot.data['level'];
    if (routeLevel) {
      this.level = Number(routeLevel);
    } else {
      const url = this.router.url;
      if (url.includes('level-2')) this.level = 2;
      else if (url.includes('level-3')) this.level = 3;
      else this.level = 1;
    }

    this.initializeSession();
  }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  get progressPercentage(): number {
    if (this.totalQuestions <= 0) return 0;
    return Math.round((this.currentQuestionIndex / this.totalQuestions) * 100);
  }

  get timerPercentage(): number {
    if (this.timeLimitSeconds <= 0) return 100;
    return Math.max(0, (this.timeLeftSeconds / this.timeLimitSeconds) * 100);
  }

  private initializeSession(): void {
    this.isLoading = true;
    this.assessmentService.createSession(this.level).subscribe({
      next: (session: SessionResponse) => {
        this.sessionId = session.sessionId;
        this.currentQuestionIndex = session.currentQuestionIndex;
        this.totalQuestions = session.totalQuestions;
        this.currentPart = session.currentPart;
        this.isCompleted = session.completed;

        if (session.completed) {
          this.isLoading = false;
          return;
        }

        if (session.warningRequired && !session.warningAcknowledged) {
          this.handleWarningRequired(session);
        } else {
          this.loadQuestion(session.currentQuestion);
          this.isLoading = false;
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(err?.error?.message || 'Failed to start assessment session.', 'Close', {
          duration: 4000
        });
        this.router.navigate(['/dashboard']);
      }
    });
  }

  private handleWarningRequired(session: SessionResponse): void {
    const dialogRef = this.dialog.open(ContentWarningDialogComponent, {
      width: '540px',
      disableClose: true,
      data: {
        title: `Sensitive Content Notice — Part ${session.currentPart || 'B'}`,
        message:
          'This upcoming section explores hypothetical dilemma situations and dark traits. You may choose to skip questions or opt out at any time.',
        partName: session.currentPart
      }
    });

    dialogRef.afterClosed().subscribe((accepted: boolean) => {
      if (accepted) {
        this.assessmentService.acknowledgeWarning(this.level, session.currentPart || 'B', true).subscribe({
          next: () => {
            this.warningAcknowledged = true;
            this.loadQuestion(session.currentQuestion);
            this.isLoading = false;
          },
          error: () => {
            this.loadQuestion(session.currentQuestion);
            this.isLoading = false;
          }
        });
      } else {
        this.router.navigate(['/dashboard']);
      }
    });
  }

  private loadQuestion(question: QuestionDto | null): void {
    this.stopTimer();
    this.selectedOptionId = null;
    this.currentQuestion = question;

    if (!question) {
      this.isCompleted = true;
      return;
    }

    if (question.contentWarning) {
      this.showInlineWarning = true;
    } else {
      this.showInlineWarning = false;
    }

    // Record start time for responseTimeMs tracking
    this.questionStartTime = Date.now();

    // Check if timed question
    if (question.timeLimitSeconds && question.timeLimitSeconds > 0) {
      this.timeLimitSeconds = question.timeLimitSeconds;
      this.timeLeftSeconds = question.timeLimitSeconds;
      this.startTimer();
    } else {
      this.timeLimitSeconds = 0;
      this.timeLeftSeconds = 0;
    }
  }

  private startTimer(): void {
    this.stopTimer();
    this.timerInterval = setInterval(() => {
      if (this.timeLeftSeconds > 0) {
        this.timeLeftSeconds--;
      } else {
        this.stopTimer();
        // Time expired: auto-advance
        if (this.currentQuestion?.isSkippable) {
          this.skipCurrentQuestion();
        } else if (this.selectedOptionId) {
          this.submitCurrentAnswer();
        } else {
          // If unselected, pick first available option or proceed
          if (this.currentQuestion && this.currentQuestion.options.length > 0) {
            this.selectedOptionId = this.currentQuestion.options[0].id;
            this.submitCurrentAnswer();
          }
        }
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  acknowledgeInlineWarning(): void {
    this.showInlineWarning = false;
  }

  submitCurrentAnswer(): void {
    if (!this.selectedOptionId || this.isSubmitting || !this.currentQuestion) {
      return;
    }

    this.isSubmitting = true;
    const responseTimeMs = Date.now() - this.questionStartTime;

    const payload: SubmitAnswerRequest = {
      questionId: this.currentQuestion.id,
      selectedOptionId: this.selectedOptionId,
      responseTimeMs
    };

    this.assessmentService.submitAnswer(this.sessionId, payload).subscribe({
      next: (res: SubmitAnswerResponse) => {
        this.isSubmitting = false;
        this.handleNextResponse(res);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.snackBar.open(err?.error?.message || 'Error submitting response.', 'Close', { duration: 3000 });
      }
    });
  }

  skipCurrentQuestion(): void {
    if (this.isSubmitting || !this.currentQuestion) return;

    this.isSubmitting = true;
    this.assessmentService.skipQuestion(this.sessionId, this.currentQuestion.id).subscribe({
      next: (res: SubmitAnswerResponse) => {
        this.isSubmitting = false;
        this.handleNextResponse(res);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.snackBar.open(err?.error?.message || 'Error skipping question.', 'Close', { duration: 3000 });
      }
    });
  }

  private handleNextResponse(res: SubmitAnswerResponse): void {
    // Check if safety flag triggered
    if (res.safetyFlagTriggered) {
      this.openSafetyDialog(res.safetyAlertMessage);
    }

    if (res.completed) {
      this.finishLevel();
    } else {
      this.currentQuestionIndex = res.questionIndex;
      this.totalQuestions = res.totalQuestions;
      this.loadQuestion(res.nextQuestion);
    }
  }

  private openSafetyDialog(message?: string): void {
    this.dialog.open(SafetyAlertDialogComponent, {
      width: '540px',
      data: { message }
    });
  }

  finishLevel(): void {
    this.stopTimer();
    this.isCompleted = true;
    this.assessmentService.completeLevel(this.sessionId).subscribe({
      next: () => {},
      error: () => {}
    });
  }

  returnToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
