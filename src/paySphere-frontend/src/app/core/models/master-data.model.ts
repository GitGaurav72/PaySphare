export interface CountryResponse {
  id: number;
  name: string;
  countryCode: string;
  currencyCode: string;
}

export interface DepartmentResponse {
  id: number;
  name: string;
  description: string | null;
}

export interface DesignationResponse {
  id: number;
  name: string;
}
