"use client";

import { useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { Mail, MapPin, Phone, Shield, User } from "lucide-react";
import { authApi } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";
import type { ApiResponse, UserResponse } from "@/types";

export default function ProfilePage() {
  const router = useRouter();
  const { user, isAuthenticated, hasHydrated } = useAuthStore();

  useEffect(() => {
    if (hasHydrated && !isAuthenticated) {
      router.replace("/login?redirect=/profile");
    }
  }, [hasHydrated, isAuthenticated, router]);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["profile"],
    queryFn: async () => {
      const response = await authApi.getProfile();
      return (response.data as ApiResponse<UserResponse>).data;
    },
    enabled: hasHydrated && isAuthenticated,
  });

  const profile = data ?? user;
  const roles = profile?.roles?.map((role) => role.name).join(", ") || "USER";

  if (!hasHydrated || !isAuthenticated) {
    return null;
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-8">
        <p className="text-sm text-orange-500 font-medium mb-1">Tài khoản</p>
        <h1 className="text-3xl font-bold text-gray-900">Hồ sơ của tôi</h1>
        <p className="text-gray-500 mt-2">Quản lý thông tin cá nhân của bạn.</p>
      </div>

      {isLoading ? (
        <div className="bg-white rounded-2xl border border-gray-200 p-6 animate-pulse">
          <div className="h-20 w-20 rounded-full bg-gray-200 mb-6" />
          <div className="h-6 bg-gray-200 rounded w-1/3 mb-3" />
          <div className="h-4 bg-gray-200 rounded w-1/2" />
        </div>
      ) : isError && !profile ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-red-700">
          Không thể tải thông tin hồ sơ. Vui lòng thử lại sau.
        </div>
      ) : profile ? (
        <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="bg-gradient-to-r from-orange-500 to-orange-400 px-6 py-8 text-white">
            <div className="flex items-center gap-4">
              <div className="h-20 w-20 rounded-full bg-white/20 flex items-center justify-center text-3xl font-bold">
                {profile.fullName?.[0]?.toUpperCase() ?? "U"}
              </div>
              <div>
                <h2 className="text-2xl font-bold">{profile.fullName || profile.username}</h2>
                <p className="text-orange-50">@{profile.username}</p>
              </div>
            </div>
          </div>

          <div className="p-6 grid gap-5 sm:grid-cols-2">
            <ProfileField icon={<User className="h-5 w-5" />} label="Họ và tên" value={profile.fullName} />
            <ProfileField icon={<Mail className="h-5 w-5" />} label="Email" value={profile.email} />
            <ProfileField icon={<Phone className="h-5 w-5" />} label="Số điện thoại" value={profile.phone || "Chưa cập nhật"} />
            <ProfileField icon={<MapPin className="h-5 w-5" />} label="Giới tính" value={profile.gender || "Chưa cập nhật"} />
            <ProfileField icon={<Shield className="h-5 w-5" />} label="Vai trò" value={roles} />
          </div>
        </div>
      ) : null}
    </div>
  );
}

function ProfileField({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value?: string;
}) {
  return (
    <div className="flex items-start gap-3 rounded-xl bg-gray-50 p-4">
      <span className="text-orange-500 mt-0.5">{icon}</span>
      <div>
        <p className="text-xs text-gray-500 mb-1">{label}</p>
        <p className="font-medium text-gray-900">{value || "Chưa cập nhật"}</p>
      </div>
    </div>
  );
}
