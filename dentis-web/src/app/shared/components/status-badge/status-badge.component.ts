import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type StatusTone =
  | 'active'
  | 'inactive'
  | 'pending'
  | 'scheduled'
  | 'completed'
  | 'cancelled'
  | 'approved'
  | 'confirmed'
  | 'in-progress';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="status-chip" [ngClass]="cssClass()">
      {{ label() }}
    </span>
  `
})
export class StatusBadgeComponent {
  readonly label = input.required<string>();
  readonly tone = input<StatusTone>('active');

  readonly cssClass = computed(() => `status-${this.tone()}`);
}

