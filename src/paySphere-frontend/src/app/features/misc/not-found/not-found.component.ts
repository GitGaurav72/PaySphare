import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="state-page">
      <mat-icon class="state-icon">search_off</mat-icon>
      <h1>Page not found</h1>
      <p>The page you're looking for doesn't exist.</p>
      <a mat-flat-button color="primary" routerLink="/dashboard">Back to dashboard</a>
    </div>
  `,
  styleUrl: '../misc-state.scss'
})
export class NotFoundComponent {}
