"use client";

import { motion } from "framer-motion";
import { Search, CreditCard, Ticket, Star } from "lucide-react";

const steps = [
  {
    number: "01",
    title: "Search & Discover",
    description:
      "Browse through thousands of events or use our smart search to find exactly what you're looking for.",
    icon: Search,
  },
  {
    number: "02",
    title: "Choose Your Tickets",
    description:
      "Select your preferred seats, ticket types, and any add-ons. See real-time availability and pricing.",
    icon: Ticket,
  },
  {
    number: "03",
    title: "Secure Payment",
    description:
      "Complete your purchase with our secure payment system. We support all major payment methods.",
    icon: CreditCard,
  },
  {
    number: "04",
    title: "Enjoy the Event",
    description:
      "Receive your tickets instantly and enjoy an unforgettable experience. Share your memories with us!",
    icon: Star,
  },
];

export function HowItWorks() {
  return (
    <section className="section bg-gray-50">
      <div className="container">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="heading-2 mb-4">How It Works</h2>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Getting your tickets is simple and secure. Follow these easy steps
            to book your next event.
          </p>
        </motion.div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {steps.map((step, index) => (
            <motion.div
              key={step.number}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: index * 0.1 }}
              viewport={{ once: true }}
              className="text-center"
            >
              <div className="relative mb-6">
                {/* Step Number */}
                <div className="absolute -top-2 -left-2 w-8 h-8 bg-primary-600 text-white rounded-full flex items-center justify-center text-sm font-bold z-10">
                  {step.number}
                </div>

                {/* Icon */}
                <div className="w-20 h-20 bg-white rounded-2xl shadow-soft flex items-center justify-center mx-auto group-hover:scale-110 transition-transform duration-300">
                  <step.icon className="h-10 w-10 text-primary-600" />
                </div>
              </div>

              <h3 className="heading-4 mb-3">{step.title}</h3>
              <p className="text-gray-600 text-sm leading-relaxed">
                {step.description}
              </p>
            </motion.div>
          ))}
        </div>

        {/* CTA Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.4 }}
          viewport={{ once: true }}
          className="text-center mt-16"
        >
          <div className="bg-white rounded-2xl shadow-soft p-8 max-w-2xl mx-auto">
            <h3 className="heading-3 mb-4">Ready to Get Started?</h3>
            <p className="text-gray-600 mb-6">
              Join thousands of people who have already discovered amazing
              events through our platform.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <button className="btn-primary btn-lg">Browse Events</button>
              <button className="btn-outline btn-lg">Learn More</button>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
