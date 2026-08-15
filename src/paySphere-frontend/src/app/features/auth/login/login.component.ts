import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ErrorResponse } from '../../../core/models/common.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly hidePassword = signal(true);

  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.getRawValue();

    this.authService
      .login({ email: email!, password: password! })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => this.router.navigate(['/dashboard']),
        error: (error: HttpErrorResponse) => {
          const body = error.error as ErrorResponse | undefined;
          this.errorMessage.set(body?.message ?? 'Unable to sign in. Please try again.');
        }
      });
  }
}
