import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HrUserService } from '../../../core/services/hr-user.service';
import { AuthService } from '../../../core/services/auth.service';
import { HrUserResponse } from '../../../core/models/hr-user.model';
import { HrUserFormDialogComponent } from '../hr-user-form/hr-user-form-dialog.component';

@Component({
  selector: 'app-hr-user-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './hr-user-list.component.html',
  styleUrl: './hr-user-list.component.scss'
})
export class HrUserListComponent implements OnInit {
  readonly loading = signal(true);
  readonly users = signal<HrUserResponse[]>([]);
  readonly displayedColumns = ['name', 'email', 'role', 'status', 'createdAt', 'actions'];

  constructor(
    private readonly hrUserService: HrUserService,
    private readonly authService: AuthService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar
  ) {}

  get currentUserId(): number | undefined {
    return this.authService.currentUser()?.id;
  }

  ngOnInit(): void {
    this.load();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(HrUserFormDialogComponent, { data: { user: null } });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('HR user created', 'Dismiss', { duration: 3000 });
        this.load();
      }
    });
  }

  openEditDialog(user: HrUserResponse): void {
    const dialogRef = this.dialog.open(HrUserFormDialogComponent, { data: { user } });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.snackBar.open('HR user updated', 'Dismiss', { duration: 3000 });
        this.load();
      }
    });
  }

  statusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  private load(): void {
    this.loading.set(true);
    this.hrUserService.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }
}
