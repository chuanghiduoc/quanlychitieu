export interface UserResponse {
  uid: string;
  email: string | null;
  displayName: string | null;
  disabled: boolean;
}

export interface UsersApiResponse {
  users: UserResponse[];
}

export interface ApiError {
  error: string;
}