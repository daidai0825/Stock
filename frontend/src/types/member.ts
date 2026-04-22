/**
 * 會員模組型別（M-MEMBER）
 * 對應 SRS §1.2 / §2.1 USER_INFO
 */

export type MemberStatus = 'UNVERIFIED' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

export interface MemberProfile {
  userId: string;
  email: string;
  displayName: string;
  status: MemberStatus;
  emailVerifiedAt: string | null;
  notifyEmailEnabled: boolean;
  notifyWebEnabled: boolean;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: MemberProfile;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface RegisterResponse {
  userId: string;
  email: string;
  status: MemberStatus;
}

export interface UpdateProfileRequest {
  displayName: string;
}
