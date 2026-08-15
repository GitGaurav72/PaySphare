import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/common.model';
import {
  EmployeeCreateRequest,
  EmployeeResponse,
  EmployeeSearchParams,
  EmployeeStatusUpdateRequest,
  EmployeeSummaryResponse,
  EmployeeUpdateRequest
} from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly baseUrl = `${environment.apiUrl}/employees`;

  constructor(private readonly http: HttpClient) {}

  search(params: EmployeeSearchParams): Observable<PageResponse<EmployeeSummaryResponse>> {
    let httpParams = new HttpParams();
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.countryId != null) httpParams = httpParams.set('countryId', params.countryId);
    if (params.departmentId != null) httpParams = httpParams.set('departmentId', params.departmentId);
    if (params.designationId != null) httpParams = httpParams.set('designationId', params.designationId);
    if (params.status) httpParams = httpParams.set('status', params.status);
    httpParams = httpParams.set('page', params.page ?? 0);
    httpParams = httpParams.set('size', params.size ?? 20);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<PageResponse<EmployeeSummaryResponse>>(this.baseUrl, { params: httpParams });
  }

  getById(id: number): Observable<EmployeeResponse> {
    return this.http.get<EmployeeResponse>(`${this.baseUrl}/${id}`);
  }

  create(request: EmployeeCreateRequest): Observable<EmployeeResponse> {
    return this.http.post<EmployeeResponse>(this.baseUrl, request);
  }

  update(id: number, request: EmployeeUpdateRequest): Observable<EmployeeResponse> {
    return this.http.put<EmployeeResponse>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: number, request: EmployeeStatusUpdateRequest): Observable<EmployeeResponse> {
    return this.http.patch<EmployeeResponse>(`${this.baseUrl}/${id}/status`, request);
  }
}
