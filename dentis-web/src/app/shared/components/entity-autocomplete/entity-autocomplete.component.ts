import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

/** Minimal shape required for autocomplete options. */
export type AutocompleteOption = object | string;

@Component({
  selector: 'app-entity-autocomplete',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule
  ],
  template: `
    <mat-form-field appearance="outline">
      <mat-label>{{ label() }}</mat-label>
      <input
        matInput
        [formControl]="control()"
        [matAutocomplete]="autocomplete"
        [placeholder]="placeholder()"
        [disabled]="disabled()"
        autocomplete="off" />

      @if (suffixIcon()) {
        <mat-icon matSuffix>{{ suffixIcon() }}</mat-icon>
      }

      <mat-autocomplete
        #autocomplete="matAutocomplete"
        [displayWith]="displayWith()"
        (optionSelected)="handleOptionSelected($event)">
        @for (option of options(); track trackByValue()(option)) {
          <mat-option [value]="option">
            {{ displayWith()(option) }}
          </mat-option>
        }

        @if (options().length === 0 && showEmptyState()) {
          <mat-option disabled>{{ emptyMessage() }}</mat-option>
        }
      </mat-autocomplete>

      <mat-error>{{ errorMessage() }}</mat-error>
    </mat-form-field>
  `
})
export class EntityAutocompleteComponent {
  readonly label = input.required<string>();
  readonly placeholder = input<string>('');
  readonly suffixIcon = input<string>('');
  readonly emptyMessage = input<string>('No results found');
  readonly errorMessage = input<string>('Please select an option from the list');
  readonly disabled = input<boolean>(false);
  readonly showEmptyState = input<boolean>(false);
  // FormControl<unknown> covers all entity/string union types used by callers
  readonly control = input.required<FormControl<unknown>>();
  readonly options = input<AutocompleteOption[]>([]);
  readonly displayWith = input<(value: AutocompleteOption | null) => string>(
    (value) => (typeof value === 'string' ? value : '')
  );
  readonly trackByValue = input<(value: AutocompleteOption) => string | number>(
    (value) => JSON.stringify(value)
  );
  readonly optionSelected = output<AutocompleteOption>();

  handleOptionSelected(event: MatAutocompleteSelectedEvent): void {
    this.optionSelected.emit(event.option.value as AutocompleteOption);
  }
}

