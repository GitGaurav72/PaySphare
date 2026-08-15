import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs';
import { EmployeeService } from '../../../core/services/employee.service';
import { MasterDataService } from '../../../core/services/master-data.service';
import { CountryResponse, DepartmentResponse, DesignationResponse } from '../../../core/models/master-data.model';
import { ErrorResponse } from '../../../core/models/common.model';
import { toIsoDate } from '../../../shared/utils/date.util';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.scss'
})
export class EmployeeFormComponent implements OnInit {
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly isEditMode = signal(false);
  readonly employeeId = signal<number | null>(null);
  readonly employeeCode = signal<string | null>(null);

  readonly countries = signal<CountryResponse[]>([]);
  readonly departments = signal<DepartmentResponse[]>([]);
  readonly designations = signal<DesignationResponse[]>([]);

  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    countryId: [null as number | null, [Validators.required]],
    departmentId: [null as number | null, [Validators.required]],
    designationId: [null as number | null, [Validators.required]],
    joiningDate: [null as Date | null, [Validators.required]]
  });

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly masterDataService: MasterDataService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.masterDataService.getCountries().subscribe((data) => this.countries.set(data));
    this.masterDataService.getDepartments().subscribe((data) => this.departments.set(data));
    this.masterDataService.getDesignations().subscribe((data) => this.designations.set(data));

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.employeeId.set(id);
      this.isEditMode.set(true);
      this.loadEmployee(id);
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request = {
      firstName: raw.firstName!,
      lastName: raw.lastName!,
      email: raw.email!,
      countryId: raw.countryId!,
      departmentId: raw.departmentId!,
      designationId: raw.designationId!,
      joiningDate: toIsoDate(raw.joiningDate)!
    };

    this.saving.set(true);

    const request$ = this.isEditMode()
      ? this.employeeService.update(this.employeeId()!, request)
      : this.employeeService.create(request);

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (employee) => {
        this.snackBar.open(this.isEditMode() ? 'Employee updated' : 'Employee created', 'Dismiss', { duration: 3000 });
        this.router.navigate(['/employees', employee.id]);
      },
      error: (error: HttpErrorResponse) => this.applyFieldErrors(error)
    });
  }

  cancel(): void {
    if (this.isEditMode()) {
      this.router.navigate(['/employees', this.employeeId()]);
    } else {
      this.router.navigate(['/employees']);
    }
  }

  private loadEmployee(id: number): void {
    this.loading.set(true);
    this.employeeService.getById(id).subscribe({
      next: (employee) => {
        this.employeeCode.set(employee.employeeCode);
        this.form.patchValue({
          firstName: employee.firstName,
          lastName: employee.lastName,
          email: employee.email,
          countryId: employee.countryId,
          departmentId: employee.departmentId,
          designationId: employee.designationId,
          joiningDate: new Date(employee.joiningDate)
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/employees']);
      }
    });
  }

  private applyFieldErrors(error: HttpErrorResponse): void {
    const body = error.error as ErrorResponse | undefined;
    if (body?.fieldErrors?.length) {
      for (const fieldError of body.fieldErrors) {
        const control = this.form.get(fieldError.field);
        if (control) {
          control.setErrors({ server: fieldError.message });
        }
      }
    }
  }
}
