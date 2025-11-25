"use client";

import { useState } from "react";
import { Calendar, MapPin, Users, Star } from "lucide-react";
import { motion } from "framer-motion";

// Mock data - in real app, this would come from API
const featuredEvents = [
  {
    id: 1,
    title: "Summer Music Festival 2024",
    description:
      "A three-day music festival featuring top artists from around the world.",
    date: "2024-07-15",
    time: "18:00",
    venue: "Madison Square Garden",
    location: "New York, NY",
    price: 150,
    image:
      "https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=500&h=300&fit=crop",
    category: "Music",
    rating: 4.8,
    attendees: 20000,
  },
  {
    id: 2,
    title: "Tech Conference 2024",
    description: "Join industry leaders for a two-day technology conference.",
    date: "2024-09-10",
    time: "09:00",
    venue: "Convention Center",
    location: "San Francisco, CA",
    price: 399,
    image:
      "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=500&h=300&fit=crop",
    category: "Conference",
    rating: 4.9,
    attendees: 5000,
  },
  {
    id: 3,
    title: "Broadway Musical: Hamilton",
    description:
      "Experience the revolutionary musical that has taken the world by storm.",
    date: "2024-06-20",
    time: "19:30",
    venue: "Richard Rodgers Theatre",
    location: "New York, NY",
    price: 200,
    image:
      "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&h=300&fit=crop",
    category: "Theater",
    rating: 4.9,
    attendees: 2000,
  },
];

export function FeaturedEvents() {
  const [selectedCategory, setSelectedCategory] = useState("All");

  const categories = [
    "All",
    "Music",
    "Conference",
    "Theater",
    "Sports",
    "Workshop",
  ];

  return (
    <section className="section bg-gray-50">
      <div className="container">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          viewport={{ once: true }}
          className="text-center mb-12"
        >
          <h2 className="heading-2 mb-4">Featured Events</h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Discover the most popular and trending events happening around you
          </p>
        </motion.div>

        {/* Category Filter */}
        <div className="flex flex-wrap justify-center gap-3 mb-12">
          {categories.map((category) => (
            <button
              key={category}
              onClick={() => setSelectedCategory(category)}
              className={`px-6 py-2 rounded-full font-medium transition-colors ${
                selectedCategory === category
                  ? "bg-primary-600 text-white"
                  : "bg-white text-gray-700 hover:bg-gray-100"
              }`}
            >
              {category}
            </button>
          ))}
        </div>

        {/* Events Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {featuredEvents.map((event, index) => (
            <motion.div
              key={event.id}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: index * 0.1 }}
              viewport={{ once: true }}
              className="card group hover:shadow-strong transition-all duration-300 cursor-pointer"
            >
              {/* Event Image */}
              <div className="relative overflow-hidden rounded-t-lg">
                <img
                  src={event.image}
                  alt={event.title}
                  className="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-300"
                  loading="lazy"
                />
                <div className="absolute top-4 left-4">
                  <span className="badge-primary">{event.category}</span>
                </div>
                <div className="absolute top-4 right-4">
                  <div className="flex items-center space-x-1 bg-white/90 backdrop-blur-sm rounded-full px-2 py-1">
                    <Star className="h-3 w-3 text-yellow-500 fill-current" />
                    <span className="text-xs font-medium text-gray-700">
                      {event.rating}
                    </span>
                  </div>
                </div>
              </div>

              {/* Event Content */}
              <div className="card-content">
                <h3 className="heading-4 mb-2 group-hover:text-primary-600 transition-colors">
                  {event.title}
                </h3>
                <p className="text-gray-600 text-sm mb-4 line-clamp-2">
                  {event.description}
                </p>

                {/* Event Details */}
                <div className="space-y-2 mb-4">
                  <div className="flex items-center text-sm text-gray-600">
                    <Calendar className="h-4 w-4 mr-2 text-primary-600" />
                    <span>
                      {new Date(event.date).toLocaleDateString("en-US", {
                        weekday: "short",
                        month: "short",
                        day: "numeric",
                      })}{" "}
                      at {event.time}
                    </span>
                  </div>
                  <div className="flex items-center text-sm text-gray-600">
                    <MapPin className="h-4 w-4 mr-2 text-primary-600" />
                    <span>
                      {event.venue}, {event.location}
                    </span>
                  </div>
                  <div className="flex items-center text-sm text-gray-600">
                    <Users className="h-4 w-4 mr-2 text-primary-600" />
                    <span>{event.attendees.toLocaleString()} attendees</span>
                  </div>
                </div>

                {/* Price and Action */}
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-2xl font-bold text-primary-600">
                      ${event.price}
                    </span>
                    <span className="text-gray-500 text-sm ml-1">
                      per ticket
                    </span>
                  </div>
                  <button className="btn-primary btn-sm">Book Now</button>
                </div>
              </div>
            </motion.div>
          ))}
        </div>

        {/* View All Events Button */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          viewport={{ once: true }}
          className="text-center mt-12"
        >
          <button className="btn-outline btn-lg">View All Events</button>
        </motion.div>
      </div>
    </section>
  );
}
