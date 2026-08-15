import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { EmployeeService } from '../../../core/services/employee.service';
import { MasterDataService } from '../../../core/services/master-data.service';
import { AuthService } from '../../../core/services/auth.service';
import { EmployeeSummaryResponse } from '../../../core/models/employee.model';
import { EMPLOYEE_STATUSES, EmployeeStatus } from '../../../core/models/enums';
import { CountryResponse, DepartmentResponse, DesignationResponse } from '../../../core/models/master-data.model';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatCardModule
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss'
})
export class EmployeeListComponent implements OnInit {
  readonly displayedColumns = ['employeeCode', 'name', 'email', 'department', 'designation', 'country', 'joiningDate', 'status'];

  readonly loading = signal(true);
  readonly employees = signal<EmployeeSummaryResponse[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);

  readonly countries = signal<CountryResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly designations = signal<DesignationResponse[]>([]);

  readonly statuses: EmployeeStatus[] = EMPLOYEE_STATUSES;
  readonly searchControl = new FormControl('', { nonNullable: true });

  countryId: number | null = null;
  departmentId: number | null = null;
  designationId: number | null = null;
  status: EmployeeStatus | null = null;

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly masterDataService: MasterDataService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  get canEdit(): boolean {
    return this.authService.hasAnyRole('HR_ADMIN', 'HR_MANAGER');
  }

  ngOnInit(): void {
    this.masterDataService.getCountries().subscribe((data) => this.countries.set(data));
    this.masterDataService.getDepartments().subscribe((data) => this.departments.set(data));
    this.masterDataService.getDesignations().subscribe((data) => this.designations.set(data));

    this.searchControl.valueChanges.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });

    this.load();
  }

  onFilterChange(): void {
    this.pageIndex.set(0);
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  openEmployee(id: number): void {
    this.router.navigate(['/employees', id]);
  }

  createEmployee(): void {
    this.router.navigate(['/employees/new']);
  }

  statusClass(status: EmployeeStatus): string {
    return `status-${status.toLowerCase().replace('_', '-')}`;
  }

  private load(): void {
    this.loading.set(true);
    this.employeeService
      .search({
        search: this.searchControl.value || undefined,
        countryId: this.countryId ?? undefined,
        departmentId: this.departmentId ?? undefined,
        designationId: this.designationId ?? undefined,
        status: this.status ?? undefined,
        page: this.pageIndex(),
        size: this.pageSize()
      })
      .subscribe({
        next: (response) => {
          this.employees.set(response.content);
          this.totalElements.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }
}
