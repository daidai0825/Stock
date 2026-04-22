import { postJson } from './http';
import type {
  LoginRequest,
  LoginResponse,
  MemberProfile,
  RegisterRequest,
  RegisterResponse,
  UpdateProfileRequest,
} from '@/types/member';

/**
 * M-MEMBER service（Wave A）
 * 對應 SRS §1.2 F-MEMBER-01 ~ F-MEMBER-08
 * 後端路徑：/api/v1/member/{action}（全 POST，Envelope）
 */

export const memberService = {
  login(req: LoginRequest): Promise<LoginResponse> {
    return postJson<LoginRequest, LoginResponse>('/api/v1/member/login', req);
  },

  register(req: RegisterRequest): Promise<RegisterResponse> {
    return postJson<RegisterRequest, RegisterResponse>('/api/v1/member/register', req);
  },

  logout(): Promise<void> {
    return postJson<Record<string, never>, void>('/api/v1/member/logout', {});
  },

  getProfile(): Promise<MemberProfile> {
    return postJson<Record<string, never>, MemberProfile>('/api/v1/member/profile/get', {});
  },

  updateProfile(req: UpdateProfileRequest): Promise<MemberProfile> {
    return postJson<UpdateProfileRequest, MemberProfile>('/api/v1/member/profile/update', req);
  },
};
