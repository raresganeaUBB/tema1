"use client";

import { motion } from "framer-motion";
import {
  Music,
  Theater,
  Briefcase,
  Gamepad2,
  GraduationCap,
  Palette,
} from "lucide-react";

const categories = [
  {
    name: "Music",
    icon: Music,
    description: "Concerts, festivals, and musical performances",
    color: "from-pink-500 to-rose-500",
    count: 156,
  },
  {
    name: "Theater",
    icon: Theater,
    description: "Plays, musicals, and theatrical performances",
    color: "from-purple-500 to-indigo-500",
    count: 89,
  },
  {
    name: "Conference",
    icon: Briefcase,
    description: "Business conferences and seminars",
    color: "from-blue-500 to-cyan-500",
    count: 234,
  },
  {
    name: "Sports",
    icon: Gamepad2,
    description: "Sports events and competitions",
    color: "from-green-500 to-emerald-500",
    count: 78,
  },
  {
    name: "Workshop",
    icon: GraduationCap,
    description: "Educational workshops and training",
    color: "from-orange-500 to-yellow-500",
    count: 123,
  },
  {
    name: "Art",
    icon: Palette,
    description: "Art exhibitions and cultural events",
    color: "from-red-500 to-pink-500",
    count: 67,
  },
];

export function Categories() {
  return (
    <section className="section">
      <div className="container">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          viewport={{ once: true }}
          className="text-center mb-12"
        >
          <h2 className="heading-2 mb-4">Browse by Category</h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Find events that match your interests and discover new experiences
          </p>
        </motion.div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {categories.map((category, index) => (
            <motion.div
              key={category.name}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: index * 0.1 }}
              viewport={{ once: true }}
              className="group cursor-pointer"
            >
              <div className="card hover:shadow-strong transition-all duration-300 h-full">
                <div className="card-content">
                  <div
                    className={`w-16 h-16 rounded-2xl bg-gradient-to-r ${category.color} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300`}
                  >
                    <category.icon className="h-8 w-8 text-white" />
                  </div>

                  <h3 className="heading-4 mb-2 group-hover:text-primary-600 transition-colors">
                    {category.name}
                  </h3>

                  <p className="text-gray-600 text-sm mb-4">
                    {category.description}
                  </p>

                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-500">
                      {category.count} events
                    </span>
                    <span className="text-primary-600 text-sm font-medium group-hover:text-primary-700">
                      Explore →
                    </span>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
