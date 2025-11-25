"use client";

import { useState } from "react";
import { Search, Calendar, MapPin, Users, ArrowRight } from "lucide-react";
import { motion } from "framer-motion";
import Link from "next/link";

export function Hero() {
  const [searchQuery, setSearchQuery] = useState("");

  return (
    <section className="relative bg-gradient-to-br from-primary-600 via-primary-700 to-primary-800 text-white">
      {/* Background Pattern */}
      <div className="absolute inset-0 bg-black/20" />
      <div className="absolute inset-0 bg-[url('data:image/svg+xml,%3Csvg%20width%3D%2260%22%20height%3D%2260%22%20viewBox%3D%220%200%2060%2060%22%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%3E%3Cg%20fill%3D%22none%22%20fill-rule%3D%22evenodd%22%3E%3Cg%20fill%3D%22%23ffffff%22%20fill-opacity%3D%220.05%22%3E%3Ccircle%20cx%3D%2230%22%20cy%3D%2230%22%20r%3D%222%22/%3E%3C/g%3E%3C/g%3E%3C/svg%3E')] opacity-30" />

      <div className="relative container py-24 sm:py-32 lg:py-40">
        <div className="max-w-4xl mx-auto text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
          >
            <h1 className="heading-1 text-white mb-6">
              Discover Amazing Events
              <span className="block text-primary-200">Near You</span>
            </h1>
            <p className="text-xl text-primary-100 mb-8 max-w-2xl mx-auto">
              From concerts to conferences, find and book the perfect events
              that match your interests. Join thousands of people discovering
              unforgettable experiences.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="max-w-2xl mx-auto"
          >
            {/* Search Bar */}
            <div className="relative mb-8">
              <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400 h-5 w-5" />
              <input
                type="text"
                placeholder="Search for events, venues, or categories..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-12 pr-4 py-4 text-lg rounded-xl border-0 focus:ring-4 focus:ring-primary-300 focus:outline-none text-gray-900 placeholder-gray-500"
              />
              <button className="absolute right-2 top-1/2 transform -translate-y-1/2 bg-primary-600 hover:bg-primary-700 text-white px-6 py-2 rounded-lg font-medium transition-colors">
                Search
              </button>
            </div>

            {/* Browse Events Button */}
            <div className="mb-8">
              <Link
                href="/events"
                className="inline-flex items-center bg-white text-primary-600 hover:bg-primary-50 px-8 py-4 rounded-xl font-semibold text-lg transition-colors shadow-lg hover:shadow-xl"
              >
                Browse All Events
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </div>

            {/* Quick Filters */}
            <div className="flex flex-wrap justify-center gap-3 mb-8">
              {["Music", "Sports", "Theater", "Conference", "Workshop"].map(
                (category) => (
                  <button
                    key={category}
                    className="px-4 py-2 bg-white/20 hover:bg-white/30 backdrop-blur-sm rounded-full text-sm font-medium transition-colors"
                  >
                    {category}
                  </button>
                )
              )}
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="grid grid-cols-1 sm:grid-cols-3 gap-8 max-w-3xl mx-auto"
          >
            <div className="text-center">
              <div className="bg-white/20 backdrop-blur-sm rounded-2xl p-6 mb-4">
                <Calendar className="h-8 w-8 mx-auto text-primary-200" />
              </div>
              <h3 className="text-lg font-semibold mb-2">Easy Booking</h3>
              <p className="text-primary-200 text-sm">
                Book your tickets in just a few clicks
              </p>
            </div>

            <div className="text-center">
              <div className="bg-white/20 backdrop-blur-sm rounded-2xl p-6 mb-4">
                <MapPin className="h-8 w-8 mx-auto text-primary-200" />
              </div>
              <h3 className="text-lg font-semibold mb-2">Local Events</h3>
              <p className="text-primary-200 text-sm">
                Discover events happening near you
              </p>
            </div>

            <div className="text-center">
              <div className="bg-white/20 backdrop-blur-sm rounded-2xl p-6 mb-4">
                <Users className="h-8 w-8 mx-auto text-primary-200" />
              </div>
              <h3 className="text-lg font-semibold mb-2">Community</h3>
              <p className="text-primary-200 text-sm">
                Join a vibrant community of event-goers
              </p>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
