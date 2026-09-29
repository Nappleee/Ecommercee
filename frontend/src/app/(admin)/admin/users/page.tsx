"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Search, Trash2, Users } from "lucide-react";
import { userApi } from "@/lib/api";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import type { ApiResponse, PaginatedResponse, UserResponse } from "@/types";

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-users", page],
    queryFn: async () => {
      const response = await userApi.getAll({ page, size: 10 });
      return response.data as ApiResponse<PaginatedResponse<UserResponse>>;
    },
  });
  const deleteMutation = useMutation({
    mutationFn: (id: number) => userApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
  });
  const users = (data?.data?.content ?? []).filter((user) => {
    const query = search.toLowerCase();
    return !query || user.username.toLowerCase().includes(query) || user.email.toLowerCase().includes(query);
  });
  const totalPages = data?.data?.totalPages ?? 0;

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Người dùng</h1>
        <p className="text-sm text-gray-500 mt-0.5">{data?.data?.totalElements ?? 0} tài khoản</p>
      </div>
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <Input placeholder="Tìm username hoặc email..." value={search} onChange={(event) => setSearch(event.target.value)} leftIcon={<Search className="h-4 w-4" />} className="max-w-sm" />
      </div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isError ? <p className="p-8 text-center text-red-600">Không thể tải danh sách người dùng.</p> : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3">Người dùng</th>
                  <th className="text-left px-4 py-3">Email</th>
                  <th className="text-left px-4 py-3">Vai trò</th>
                  <th className="text-center px-4 py-3">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? <tr><td colSpan={4} className="p-8 text-center text-gray-500">Đang tải...</td></tr> : users.length === 0 ? (
                  <tr><td colSpan={4} className="p-8 text-center text-gray-500"><Users className="mx-auto mb-2 h-10 w-10 text-gray-300" />Không tìm thấy người dùng.</td></tr>
                ) : users.map((item) => (
                  <tr key={item.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="px-4 py-3"><p className="font-medium text-gray-900">{item.fullName}</p><p className="text-xs text-gray-500">@{item.username}</p></td>
                    <td className="px-4 py-3 text-gray-600">{item.email}</td>
                    <td className="px-4 py-3"><div className="flex gap-1">{(item.roles ?? []).map((role) => <Badge key={role.id} variant={role.name.includes("ADMIN") ? "warning" : "info"}>{role.name}</Badge>)}</div></td>
                    <td className="px-4 py-3 text-center"><button className="rounded-lg p-1.5 text-gray-500 hover:bg-red-50 hover:text-red-600" title="Xóa người dùng" onClick={() => window.confirm(`Xóa người dùng ${item.username}?`) && deleteMutation.mutate(item.id)}><Trash2 className="h-4 w-4" /></button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {totalPages > 1 && <div className="flex items-center justify-center gap-3 border-t border-gray-200 p-4"><Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</Button><span className="text-sm text-gray-600">Trang {page + 1}/{totalPages}</span><Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((current) => current + 1)}>Sau</Button></div>}
      </div>
    </div>
  );
}
