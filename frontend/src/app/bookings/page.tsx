import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export default function BookingsPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="container mx-auto px-4 py-16">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold text-gray-900 mb-8">My Bookings</h1>
          <div className="bg-white rounded-lg shadow p-8">
            <p className="text-gray-600">Bookings page coming soon!</p>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
