import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { HrUserCreateRequest, HrUserResponse, HrUserUpdateRequest } from '../models/hr-user.model';

@Injectable({ providedIn: 'root' })
export class HrUserService {
  private readonly baseUrl = `${environment.apiUrl}/hr-users`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<HrUserResponse[]> {
    return this.http.get<HrUserResponse[]>(this.baseUrl);
  }

  create(request: HrUserCreateRequest): Observable<HrUserResponse> {
    return this.http.post<HrUserResponse>(this.baseUrl, request);
  }

  update(id: number, request: HrUserUpdateRequest): Observable<HrUserResponse> {
    return this.http.put<HrUserResponse>(`${this.baseUrl}/${id}`, request);
  }
}
