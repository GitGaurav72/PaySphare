import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { EMPLOYEE_STATUSES, EmployeeStatus } from '../../../core/models/enums';

export interface StatusUpdateDialogData {
  currentStatus: EmployeeStatus;
}

@Component({
  selector: 'app-status-update-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Update employee status</h2>
    <mat-dialog-content>
      <mat-form-field class="full-width">
        <mat-label>Status</mat-label>
        <mat-select [(ngModel)]="selectedStatus">
          @for (status of statuses; track status) {
            <mat-option [value]="status">{{ status }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null">Cancel</button>
      <button mat-flat-button color="primary" [mat-dialog-close]="selectedStatus">Update</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .full-width {
        width: 320px;
        max-width: 100%;
      }
    `
  ]
})
export class StatusUpdateDialogComponent {
  readonly statuses = EMPLOYEE_STATUSES;
  selectedStatus: EmployeeStatus;

  constructor(@Inject(MAT_DIALOG_DATA) public data: StatusUpdateDialogData) {
    this.selectedStatus = data.currentStatus;
  }
}
