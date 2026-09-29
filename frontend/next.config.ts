import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "**" },
      { protocol: "http", hostname: "**" },
    ],
  },
  async rewrites() {
    const apiBase = process.env.API_GATEWAY_URL ||"http://localhost:8888";
    return [
      { source: "/api/:path*",        destination: `${apiBase}/api/:path*` },
      { source: "/storefront/:path*", destination: `${apiBase}/storefront/:path*` },
    ];
  },
};

export default nextConfig;
