import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ContentWarningData {
  title?: string;
  message?: string;
  partName?: string;
}

@Component({
  selector: 'app-content-warning-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './content-warning-dialog.component.html',
  styleUrls: ['./content-warning-dialog.component.scss']
})
export class ContentWarningDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ContentWarningDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ContentWarningData
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onDecline(): void {
    this.dialogRef.close(false);
  }
}
