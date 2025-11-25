"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { motion } from "framer-motion";
import {
  Calendar,
  MapPin,
  Users,
  Clock,
  Ticket,
  ArrowLeft,
  CreditCard,
  ShoppingCart,
} from "lucide-react";
import Link from "next/link";
import { useCart } from "@/contexts/CartContext";
import toast from "react-hot-toast";

interface Event {
  id: number;
  title: string;
  description: string;
  eventDate: string;
  venueId?: number;
  category: string;
  status: string;
  maxAttendees?: number;
  basePrice?: number;
  ticketPrice?: number; // For backward compatibility
  imageUrl?: string;
}

export default function EventDetailPage() {
  const params = useParams();
  const router = useRouter();
  const eventId = params.id as string;
  const { addToCart } = useCart();

  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);

  useEffect(() => {
    if (eventId) {
      fetchEvent();
    }
  }, [eventId]);

  const fetchEvent = async () => {
    try {
      setLoading(true);
      const response = await fetch(`/api/events/${eventId}`);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      // Map basePrice to ticketPrice for backward compatibility
      if (data.basePrice !== undefined && data.ticketPrice === undefined) {
        data.ticketPrice = data.basePrice;
      }
      setEvent(data);
    } catch (err) {
      console.error("Error fetching event:", err);
      setError(err instanceof Error ? err.message : "Failed to fetch event");
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = () => {
    if (!event) return;

    const price = event.basePrice || event.ticketPrice;
    if (!price) {
      toast.error("This event doesn't have a price set");
      return;
    }

    setAddingToCart(true);

    try {
      addToCart({
        eventId: event.id,
        eventTitle: event.title,
        eventDate: event.eventDate,
        quantity: quantity,
        unitPrice: price,
        imageUrl: event.imageUrl,
      });

      toast.success(`Added ${quantity} ticket(s) to cart!`);

      // Optionally redirect to cart after a short delay
      setTimeout(() => {
        router.push("/cart");
      }, 1000);
    } catch (err) {
      toast.error("Failed to add to cart");
      console.error("Error adding to cart:", err);
    } finally {
      setAddingToCart(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(price);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading event details...</p>
        </div>
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="text-red-500 text-6xl mb-4">⚠️</div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">
            Event Not Found
          </h2>
          <p className="text-gray-600 mb-4">
            {error || "The event you are looking for does not exist."}
          </p>
          <Link
            href="/events"
            className="bg-primary-600 text-white px-6 py-2 rounded-lg hover:bg-primary-700 transition-colors"
          >
            Back to Events
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <Link
            href="/events"
            className="inline-flex items-center text-primary-600 hover:text-primary-700 mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Events
          </Link>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Event Image */}
            <div className="aspect-video bg-gradient-to-br from-primary-400 to-primary-600 rounded-xl flex items-center justify-center">
              {event.imageUrl ? (
                <img
                  src={event.imageUrl}
                  alt={event.title || "Event"}
                  className="w-full h-full object-cover rounded-xl"
                />
              ) : (
                <div className="text-white text-8xl">🎪</div>
              )}
            </div>

            {/* Event Info */}
            <div className="space-y-6">
              <div>
                <div className="flex items-center gap-2 mb-2">
                  <span className="bg-primary-100 text-primary-800 text-sm font-semibold px-3 py-1 rounded-full">
                    {event.category || "General"}
                  </span>
                  <span
                    className={`text-sm font-semibold px-3 py-1 rounded-full ${
                      event.status === "ACTIVE"
                        ? "bg-green-100 text-green-800"
                        : "bg-gray-100 text-gray-800"
                    }`}
                  >
                    {event.status || "ACTIVE"}
                  </span>
                </div>
                <h1 className="text-3xl font-bold text-gray-900 mb-4">
                  {event.title || "Untitled Event"}
                </h1>
                <p className="text-gray-600 text-lg leading-relaxed">
                  {event.description || "No description available"}
                </p>
              </div>

              {/* Event Details */}
              <div className="space-y-4">
                <div className="flex items-center text-gray-700">
                  <Calendar className="h-5 w-5 mr-3 text-primary-600" />
                  <span className="font-medium">
                    {event.eventDate ? formatDate(event.eventDate) : "Date TBA"}
                  </span>
                </div>
                {event.maxAttendees !== undefined && (
                  <div className="flex items-center text-gray-700">
                    <Users className="h-5 w-5 mr-3 text-primary-600" />
                    <span className="font-medium">
                      Capacity: {event.maxAttendees.toLocaleString()}
                    </span>
                  </div>
                )}
                {(event.basePrice !== undefined ||
                  event.ticketPrice !== undefined) && (
                  <div className="flex items-center text-gray-700">
                    <Ticket className="h-5 w-5 mr-3 text-primary-600" />
                    <span className="font-medium text-2xl text-primary-600">
                      {formatPrice(event.basePrice || event.ticketPrice || 0)}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Add to Cart Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="max-w-2xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white rounded-xl shadow-lg p-8"
          >
            <h2 className="text-2xl font-bold text-gray-900 mb-6">
              Book Your Tickets
            </h2>

            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Number of Tickets
                </label>
                <select
                  value={quantity}
                  onChange={(e) => setQuantity(parseInt(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((num) => (
                    <option key={num} value={num}>
                      {num}
                    </option>
                  ))}
                </select>
              </div>

              {(event.basePrice || event.ticketPrice) && (
                <div className="bg-gray-50 p-4 rounded-lg">
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-lg font-medium text-gray-700">
                      Price per ticket:
                    </span>
                    <span className="text-lg font-semibold text-primary-600">
                      {formatPrice(event.basePrice || event.ticketPrice || 0)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-medium text-gray-700">
                      Total Amount:
                    </span>
                    <span className="text-2xl font-bold text-primary-600">
                      {formatPrice(
                        (event.basePrice || event.ticketPrice || 0) * quantity
                      )}
                    </span>
                  </div>
                </div>
              )}

              <button
                onClick={handleAddToCart}
                disabled={
                  addingToCart ||
                  (event.status && event.status !== "ACTIVE") ||
                  (!event.basePrice && !event.ticketPrice)
                }
                className={`w-full py-3 px-4 rounded-lg font-medium transition-colors flex items-center justify-center ${
                  (event.status && event.status !== "ACTIVE") ||
                  (!event.basePrice && !event.ticketPrice)
                    ? "bg-gray-300 text-gray-500 cursor-not-allowed"
                    : addingToCart
                    ? "bg-primary-400 text-white cursor-not-allowed"
                    : "bg-primary-600 text-white hover:bg-primary-700"
                }`}
              >
                {addingToCart ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Adding to cart...
                  </>
                ) : (
                  <>
                    <ShoppingCart className="h-4 w-4 mr-2" />
                    {event.basePrice || event.ticketPrice
                      ? "Add to Cart"
                      : "Pricing TBA"}
                  </>
                )}
              </button>

              <div className="text-center">
                <Link
                  href="/cart"
                  className="text-primary-600 hover:text-primary-700 text-sm font-medium"
                >
                  View Cart →
                </Link>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
