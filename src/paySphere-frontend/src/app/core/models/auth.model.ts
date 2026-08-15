import { HrRole } from './enums';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface HrUserSummaryResponse {
  id: number;
  name: string;
  email: string;
  role: HrRole;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: HrUserSummaryResponse;
}
