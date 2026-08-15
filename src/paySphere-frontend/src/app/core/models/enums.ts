export type EmployeeStatus = 'ACTIVE' | 'INACTIVE' | 'ON_LEAVE' | 'TERMINATED';

export const EMPLOYEE_STATUSES: EmployeeStatus[] = ['ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED'];

export type HrRole = 'HR_ADMIN' | 'HR_MANAGER' | 'HR_VIEWER';

export const HR_ROLES: HrRole[] = ['HR_ADMIN', 'HR_MANAGER', 'HR_VIEWER'];

export type UserStatus = 'ACTIVE' | 'INACTIVE';

export const USER_STATUSES: UserStatus[] = ['ACTIVE', 'INACTIVE'];

export type PaymentStatus = 'PENDING' | 'PAID';
