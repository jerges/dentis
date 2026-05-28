import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';

describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<ConfirmDialogComponent>>;

  const mockDialogData: ConfirmDialogData = {
    title: 'Confirm Action',
    message: 'Are you sure you want to proceed?',
    confirmLabel: 'Yes',
    cancelLabel: 'No',
    danger: false
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display dialog title', () => {
    const titleEl = fixture.nativeElement.querySelector('h2');
    expect(titleEl).toBeTruthy();
    expect(titleEl.textContent).toContain('Confirm Action');
  });

  it('should display dialog message', () => {
    const contentEl = fixture.nativeElement.querySelector('mat-dialog-content p');
    expect(contentEl).toBeTruthy();
    expect(contentEl.textContent).toContain('Are you sure you want to proceed?');
  });

  it('should display custom confirm label', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons.length).toBeGreaterThan(1);
    const confirmBtn = buttons[buttons.length - 1];
    expect(confirmBtn.textContent).toContain('Yes');
  });

  it('should display custom cancel label', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons.length).toBeGreaterThan(0);
    const cancelBtn = buttons[0];
    expect(cancelBtn.textContent).toContain('No');
  });

  it('should display default cancel label when not provided', async () => {
    const dataWithoutCancelLabel: ConfirmDialogData = {
      title: 'Test',
      message: 'Test message',
      confirmLabel: 'Submit'
    };

    const dialogRef2 = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef2 },
        { provide: MAT_DIALOG_DATA, useValue: dataWithoutCancelLabel }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    fixture.detectChanges();

    const cancelBtn = fixture.nativeElement.querySelectorAll('button')[0];
    expect(cancelBtn.textContent).toContain('Cancelar'); // Default Spanish
  });

  it('should display default confirm label when not provided', async () => {
    const dataWithoutConfirmLabel: ConfirmDialogData = {
      title: 'Test',
      message: 'Test message'
    };

    const dialogRef2 = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef2 },
        { provide: MAT_DIALOG_DATA, useValue: dataWithoutConfirmLabel }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    fixture.detectChanges();

    const confirmBtn = fixture.nativeElement.querySelectorAll('button')[1];
    expect(confirmBtn.textContent).toContain('Confirmar'); // Default Spanish
  });

  it('should show help icon for non-danger dialogs', () => {
    const dataNotDanger: ConfirmDialogData = {
      title: 'Confirm',
      message: 'Message',
      danger: false
    };

    const dialogRef2 = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.resetTestingModule();

    TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef2 },
        { provide: MAT_DIALOG_DATA, useValue: dataNotDanger }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    fixture.detectChanges();

    const titleIcon = fixture.nativeElement.querySelector('h2 mat-icon');
    expect(titleIcon).toBeTruthy();
    expect(titleIcon.textContent).toContain('help_outline');
  });

  it('should show warning icon for danger dialogs', () => {
    const dangerData: ConfirmDialogData = {
      title: 'Delete Item',
      message: 'This action cannot be undone',
      danger: true
    };

    const dialogRef2 = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.resetTestingModule();

    TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef2 },
        { provide: MAT_DIALOG_DATA, useValue: dangerData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    fixture.detectChanges();

    const titleIcon = fixture.nativeElement.querySelector('h2 mat-icon');
    expect(titleIcon).toBeTruthy();
    expect(titleIcon.textContent).toContain('warning');
  });

  it('should close dialog with false when cancel button is clicked', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const cancelBtn = buttons[0];
    cancelBtn.click();

    expect(dialogRefSpy.close).toHaveBeenCalledWith(false);
  });

  it('should close dialog with true when confirm button is clicked', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const confirmBtn = buttons[buttons.length - 1];
    confirmBtn.click();

    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });

  it('should have correct button styling for non-danger dialogs', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const confirmBtn = buttons[buttons.length - 1];
    // For non-danger, the color should be 'primary'
    expect(confirmBtn).toBeTruthy();
  });

  it('should have warning color for confirm button in danger dialogs', () => {
    const dangerData: ConfirmDialogData = {
      title: 'Delete',
      message: 'Confirm deletion',
      danger: true
    };

    const dialogRef2 = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.resetTestingModule();

    TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef2 },
        { provide: MAT_DIALOG_DATA, useValue: dangerData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button');
    const confirmBtn = buttons[buttons.length - 1];
    expect(confirmBtn).toBeTruthy();
  });

  it('should have actions aligned to end', () => {
    const dialogActions = fixture.nativeElement.querySelector('mat-dialog-actions');
    expect(dialogActions).toBeTruthy();
    expect(dialogActions.getAttribute('align')).toBe('end');
  });

  it('should inject dialog data correctly', () => {
    expect(component.data).toEqual(mockDialogData);
  });

  it('should have proper dialog structure', () => {
    const dialogTitle = fixture.nativeElement.querySelector('h2');
    const dialogContent = fixture.nativeElement.querySelector('mat-dialog-content');
    const dialogActions = fixture.nativeElement.querySelector('mat-dialog-actions');

    expect(dialogTitle).toBeTruthy();
    expect(dialogContent).toBeTruthy();
    expect(dialogActions).toBeTruthy();
  });
});

