import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-disclaimer-footer',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './disclaimer-footer.component.html',
  styleUrls: ['./disclaimer-footer.component.scss']
})
export class DisclaimerFooterComponent {}
