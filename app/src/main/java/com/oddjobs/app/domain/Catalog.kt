package com.oddjobs.app.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Catalog {
    val categories = listOf(
        Category("electrician", "Electrician", "ইলেকট্রিশিয়ান", "⚡", 300, 1500),
        Category("plumber", "Plumber", "প্লাম্বার", "🔧", 300, 1500),
        Category("ac_repair", "AC & Appliance Repair", "এসি ও যন্ত্রপাতি মেরামত", "❄️", 500, 3000),
        Category("cleaning", "Cleaning", "পরিষ্কার-পরিচ্ছন্নতা", "🧹", 400, 3000),
        Category("maid", "House Help", "গৃহকর্মী", "🏠", 300, 1500),
        Category("cook", "Cook", "রাঁধুনি", "🍳", 400, 2000),
        Category("painter", "Painter", "রং মিস্ত্রি", "🎨", 800, 5000),
        Category("carpenter", "Carpenter", "কাঠমিস্ত্রি", "🔨", 500, 4000),
        Category("mason", "Mason / Labour", "রাজমিস্ত্রি / শ্রমিক", "🏗️", 700, 3000),
        Category("moving", "Moving & Shifting", "বাসা বদল", "📦", 1500, 10000),
        Category("delivery", "Delivery", "ডেলিভারি", "🛵", 100, 600),
        Category("driver", "Driver", "ড্রাইভার", "🚗", 800, 3000),
        Category("tutor", "Home Tutor", "গৃহশিক্ষক", "📘", 300, 1500),
        Category("mobile_repair", "Phone / Computer Repair", "মোবাইল / কম্পিউটার মেরামত", "📱", 200, 2500),
        Category("beauty", "Beauty at Home", "হোম বিউটি", "💄", 500, 3000),
        Category("pest", "Pest Control", "পোকামাকড় নিয়ন্ত্রণ", "🐜", 800, 4000),
        Category("security", "Security Guard", "নিরাপত্তা কর্মী", "💂", 600, 2000),
        Category("other", "Other", "অন্যান্য", "✨", 200, 5000)
    )

    val areas = listOf(
        Area("dhanmondi", "Dhaka", "ঢাকা", "Dhanmondi", "ধানমন্ডি", 23.7461, 90.3742),
        Area("mohammadpur", "Dhaka", "ঢাকা", "Mohammadpur", "মোহাম্মদপুর", 23.7662, 90.3589),
        Area("uttara", "Dhaka", "ঢাকা", "Uttara", "উত্তরা", 23.8759, 90.3795),
        Area("mirpur", "Dhaka", "ঢাকা", "Mirpur", "মিরপুর", 23.8069, 90.3687),
        Area("gulshan", "Dhaka", "ঢাকা", "Gulshan", "গুলশান", 23.7925, 90.4078),
        Area("banani", "Dhaka", "ঢাকা", "Banani", "বনানী", 23.7937, 90.4066),
        Area("badda", "Dhaka", "ঢাকা", "Badda", "বাড্ডা", 23.7806, 90.4262),
        Area("rampura", "Dhaka", "ঢাকা", "Rampura", "রামপুরা", 23.7615, 90.4258),
        Area("motijheel", "Dhaka", "ঢাকা", "Motijheel", "মতিঝিল", 23.7330, 90.4172),
        Area("old_dhaka", "Dhaka", "ঢাকা", "Old Dhaka", "পুরান ঢাকা", 23.7104, 90.4074),
        Area("bashundhara", "Dhaka", "ঢাকা", "Bashundhara", "বসুন্ধরা", 23.8193, 90.4526),
        Area("mohakhali", "Dhaka", "ঢাকা", "Mohakhali", "মহাখালী", 23.7781, 90.4056),
        Area("farmgate", "Dhaka", "ঢাকা", "Farmgate", "ফার্মগেট", 23.7561, 90.3872),
        Area("tejgaon", "Dhaka", "ঢাকা", "Tejgaon", "তেজগাঁও", 23.7639, 90.3925),
        Area("khilgaon", "Dhaka", "ঢাকা", "Khilgaon", "খিলগাঁও", 23.7510, 90.4300),
        Area("jatrabari", "Dhaka", "ঢাকা", "Jatrabari", "যাত্রাবাড়ী", 23.7100, 90.4350),
        Area("savar", "Dhaka", "ঢাকা", "Savar", "সাভার", 23.8583, 90.2667),
        Area("tongi", "Gazipur", "গাজীপুর", "Tongi", "টঙ্গী", 23.8900, 90.4050),
        Area("gazipur", "Gazipur", "গাজীপুর", "Gazipur Chowrasta", "গাজীপুর চৌরাস্তা", 23.9999, 90.4203),
        Area("narayanganj", "Narayanganj", "নারায়ণগঞ্জ", "Narayanganj Sadar", "নারায়ণগঞ্জ সদর", 23.6238, 90.5000),
        Area("agrabad", "Chattogram", "চট্টগ্রাম", "Agrabad", "আগ্রাবাদ", 22.3250, 91.8110),
        Area("gec", "Chattogram", "চট্টগ্রাম", "GEC Circle", "জিইসি মোড়", 22.3600, 91.8210),
        Area("halishahar", "Chattogram", "চট্টগ্রাম", "Halishahar", "হালিশহর", 22.3400, 91.7800),
        Area("zindabazar", "Sylhet", "সিলেট", "Zindabazar", "জিন্দাবাজার", 24.8949, 91.8687),
        Area("shaheb_bazar", "Rajshahi", "রাজশাহী", "Shaheb Bazar", "সাহেব বাজার", 24.3745, 88.6042),
        Area("sonadanga", "Khulna", "খুলনা", "Sonadanga", "সোনাডাঙ্গা", 22.8220, 89.5400),
        Area("barishal", "Barishal", "বরিশাল", "Barishal Sadar", "বরিশাল সদর", 22.7010, 90.3535),
        Area("rangpur", "Rangpur", "রংপুর", "Rangpur Sadar", "রংপুর সদর", 25.7439, 89.2752),
        Area("mymensingh", "Mymensingh", "ময়মনসিংহ", "Mymensingh Sadar", "ময়মনসিংহ সদর", 24.7471, 90.4203),
        Area("cumilla", "Cumilla", "কুমিল্লা", "Cumilla Sadar", "কুমিল্লা সদর", 23.4607, 91.1809)
    )

    val districts: List<Pair<String, String>> =
        areas.map { it.district to it.districtBn }.distinct()

    fun category(id: String?): Category = categories.firstOrNull { it.id == id } ?: categories.last()
    fun area(id: String?): Area = areas.firstOrNull { it.id == id } ?: areas.first()
    fun areasOf(district: String): List<Area> = areas.filter { it.district == district }
}

object Geo {
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun between(areaA: String, areaB: String): Double {
        if (areaA == areaB) return 0.5
        val a = Catalog.area(areaA)
        val b = Catalog.area(areaB)
        return distanceKm(a.lat, a.lng, b.lat, b.lng)
    }
}

object Validators {
    private val phoneRegex = Regex("^(?:\\+?88)?01[3-9]\\d{8}$")

    /** Converts Bangla digits to ASCII and strips everything except digits and '+'. */
    fun asciiDigits(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when {
                ch in '০'..'৯' -> sb.append(('0' + (ch - '০')))
                ch.isDigit() || ch == '+' -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /** Returns +8801XXXXXXXXX or null when invalid. */
    fun normalizeBdPhone(raw: String): String? {
        val s = asciiDigits(raw)
        if (!phoneRegex.matches(s)) return null
        val core = s.removePrefix("+").removePrefix("88")
        return "+88$core"
    }
}
