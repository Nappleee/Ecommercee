"use client";

import { useQuery } from "@tanstack/react-query";
import { AlertTriangle, Package } from "lucide-react";
import { productApi } from "@/lib/api";
import type { Product } from "@/types";

export default function AdminInventoryPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-inventory"],
    queryFn: async () => (await productApi.getAll()).data as Product[],
  });
  const products = data ?? [];

  return (
    <div className="space-y-5">
      <div><h1 className="text-2xl font-bold text-gray-900">Kho hàng</h1><p className="text-sm text-gray-500 mt-0.5">Theo dõi tồn kho sản phẩm</p></div>
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? <p className="p-10 text-center text-gray-500">Đang tải...</p> : isError ? <p className="p-10 text-center text-red-600">Không thể tải dữ liệu kho.</p> : (
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200"><tr><th className="text-left px-4 py-3">Sản phẩm</th><th className="text-left px-4 py-3">SKU</th><th className="text-right px-4 py-3">Tồn kho</th><th className="text-left px-4 py-3">Trạng thái</th></tr></thead>
            <tbody>{products.length === 0 ? <tr><td colSpan={4} className="p-10 text-center text-gray-500"><Package className="mx-auto mb-2 h-10 w-10 text-gray-300" />Chưa có dữ liệu kho.</td></tr> : products.map((product) => <tr key={product.productId} className="border-b border-gray-100"><td className="px-4 py-3 font-medium">{product.productTitle}</td><td className="px-4 py-3 text-gray-500">{product.sku ?? "—"}</td><td className="px-4 py-3 text-right font-semibold">{product.quantity}</td><td className="px-4 py-3">{product.quantity <= 5 ? <span className="inline-flex items-center gap-1 text-amber-600"><AlertTriangle className="h-4 w-4" />Sắp hết</span> : <span className="text-green-600">Còn hàng</span>}</td></tr>)}</tbody>
          </table>
        )}
      </div>
    </div>
  );
}
