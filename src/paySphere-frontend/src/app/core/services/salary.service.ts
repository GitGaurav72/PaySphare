import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SalaryCreateRequest, SalaryResponse } from '../models/salary.model';
import { SKIP_ERROR_TOAST } from '../interceptors/silent-request.token';

@Injectable({ providedIn: 'root' })
export class SalaryService {
  private readonly baseUrl = `${environment.apiUrl}/employees`;

  constructor(private readonly http: HttpClient) {}

  getCurrent(employeeId: number): Observable<SalaryResponse> {
    // A brand-new employee has no salary yet; the backend 404s, which is an
    // expected state here, not an error worth surfacing as a toast.
    return this.http.get<SalaryResponse>(`${this.baseUrl}/${employeeId}/salary`, {
      context: new HttpContext().set(SKIP_ERROR_TOAST, true)
    });
  }

  getHistory(employeeId: number): Observable<SalaryResponse[]> {
    return this.http.get<SalaryResponse[]>(`${this.baseUrl}/${employeeId}/salary-history`);
  }

  create(employeeId: number, request: SalaryCreateRequest): Observable<SalaryResponse> {
    return this.http.post<SalaryResponse>(`${this.baseUrl}/${employeeId}/salary`, request);
  }
}
