import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HrRole } from '../models/enums';
import { HrUserSummaryResponse, LoginRequest, LoginResponse } from '../models/auth.model';

const TOKEN_KEY = 'paysphere.accessToken';
const USER_KEY = 'paysphere.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly currentUserSignal = signal<HrUserSummaryResponse | null>(this.readStoredUser());
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);
  readonly role = computed<HrRole | null>(() => this.currentUserSignal()?.role ?? null);

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => {
        localStorage.setItem(TOKEN_KEY, response.accessToken);
        localStorage.setItem(USER_KEY, JSON.stringify(response.user));
        this.currentUserSignal.set(response.user);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUserSignal.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  hasAnyRole(...roles: HrRole[]): boolean {
    const current = this.role();
    return current !== null && roles.includes(current);
  }

  private readStoredUser(): HrUserSummaryResponse | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as HrUserSummaryResponse;
    } catch {
      return null;
    }
  }
}
