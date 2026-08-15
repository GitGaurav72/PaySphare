export interface DashboardSummaryResponse {
  totalEmployees: number;
  activeEmployees: number;
  inactiveEmployees: number;
  onLeaveEmployees: number;
  terminatedEmployees: number;
  primaryCurrencyCode: string;
  averageTotalCompensation: number;
  highestTotalCompensation: number;
  lowestTotalCompensation: number;
}

export interface DepartmentSalaryResponse {
  departmentName: string;
  currencyCode: string;
  averageTotalCompensation: number;
  employeeCount: number;
}

export interface CountrySalaryResponse {
  countryName: string;
  currencyCode: string;
  averageTotalCompensation: number;
  minTotalCompensation: number;
  maxTotalCompensation: number;
  employeeCount: number;
}

export interface DepartmentCountResponse {
  departmentName: string;
  employeeCount: number;
}

export interface SalaryDistributionBucket {
  rangeStart: number;
  rangeEnd: number;
  employeeCount: number;
}

export interface SalaryDistributionResponse {
  currencyCode: string;
  buckets: SalaryDistributionBucket[];
}

export interface TopPaidEmployeeResponse {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  departmentName: string;
  countryName: string;
  currencyCode: string;
  totalCompensation: number;
}
