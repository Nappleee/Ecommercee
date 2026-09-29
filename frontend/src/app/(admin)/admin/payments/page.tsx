"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { CreditCard } from "lucide-react";
import { paymentApi } from "@/lib/api";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import type { PaginatedResponse, Payment } from "@/types";

export default function AdminPaymentsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-payments", page],
    queryFn: async () => (await paymentApi.getAllPaged({ page, size: 10 })).data as PaginatedResponse<Payment>,
  });
  const payments = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const variant = (status: Payment["paymentStatus"]) => status === "COMPLETED" ? "success" : status === "FAILED" ? "danger" : "warning";

  return (
    <div className="space-y-5">
      <div><h1 className="text-2xl font-bold text-gray-900">Thanh toán</h1><p className="text-sm text-gray-500 mt-0.5">{data?.totalElements ?? 0} giao dịch</p></div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? <p className="p-10 text-center text-gray-500">Đang tải...</p> : isError ? <p className="p-10 text-center text-red-600">Không thể tải giao dịch thanh toán.</p> : (
          <div className="overflow-x-auto"><table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200"><tr><th className="text-left px-4 py-3">Mã thanh toán</th><th className="text-left px-4 py-3">Mã đơn hàng</th><th className="text-left px-4 py-3">Trạng thái</th><th className="text-center px-4 py-3">Đã thanh toán</th></tr></thead>
            <tbody>{payments.length === 0 ? <tr><td colSpan={4} className="p-10 text-center text-gray-500"><CreditCard className="mx-auto mb-2 h-10 w-10 text-gray-300" />Chưa có giao dịch.</td></tr> : payments.map((payment) => <tr key={payment.paymentId} className="border-b border-gray-100"><td className="px-4 py-3 font-medium">#{payment.paymentId}</td><td className="px-4 py-3">#{payment.orderId}</td><td className="px-4 py-3"><Badge variant={variant(payment.paymentStatus)}>{payment.paymentStatus}</Badge></td><td className="px-4 py-3 text-center">{payment.isPayed ? "Có" : "Chưa"}</td></tr>)}</tbody>
          </table></div>
        )}
        {totalPages > 1 && <div className="flex items-center justify-center gap-3 border-t border-gray-200 p-4"><Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Trước</Button><span className="text-sm text-gray-600">Trang {page + 1}/{totalPages}</span><Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => setPage((current) => current + 1)}>Sau</Button></div>}
      </div>
    </div>
  );
}
