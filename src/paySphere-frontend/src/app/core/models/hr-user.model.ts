import { HrRole, UserStatus } from './enums';

export interface HrUserCreateRequest {
  name: string;
  email: string;
  password: string;
  role: HrRole;
}

export interface HrUserUpdateRequest {
  name: string;
  role: HrRole;
  status: UserStatus;
}

export interface HrUserResponse {
  id: number;
  name: string;
  email: string;
  role: HrRole;
  status: UserStatus;
  createdAt: string;
}
