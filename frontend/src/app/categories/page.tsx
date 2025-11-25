"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  Music,
  Theater,
  Briefcase,
  Gamepad2,
  GraduationCap,
  Palette,
  ArrowRight,
} from "lucide-react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

interface Event {
  id: number;
  title: string;
  description: string;
  eventDate: string;
  venueId?: number;
  category: string;
  status: string;
  maxAttendees?: number;
}

const categoryIcons: Record<string, any> = {
  Music: Music,
  Theater: Theater,
  Conference: Briefcase,
  Sports: Gamepad2,
  Workshop: GraduationCap,
  Art: Palette,
};

const categoryColors: Record<string, string> = {
  Music: "from-pink-500 to-rose-500",
  Theater: "from-purple-500 to-indigo-500",
  Conference: "from-blue-500 to-cyan-500",
  Sports: "from-green-500 to-emerald-500",
  Workshop: "from-orange-500 to-yellow-500",
  Art: "from-red-500 to-pink-500",
};

export default function CategoriesPage() {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchEvents();
  }, []);

  const fetchEvents = async () => {
    try {
      setLoading(true);
      const response = await fetch("/api/events/");

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      setEvents(data);
    } catch (err) {
      console.error("Error fetching events:", err);
      setError(err instanceof Error ? err.message : "Failed to fetch events");
    } finally {
      setLoading(false);
    }
  };

  // Group events by category
  const eventsByCategory = events.reduce(
    (acc, event) => {
      const category = event.category || "Other";
      if (!acc[category]) {
        acc[category] = [];
      }
      acc[category].push(event);
      return acc;
    },
    {} as Record<string, Event[]>
  );

  // Get all unique categories
  const categories = Object.keys(eventsByCategory).sort();

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header />
        <main className="container mx-auto px-4 py-16">
          <div className="text-center">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            <p className="mt-4 text-gray-600">Loading categories...</p>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header />
        <main className="container mx-auto px-4 py-16">
          <div className="text-center">
            <p className="text-red-600">Error: {error}</p>
            <button onClick={fetchEvents} className="mt-4 btn-primary">
              Try Again
            </button>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="container mx-auto px-4 py-16">
        {/* Header Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h1 className="text-4xl font-bold text-gray-900 mb-4">
            Browse by Category
          </h1>
          <p className="text-xl text-gray-600 max-w-2xl mx-auto">
            Find events that match your interests and discover new experiences
          </p>
        </motion.div>

        {/* Categories Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
          {categories.map((category, index) => {
            const Icon = categoryIcons[category] || Briefcase;
            const color =
              categoryColors[category] || "from-gray-500 to-gray-600";
            const categoryEvents = eventsByCategory[category];
            const eventCount = categoryEvents.length;

            return (
              <motion.div
                key={category}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
              >
                <Link
                  href={`/events?category=${encodeURIComponent(category)}`}
                  className="block group"
                >
                  <div className="card hover:shadow-strong transition-all duration-300 h-full">
                    <div className="card-content">
                      <div
                        className={`w-16 h-16 rounded-2xl bg-gradient-to-r ${color} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300`}
                      >
                        <Icon className="h-8 w-8 text-white" />
                      </div>

                      <h3 className="heading-4 mb-2 group-hover:text-primary-600 transition-colors">
                        {category}
                      </h3>

                      <p className="text-gray-600 text-sm mb-4">
                        {eventCount} {eventCount === 1 ? "event" : "events"}{" "}
                        available
                      </p>

                      <div className="flex items-center justify-between">
                        <span className="text-primary-600 text-sm font-medium group-hover:text-primary-700">
                          Explore {category}
                        </span>
                        <ArrowRight className="h-4 w-4 text-primary-600 group-hover:translate-x-1 transition-transform" />
                      </div>
                    </div>
                  </div>
                </Link>
              </motion.div>
            );
          })}
        </div>

        {/* If no categories found */}
        {categories.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-600 mb-4">No categories found.</p>
            <Link href="/events" className="btn-primary">
              Browse All Events
            </Link>
          </div>
        )}

        {/* View All Events */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="text-center mt-12"
        >
          <Link href="/events" className="btn-outline btn-lg">
            View All Events
          </Link>
        </motion.div>
      </main>
      <Footer />
    </div>
  );
}
