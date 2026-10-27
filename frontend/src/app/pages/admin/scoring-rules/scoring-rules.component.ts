import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AdminService } from '../../../core/services/admin.service';
import { ScoringRule } from '../../../core/models/models';

@Component({
  selector: 'app-scoring-rules',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './scoring-rules.component.html',
  styleUrls: ['./scoring-rules.component.scss']
})
export class ScoringRulesComponent implements OnInit {
  displayedColumns: string[] = ['level', 'trait', 'ruleName', 'ruleType', 'parameters', 'active', 'actions'];
  dataSource = new MatTableDataSource<ScoringRule>([]);
  isLoading = true;

  // Selected rule for modal editing
  editingRule: ScoringRule | null = null;
  editForm!: FormGroup;
  isSaving = false;

  constructor(
    private adminService: AdminService,
    private fb: FormBuilder,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initEditForm();
    this.loadRules();
  }

  private initEditForm(): void {
    this.editForm = this.fb.group({
      ruleName: ['', [Validators.required]],
      parameters: ['', [Validators.required, this.jsonValidator]]
    });
  }

  private jsonValidator(control: any) {
    if (!control.value) return null;
    try {
      JSON.parse(control.value);
      return null;
    } catch {
      return { invalidJson: true };
    }
  }

  loadRules(): void {
    this.isLoading = true;
    this.adminService
      .getScoringRules()
      .pipe(
        catchError(() => {
          // Mock data fallback
          const mockRules: ScoringRule[] = [
            {
              id: 'rule-1',
              level: 1,
              trait: 'Openness',
              ruleName: 'BigFive_Openness_StandardNorm',
              ruleType: 'WEIGHTED_SUM',
              parameters: JSON.stringify({ weight: 1.25, baseline: 50, standardDeviation: 10 }, null, 2),
              active: true
            },
            {
              id: 'rule-2',
              level: 1,
              trait: 'Conscientiousness',
              ruleName: 'BigFive_Conscientiousness_Linear',
              ruleType: 'WEIGHTED_SUM',
              parameters: JSON.stringify({ weight: 1.1, baseline: 48, standardDeviation: 12 }, null, 2),
              active: true
            },
            {
              id: 'rule-3',
              level: 2,
              trait: 'Ambiguity',
              ruleName: 'CognitiveAmbiguity_Threshold_v2',
              ruleType: 'THRESHOLD_PENALTY',
              parameters: JSON.stringify({ penaltyFactor: 0.15, maxThresholdMs: 45000 }, null, 2),
              active: true
            },
            {
              id: 'rule-4',
              level: 2,
              trait: 'DarkTheme',
              ruleName: 'DarkTriad_SubclinicalScore',
              ruleType: 'BAYESIAN_PROBABILITY',
              parameters: JSON.stringify({ prior: 0.2, alpha: 1.5, beta: 2.0 }, null, 2),
              active: true
            },
            {
              id: 'rule-5',
              level: 3,
              trait: 'Temperament',
              ruleName: 'Temperament_Synthesis_Matrix',
              ruleType: 'CLUSTERING_VECTOR',
              parameters: JSON.stringify({ distanceMetric: 'euclidean', k: 4, iterations: 100 }, null, 2),
              active: true
            }
          ];
          return of(mockRules);
        })
      )
      .subscribe({
        next: (rules: ScoringRule[]) => {
          this.dataSource.data = rules;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        }
      });
  }

  toggleRuleActive(rule: ScoringRule): void {
    const updatedStatus = !rule.active;
    this.adminService.updateScoringRule(rule.id, { active: updatedStatus }).subscribe({
      next: () => {
        rule.active = updatedStatus;
        this.snackBar.open(`Rule "${rule.ruleName}" ${updatedStatus ? 'activated' : 'deactivated'}.`, 'OK', {
          duration: 3000
        });
      },
      error: (err) => {
        rule.active = !updatedStatus; // revert
        this.snackBar.open(err?.error?.message || 'Failed to update rule status.', 'Close', { duration: 3000 });
      }
    });
  }

  openEdit(rule: ScoringRule): void {
    this.editingRule = rule;
    this.editForm.patchValue({
      ruleName: rule.ruleName,
      parameters: rule.parameters
    });
  }

  cancelEdit(): void {
    this.editingRule = null;
  }

  saveEdit(): void {
    if (this.editForm.invalid || !this.editingRule) {
      return;
    }

    this.isSaving = true;
    const formVals = this.editForm.value;

    this.adminService
      .updateScoringRule(this.editingRule.id, {
        ruleName: formVals.ruleName,
        parameters: formVals.parameters
      })
      .subscribe({
        next: (updatedRule: ScoringRule) => {
          this.isSaving = false;
          if (this.editingRule) {
            this.editingRule.ruleName = formVals.ruleName;
            this.editingRule.parameters = formVals.parameters;
          }
          this.editingRule = null;
          this.snackBar.open('Rule parameters successfully saved!', 'OK', { duration: 3000 });
        },
        error: (err) => {
          this.isSaving = false;
          this.snackBar.open(err?.error?.message || 'Failed to save rule parameters.', 'Close', { duration: 3000 });
        }
      });
  }
}
