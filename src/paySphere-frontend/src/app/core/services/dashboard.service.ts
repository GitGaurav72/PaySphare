import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CountrySalaryResponse,
  DashboardSummaryResponse,
  DepartmentCountResponse,
  DepartmentSalaryResponse,
  SalaryDistributionResponse,
  TopPaidEmployeeResponse
} from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  constructor(private readonly http: HttpClient) {}

  getSummary(): Observable<DashboardSummaryResponse> {
    return this.http.get<DashboardSummaryResponse>(`${this.baseUrl}/summary`);
  }

  getSalaryByDepartment(): Observable<DepartmentSalaryResponse[]> {
    return this.http.get<DepartmentSalaryResponse[]>(`${this.baseUrl}/salary-by-department`);
  }

  getSalaryByCountry(): Observable<CountrySalaryResponse[]> {
    return this.http.get<CountrySalaryResponse[]>(`${this.baseUrl}/salary-by-country`);
  }

  getEmployeeCountByDepartment(): Observable<DepartmentCountResponse[]> {
    return this.http.get<DepartmentCountResponse[]>(`${this.baseUrl}/employee-count-by-department`);
  }

  getSalaryDistribution(currency?: string): Observable<SalaryDistributionResponse[]> {
    let params = new HttpParams();
    if (currency) params = params.set('currency', currency);
    return this.http.get<SalaryDistributionResponse[]>(`${this.baseUrl}/salary-distribution`, { params });
  }

  getTopPaidEmployees(limit = 10): Observable<TopPaidEmployeeResponse[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<TopPaidEmployeeResponse[]>(`${this.baseUrl}/top-paid-employees`, { params });
  }
}
