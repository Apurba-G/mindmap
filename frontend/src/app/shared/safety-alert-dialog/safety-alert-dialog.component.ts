import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface SafetyDialogData {
  message?: string;
}

@Component({
  selector: 'app-safety-alert-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './safety-alert-dialog.component.html',
  styleUrls: ['./safety-alert-dialog.component.scss']
})
export class SafetyAlertDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SafetyAlertDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SafetyDialogData
  ) {}

  onClose(): void {
    this.dialogRef.close();
  }
}
