import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { EmployeeService } from '../../../core/services/employee.service';
import { BulkUploadResponse, BulkUploadRowResult } from '../../../core/models/employee.model';
import { ErrorResponse } from '../../../core/models/common.model';
import { downloadBlob } from '../../../shared/utils/download.util';

@Component({
  selector: 'app-employee-bulk-upload-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ],
  templateUrl: './employee-bulk-upload-dialog.component.html',
  styleUrl: './employee-bulk-upload-dialog.component.scss'
})
export class EmployeeBulkUploadDialogComponent {
  readonly selectedFile = signal<File | null>(null);
  readonly uploading = signal(false);
  readonly downloadingTemplate = signal(false);
  readonly result = signal<BulkUploadResponse | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly resultColumns = ['row', 'email', 'message'];

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly dialogRef: MatDialogRef<EmployeeBulkUploadDialogComponent, boolean>
  ) {}

  downloadTemplate(): void {
    this.downloadingTemplate.set(true);
    this.employeeService
      .downloadBulkTemplate()
      .pipe(finalize(() => this.downloadingTemplate.set(false)))
      .subscribe({
        next: (blob) => downloadBlob(blob, 'employee-bulk-upload-template.xlsx')
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
    this.result.set(null);
    this.errorMessage.set(null);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) return;

    this.uploading.set(true);
    this.errorMessage.set(null);
    this.result.set(null);

    this.employeeService
      .bulkUpload(file)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: (res) => this.result.set(res),
        error: (error: HttpErrorResponse) => {
          const body = error.error as ErrorResponse | undefined;
          this.errorMessage.set(body?.message ?? 'Upload failed. Please try again.');
        }
      });
  }

  failedRows(res: BulkUploadResponse): BulkUploadRowResult[] {
    return res.results.filter((r) => !r.success);
  }

  close(): void {
    const res = this.result();
    this.dialogRef.close(res ? res.successCount > 0 : false);
  }
}
