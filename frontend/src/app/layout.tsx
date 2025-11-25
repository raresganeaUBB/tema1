import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";
import { ToasterClient } from "@/components/ToasterClient";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "EventTicketing - Your Gateway to Amazing Events",
  description:
    "Discover, book, and enjoy the best events in your city. From concerts to conferences, find your perfect event experience.",
  keywords: "events, tickets, concerts, conferences, booking, entertainment",
  authors: [{ name: "EventTicketing Team" }],
  openGraph: {
    title: "EventTicketing - Your Gateway to Amazing Events",
    description: "Discover, book, and enjoy the best events in your city.",
    type: "website",
    locale: "en_US",
  },
  twitter: {
    card: "summary_large_image",
    title: "EventTicketing - Your Gateway to Amazing Events",
    description: "Discover, book, and enjoy the best events in your city.",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full">
      <body className={`${inter.className} h-full bg-gray-50`}>
        <Providers>
          <div className="min-h-full">{children}</div>
          <ToasterClient />
        </Providers>
      </body>
    </html>
  );
}
