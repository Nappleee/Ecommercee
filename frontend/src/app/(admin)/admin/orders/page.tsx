"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ShoppingBag, Search, Eye, Clock, XCircle } from "lucide-react";
import { orderApi } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { formatPrice, formatDate } from "@/lib/utils";
import type { Order, PaginatedResponse } from "@/types";
import { useAuthStore } from "@/store/authStore";

export default function AdminOrdersPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const { hasHydrated, isAuthenticated } = useAuthStore();

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["admin-orders-list", page],
    queryFn: async () => {
      const res = await orderApi.getAllPaged({ page, size: 10 });
      return res.data as PaginatedResponse<Order>;
    },
    enabled: hasHydrated && isAuthenticated,
    retry: false,
  });

  const orders = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const { data: selectedOrder, isLoading: isOrderLoading, isError: isOrderError } = useQuery({
    queryKey: ["admin-order", selectedOrderId],
    queryFn: async () => (await orderApi.getById(selectedOrderId as number)).data as Order,
    enabled: selectedOrderId !== null,
  });

  const filtered = orders.filter(
    (o) => !search || String(o.orderId).includes(search)
  );
  const cancelOrder = async (orderId: number) => {
    if (!window.confirm(`Hủy đơn hàng #${orderId} và hoàn lại tồn kho?`)) return;
    setCancellingId(orderId);
    try {
      await orderApi.cancel(orderId);
      await refetch();
      if (selectedOrderId === orderId) setSelectedOrderId(null);
    } catch {
      window.alert("Không thể hủy đơn hàng. Đơn có thể đã hoàn thành.");
    } finally {
      setCancellingId(null);
    }
  };

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Đơn hàng</h1>
        <p className="text-sm text-gray-500 mt-0.5">{data?.totalElements ?? 0} đơn hàng</p>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <Input
          placeholder="Tìm theo mã đơn hàng..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          leftIcon={<Search className="h-4 w-4" />}
          className="max-w-sm"
        />
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="text-left px-4 py-3 font-semibold text-gray-700">Mã đơn</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-700">Người đặt</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-700">Ngày đặt</th>
                <th className="text-left px-4 py-3 font-semibold text-gray-700">Mô tả</th>
                <th className="text-right px-4 py-3 font-semibold text-gray-700">SL / Tổng tiền</th>
                <th className="text-center px-4 py-3 font-semibold text-gray-700">Trạng thái</th>
                <th className="text-center px-4 py-3 font-semibold text-gray-700">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i} className="border-b border-gray-100">
                    {[...Array(7)].map((_, j) => (
                      <td key={j} className="px-4 py-3">
                        <div className="h-4 bg-gray-200 rounded animate-pulse" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : isError ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-red-600">
                    Không thể tải danh sách đơn hàng.
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-gray-500">
                    <ShoppingBag className="h-12 w-12 mx-auto mb-2 text-gray-300" />
                    Chưa có đơn hàng nào
                  </td>
                </tr>
              ) : (
                filtered.map((order) => (
                  <tr key={order.orderId} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                          <ShoppingBag className="h-4 w-4 text-blue-600" />
                        </div>
                        <span className="font-medium text-gray-900">#{order.orderId}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {order.orderedBy?.fullName ?? order.orderedBy?.fullname ?? order.orderedBy?.username ?? "—"}
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      <div className="flex items-center gap-1">
                        <Clock className="h-3.5 w-3.5" />
                        {order.orderDate ? formatDate(order.orderDate) : "—"}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-gray-600 max-w-[200px] truncate">
                      {order.orderDesc ?? "—"}
                    </td>
                    <td className="px-4 py-3 text-right font-bold text-orange-500">
                      <div>{order.quantity ?? 0} sản phẩm</div>
                      <div>{formatPrice(order.orderFee ?? 0)}</div>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <Badge variant={order.status === "CANCELLED" ? "danger" : order.status === "COMPLETED" ? "success" : "warning"}>
                        {order.status === "CANCELLED" ? "Đã hủy" : order.status === "COMPLETED" ? "Hoàn thành" : "Đang xử lý"}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-center gap-1">
                        <button
                          type="button"
                          aria-label={`Xem đơn hàng ${order.orderId}`}
                          onClick={() => setSelectedOrderId(order.orderId)}
                          className="p-1.5 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        {order.status !== "COMPLETED" && order.status !== "CANCELLED" && (
                          <button
                            type="button"
                            aria-label={`Hủy đơn hàng ${order.orderId}`}
                            disabled={cancellingId === order.orderId}
                            onClick={() => cancelOrder(order.orderId)}
                            className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                          >
                            <XCircle className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="flex justify-center gap-2 p-4 border-t border-gray-200">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Trước</Button>
            <span className="px-3 py-1.5 text-sm text-gray-600">Trang {page + 1}/{totalPages}</span>
            <Button variant="outline" size="sm" disabled={page === totalPages - 1} onClick={() => setPage(page + 1)}>Sau</Button>
          </div>
        )}
      </div>

      {selectedOrderId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-lg rounded-2xl bg-white shadow-xl">
            <div className="flex items-center justify-between border-b p-6">
              <h2 className="text-lg font-bold">Chi tiết đơn #{selectedOrderId}</h2>
              <button type="button" onClick={() => setSelectedOrderId(null)} className="text-gray-500 hover:text-gray-900">
                Đóng
              </button>
            </div>
            <div className="space-y-3 p-6 text-sm">
              {isOrderLoading ? (
                <p className="text-gray-500">Đang tải chi tiết đơn hàng...</p>
              ) : isOrderError || !selectedOrder ? (
                <p className="text-red-600">Không thể tải chi tiết đơn hàng.</p>
              ) : (
                <>
                  <div className="flex justify-between gap-4">
                    <span className="text-gray-500">Người đặt</span>
                    <span>{selectedOrder.orderedBy?.fullName ?? selectedOrder.orderedBy?.fullname ?? selectedOrder.orderedBy?.username ?? "—"}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-gray-500">Ngày đặt</span>
                    <span>{selectedOrder.orderDate ? formatDate(selectedOrder.orderDate) : "—"}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-gray-500">Số lượng</span>
                    <span>{selectedOrder.quantity ?? 0}</span>
                  </div>
                  <div className="flex justify-between gap-4">
                    <span className="text-gray-500">Tổng tiền</span>
                    <span className="font-bold text-orange-500">{formatPrice(selectedOrder.orderFee ?? 0)}</span>
                  </div>
                  <div>
                    <p className="text-gray-500">Mô tả</p>
                    <p className="mt-1">{selectedOrder.orderDesc ?? "—"}</p>
                  </div>
                  <div>
                    <p className="text-gray-500">Mã sản phẩm</p>
                    <p className="mt-1">{selectedOrder.productId ?? "—"}</p>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
