import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl } from '@angular/forms';
import { EntityAutocompleteComponent, AutocompleteOption } from './entity-autocomplete.component';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';

describe('EntityAutocompleteComponent', () => {
  let component: EntityAutocompleteComponent;
  let fixture: ComponentFixture<EntityAutocompleteComponent>;

  const options: AutocompleteOption[] = [
    { id: '1', name: 'Option A' },
    { id: '2', name: 'Option B' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EntityAutocompleteComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(EntityAutocompleteComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('label', 'Test Label');
    fixture.componentRef.setInput('control', new FormControl(''));
    fixture.componentRef.setInput('options', options);
    fixture.detectChanges();
  });

  it('should create with required inputs', () => {
    expect(component).toBeTruthy();
    expect(component.label()).toBe('Test Label');
  });

  it('should reflect placeholder input', () => {
    fixture.componentRef.setInput('placeholder', 'Search...');
    fixture.detectChanges();
    expect(component.placeholder()).toBe('Search...');
  });

  it('should emit optionSelected when an option is selected', () => {
    const emitted: AutocompleteOption[] = [];
    component.optionSelected.subscribe((v) => emitted.push(v));

    const fakeEvent = { option: { value: options[0] } } as MatAutocompleteSelectedEvent;
    component.handleOptionSelected(fakeEvent);

    expect(emitted.length).toBe(1);
    expect(emitted[0]).toEqual(options[0]);
  });

  it('should use default displayWith for string values', () => {
    const display = component.displayWith();
    expect(display('hello')).toBe('hello');
    expect(display(null)).toBe('');
  });
});
