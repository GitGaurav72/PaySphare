import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { HrUserService } from '../../../core/services/hr-user.service';
import { HR_ROLES, USER_STATUSES } from '../../../core/models/enums';
import { HrUserResponse } from '../../../core/models/hr-user.model';
import { ErrorResponse } from '../../../core/models/common.model';

export interface HrUserFormDialogData {
  user: HrUserResponse | null;
}

@Component({
  selector: 'app-hr-user-form-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './hr-user-form-dialog.component.html',
  styleUrl: './hr-user-form-dialog.component.scss'
})
export class HrUserFormDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly data = inject<HrUserFormDialogData>(MAT_DIALOG_DATA);

  readonly roles = HR_ROLES;
  readonly statuses = USER_STATUSES;
  readonly isEditMode = !!this.data.user;
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    name: [this.data.user?.name ?? '', [Validators.required, Validators.maxLength(150)]],
    email: [
      { value: this.data.user?.email ?? '', disabled: this.isEditMode },
      [Validators.required, Validators.email]
    ],
    password: ['', this.isEditMode ? [] : [Validators.required, Validators.minLength(8)]],
    role: [this.data.user?.role ?? 'HR_VIEWER', [Validators.required]],
    status: [this.data.user?.status ?? 'ACTIVE', [Validators.required]]
  });

  constructor(
    private readonly hrUserService: HrUserService,
    private readonly dialogRef: MatDialogRef<HrUserFormDialogComponent, HrUserResponse>
  ) {}

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.errorMessage.set(null);

    const request$ = this.isEditMode
      ? this.hrUserService.update(this.data.user!.id, {
          name: raw.name!,
          role: raw.role!,
          status: raw.status!
        })
      : this.hrUserService.create({
          name: raw.name!,
          email: raw.email!,
          password: raw.password!,
          role: raw.role!
        });

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (user) => this.dialogRef.close(user),
      error: (error: HttpErrorResponse) => {
        const body = error.error as ErrorResponse | undefined;
        this.errorMessage.set(body?.message ?? 'Unable to save HR user.');
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
