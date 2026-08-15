import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CountryResponse, DepartmentResponse, DesignationResponse } from '../models/master-data.model';

@Injectable({ providedIn: 'root' })
export class MasterDataService {
  private countries$?: Observable<CountryResponse[]>;
  private departments$?: Observable<DepartmentResponse[]>;
  private designations$?: Observable<DesignationResponse[]>;

  constructor(private readonly http: HttpClient) {}

  getCountries(): Observable<CountryResponse[]> {
    if (!this.countries$) {
      this.countries$ = this.http
        .get<CountryResponse[]>(`${environment.apiUrl}/countries`)
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.countries$;
  }

  getDepartments(): Observable<DepartmentResponse[]> {
    if (!this.departments$) {
      this.departments$ = this.http
        .get<DepartmentResponse[]>(`${environment.apiUrl}/departments`)
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.departments$;
  }

  getDesignations(): Observable<DesignationResponse[]> {
    if (!this.designations$) {
      this.designations$ = this.http
        .get<DesignationResponse[]>(`${environment.apiUrl}/designations`)
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.designations$;
  }
}
