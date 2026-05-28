import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule, MatDividerModule],
  template: `
    <div class="header-bar page-header">
      @if (backRoute()) {
        <button mat-icon-button class="back-btn" [routerLink]="backRoute()">
          <mat-icon>arrow_back</mat-icon>
        </button>
      }
      <div class="header-title-block">
        <h1 class="title page-title">{{ title() }}</h1>
        @if (subtitle()) { <p class="subtitle">{{ subtitle() }}</p> }
      </div>
      <span class="spacer"></span>
      <div class="header-actions">
        <ng-content />
      </div>
    </div>
  `,
  styles: [`
    .header-bar { gap: 12px; flex-wrap: wrap; }
    .back-btn {
      border: 1px solid var(--dentis-border);
      background: var(--dentis-surface);
    }
    .header-title-block { display: flex; flex-direction: column; }
    .title { margin: 0; }
    .subtitle { margin: 0; font-size: 13px; color: var(--dentis-text-muted); }
    .spacer { flex: 1 1 auto; }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
  `]
})
export class PageHeaderComponent {
  title = input<string>('');
  subtitle = input<string>('');
  backRoute = input<string>('');
}

