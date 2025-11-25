import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export default function SettingsPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="container mx-auto px-4 py-16">
        <div className="max-w-2xl mx-auto">
          <h1 className="text-3xl font-bold text-gray-900 mb-8">Settings</h1>
          <div className="bg-white rounded-lg shadow p-8">
            <p className="text-gray-600">Settings page coming soon!</p>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
