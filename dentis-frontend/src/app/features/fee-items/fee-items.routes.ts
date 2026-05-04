import { Routes } from '@angular/router';

export const FEE_ITEMS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./fee-item-list/fee-item-list.component').then(m => m.FeeItemListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./fee-item-form/fee-item-form.component').then(m => m.FeeItemFormComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./fee-item-form/fee-item-form.component').then(m => m.FeeItemFormComponent)
  }
];
