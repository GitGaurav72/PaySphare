import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'employees',
        loadComponent: () =>
          import('./features/employees/employee-list/employee-list.component').then((m) => m.EmployeeListComponent)
      },
      {
        path: 'employees/new',
        loadComponent: () =>
          import('./features/employees/employee-form/employee-form.component').then((m) => m.EmployeeFormComponent),
        canActivate: [roleGuard],
        data: { roles: ['HR_ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'employees/:id',
        loadComponent: () =>
          import('./features/employees/employee-detail/employee-detail.component').then((m) => m.EmployeeDetailComponent)
      },
      {
        path: 'employees/:id/edit',
        loadComponent: () =>
          import('./features/employees/employee-form/employee-form.component').then((m) => m.EmployeeFormComponent),
        canActivate: [roleGuard],
        data: { roles: ['HR_ADMIN', 'HR_MANAGER'] }
      },
      {
        path: 'hr-users',
        loadComponent: () =>
          import('./features/hr-users/hr-user-list/hr-user-list.component').then((m) => m.HrUserListComponent),
        canActivate: [roleGuard],
        data: { roles: ['HR_ADMIN'] }
      },
      {
        path: 'forbidden',
        loadComponent: () => import('./features/misc/forbidden/forbidden.component').then((m) => m.ForbiddenComponent)
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./features/misc/not-found/not-found.component').then((m) => m.NotFoundComponent)
  }
];
