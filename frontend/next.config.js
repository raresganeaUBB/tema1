/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    domains: ["localhost", "example.com"],
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**",
      },
    ],
  },
  env: {
    NEXT_PUBLIC_API_URL:
      process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
    NEXT_PUBLIC_BOOKING_API_URL:
      process.env.NEXT_PUBLIC_BOOKING_API_URL || "http://localhost:8081",
  },
  // Output standalone pentru Netlify
  output: "standalone",
  async rewrites() {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
    const bookingApiUrl =
      process.env.NEXT_PUBLIC_BOOKING_API_URL || "http://localhost:8081";

    return [
      {
        source: "/api/events/:path*",
        destination: `${apiUrl}/event-servlet/api/events/:path*`,
      },
      {
        source: "/api/bookings/:path*",
        destination: `${bookingApiUrl}/booking-servlet/api/bookings/:path*`,
      },
      {
        source: "/api/bookings",
        destination: `${bookingApiUrl}/booking-servlet/api/bookings`,
      },
      {
        source: "/api/auth/:path*",
        destination: `${apiUrl}/event-servlet/api/auth/:path*`,
      },
    ];
  },
  webpack: (config) => {
    config.resolve.fallback = {
      ...config.resolve.fallback,
      fs: false,
    };
    return config;
  },
};

module.exports = nextConfig;
