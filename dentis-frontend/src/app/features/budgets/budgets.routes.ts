import { Routes } from '@angular/router';

export const BUDGETS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./budget-list/budget-list.component').then(m => m.BudgetListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./budget-form/budget-form.component').then(m => m.BudgetFormComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./budget-detail/budget-detail.component').then(m => m.BudgetDetailComponent)
  }
];
