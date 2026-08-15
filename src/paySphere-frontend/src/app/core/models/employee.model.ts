import { EmployeeStatus } from './enums';

export interface EmployeeCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  countryId: number;
  departmentId: number;
  designationId: number;
  joiningDate: string;
}

export type EmployeeUpdateRequest = EmployeeCreateRequest;

export interface EmployeeStatusUpdateRequest {
  status: EmployeeStatus;
}

export interface EmployeeResponse {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  countryId: number;
  countryName: string;
  departmentId: number;
  departmentName: string;
  designationId: number;
  designationName: string;
  joiningDate: string;
  status: EmployeeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeSummaryResponse {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  countryName: string;
  departmentName: string;
  designationName: string;
  joiningDate: string;
  status: EmployeeStatus;
}

export interface BulkUploadRowResult {
  rowNumber: number;
  email: string;
  success: boolean;
  employeeCode: string | null;
  message: string;
}

export interface BulkUploadResponse {
  totalRows: number;
  successCount: number;
  failureCount: number;
  results: BulkUploadRowResult[];
}

export interface EmployeeSearchParams {
  search?: string;
  countryId?: number;
  departmentId?: number;
  designationId?: number;
  status?: EmployeeStatus;
  page?: number;
  size?: number;
  sort?: string;
}
