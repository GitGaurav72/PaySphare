import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, forkJoin, of } from 'rxjs';
import { EmployeeService } from '../../../core/services/employee.service';
import { SalaryService } from '../../../core/services/salary.service';
import { AuthService } from '../../../core/services/auth.service';
import { EmployeeResponse } from '../../../core/models/employee.model';
import { SalaryResponse } from '../../../core/models/salary.model';
import { EmployeeStatus } from '../../../core/models/enums';
import { MoneyPipe } from '../../../shared/pipes/money.pipe';
import { StatusUpdateDialogComponent } from './status-update-dialog.component';
import { SalaryFormDialogComponent } from './salary-form-dialog.component';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MoneyPipe
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss'
})
export class EmployeeDetailComponent implements OnInit {
  readonly loading = signal(true);
  readonly employee = signal<EmployeeResponse | null>(null);
  readonly currentSalary = signal<SalaryResponse | null>(null);
  readonly salaryHistory = signal<SalaryResponse[]>([]);

  readonly historyColumns = ['effectiveFrom', 'effectiveTo', 'baseSalary', 'bonus', 'total', 'createdByName'];

  private employeeId!: number;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly employeeService: EmployeeService,
    private readonly salaryService: SalaryService,
    private readonly authService: AuthService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar
  ) {}

  get canEdit(): boolean {
    return this.authService.hasAnyRole('HR_ADMIN', 'HR_MANAGER');
  }

  ngOnInit(): void {
    this.employeeId = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  editEmployee(): void {
    this.router.navigate(['/employees', this.employeeId, 'edit']);
  }

  changeStatus(): void {
    const employee = this.employee();
    if (!employee) return;

    const dialogRef = this.dialog.open(StatusUpdateDialogComponent, {
      data: { currentStatus: employee.status }
    });

    dialogRef.afterClosed().subscribe((newStatus: EmployeeStatus | null) => {
      if (!newStatus || newStatus === employee.status) return;

      this.employeeService.updateStatus(this.employeeId, { status: newStatus }).subscribe({
        next: (updated) => {
          this.employee.set(updated);
          this.snackBar.open('Employee status updated', 'Dismiss', { duration: 3000 });
        }
      });
    });
  }

  addSalaryChange(): void {
    const dialogRef = this.dialog.open(SalaryFormDialogComponent, {
      data: {
        employeeId: this.employeeId,
        defaultCurrencyCode: this.currentSalary()?.currencyCode ?? ''
      }
    });

    dialogRef.afterClosed().subscribe((result: SalaryResponse | undefined) => {
      if (result) {
        this.snackBar.open('Salary change recorded', 'Dismiss', { duration: 3000 });
        this.loadSalaryData();
      }
    });
  }

  statusClass(status: EmployeeStatus): string {
    return `status-${status.toLowerCase().replace('_', '-')}`;
  }

  total(salary: SalaryResponse): number {
    return salary.baseSalary + (salary.bonus ?? 0);
  }

  private load(): void {
    this.loading.set(true);
    this.employeeService.getById(this.employeeId).subscribe({
      next: (employee) => {
        this.employee.set(employee);
        this.loadSalaryData();
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/employees']);
      }
    });
  }

  private loadSalaryData(): void {
    forkJoin({
      current: this.salaryService.getCurrent(this.employeeId).pipe(
        catchError((error: HttpErrorResponse) => (error.status === 404 ? of(null) : (() => { throw error; })()))
      ),
      history: this.salaryService.getHistory(this.employeeId)
    }).subscribe({
      next: ({ current, history }) => {
        this.currentSalary.set(current);
        this.salaryHistory.set(history);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
