import { PaymentStatus } from './enums';

export interface SalaryCreateRequest {
  currencyCode: string;
  baseSalary: number;
  bonus: number | null;
  effectiveFrom: string;
}

export interface SalaryResponse {
  id: number;
  employeeId: number;
  currencyCode: string;
  baseSalary: number;
  bonus: number;
  effectiveFrom: string;
  effectiveTo: string | null;
  createdById: number;
  createdByName: string;
  createdAt: string;
  paymentStatus: PaymentStatus;
  paidAt: string | null;
}
