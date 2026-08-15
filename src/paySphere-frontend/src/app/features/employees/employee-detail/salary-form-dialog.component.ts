import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { SalaryService } from '../../../core/services/salary.service';
import { SalaryResponse } from '../../../core/models/salary.model';
import { ErrorResponse } from '../../../core/models/common.model';
import { toIsoDate } from '../../../shared/utils/date.util';

export interface SalaryFormDialogData {
  employeeId: number;
  defaultCurrencyCode: string;
}

@Component({
  selector: 'app-salary-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './salary-form-dialog.component.html',
  styleUrl: './salary-form-dialog.component.scss'
})
export class SalaryFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<SalaryFormDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    currencyCode: [this.data.defaultCurrencyCode ?? '', [Validators.required, Validators.minLength(3), Validators.maxLength(10)]],
    baseSalary: [null as number | null, [Validators.required, Validators.min(0)]],
    bonus: [null as number | null, [Validators.min(0)]],
    effectiveFrom: [new Date(), [Validators.required]]
  });

  constructor(
    private readonly salaryService: SalaryService,
    private readonly dialogRef: MatDialogRef<SalaryFormDialogComponent, SalaryResponse>
  ) {}

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);

    this.salaryService
      .create(this.data.employeeId, {
        currencyCode: raw.currencyCode!,
        baseSalary: raw.baseSalary!,
        bonus: raw.bonus,
        effectiveFrom: toIsoDate(raw.effectiveFrom)!
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (salary) => this.dialogRef.close(salary),
        error: (error: HttpErrorResponse) => {
          const body = error.error as ErrorResponse | undefined;
          this.errorMessage.set(body?.message ?? 'Unable to save salary change.');
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
