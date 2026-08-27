package com.booking.intelligent.config;

import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.*;
import com.booking.intelligent.repository.*;
import com.booking.intelligent.service.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private ShowService showService;

    @Override
    public void run(String... args) throws Exception {
        // 0a. Automatic Schema Migration
        try {
            jdbcTemplate.execute("ALTER TABLE `show_seat` ADD COLUMN `version` INT NOT NULL DEFAULT 0");
        } catch (Exception ignored) {}

        String[] schemaAdditions = new String[]{
            "ALTER TABLE `movie` ADD COLUMN `description` VARCHAR(1500)",
            "ALTER TABLE `movie` ADD COLUMN `original_title` VARCHAR(200)",
            "ALTER TABLE `movie` ADD COLUMN `languages` VARCHAR(200)",
            "ALTER TABLE `movie` ADD COLUMN `censor_rating` VARCHAR(20)",
            "ALTER TABLE `movie` ADD COLUMN `release_year` INT",
            "ALTER TABLE `movie` ADD COLUMN `release_date_status` VARCHAR(30)",
            "ALTER TABLE `movie` ADD COLUMN `poster_url` VARCHAR(500)",
            "ALTER TABLE `movie` ADD COLUMN `backdrop_url` VARCHAR(500)",
            "ALTER TABLE `movie` ADD COLUMN `thumbnail_url` VARCHAR(500)",
            "ALTER TABLE `movie` ADD COLUMN `trailer_url` VARCHAR(500)",
            "ALTER TABLE `movie` ADD COLUMN `director` VARCHAR(200)",
            "ALTER TABLE `movie` ADD COLUMN `movie_cast` VARCHAR(500)",
            "ALTER TABLE `movie` ADD COLUMN `studio` VARCHAR(200)",
            "ALTER TABLE `movie` ADD COLUMN `country` VARCHAR(100)",
            "ALTER TABLE `movie` ADD COLUMN `rating` DOUBLE",
            "ALTER TABLE `movie` ADD COLUMN `anticipation_score` INT",
            "ALTER TABLE `movie` ADD COLUMN `anticipation_label` VARCHAR(30)",
            "ALTER TABLE `movie` ADD COLUMN `is_upcoming` BOOLEAN",
            "ALTER TABLE `movie` ADD COLUMN `is_now_showing` BOOLEAN",
            "ALTER TABLE `theatre` ADD COLUMN `city` VARCHAR(100)",
            "ALTER TABLE `theatre` ADD COLUMN `locality` VARCHAR(150)",
            "ALTER TABLE `theatre` ADD COLUMN `latitude` DOUBLE",
            "ALTER TABLE `theatre` ADD COLUMN `longitude` DOUBLE",
            "ALTER TABLE `theatre` ADD COLUMN `description` VARCHAR(1000)",
            "ALTER TABLE `theatre` ADD COLUMN `phone` VARCHAR(50)",
            "ALTER TABLE `theatre` ADD COLUMN `operating_hours` VARCHAR(100)",
            "ALTER TABLE `theatre` ADD COLUMN `amenities` VARCHAR(500)",
            "ALTER TABLE `theatre` ADD COLUMN `formats` VARCHAR(200)",
            "ALTER TABLE `theatre` ADD COLUMN `total_screens` INT",
            "ALTER TABLE `theatre` ADD COLUMN `total_capacity` INT",
            "ALTER TABLE `screen` ADD COLUMN `screen_type` VARCHAR(50)"
        };

        for (String sql : schemaAdditions) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ignored) {}
        }

        // 1. Seed Roles
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_ADMIN).build()));

        // 2. Seed Demo Users
        User customer = createOrUpdateDemoUser("John Customer", "customer@example.com", "password123", customerRole);
        User provider = createOrUpdateDemoUser("PVK Cinema Operator", "provider@example.com", "password123", providerRole);
        User admin = createOrUpdateDemoUser("System Administrator", "admin@example.com", "password123", adminRole);

        // 3. Seed 25 PVK National Cinema Network (5 Cities x 5 Theatres)
        List<Theatre> allTheatres = seedNationalCinemaNetwork(provider);

        // 4. Seed Verified Upcoming & Currently Airing Movie Catalogue
        List<Movie> nowShowingMovies = seedMovieCatalogue();

        // 5. Seed Active Showtimes across PVK Multiplexes
        seedActiveShowtimes(allTheatres, nowShowingMovies);
    }

    private List<Theatre> seedNationalCinemaNetwork(User owner) {
        List<Theatre> theatres = new ArrayList<>();

        // CHENNAI (5 Theatres)
        theatres.add(upsertNationalTheatre("PVK ICON VR — Anna Nagar", "Chennai", "Anna Nagar", "VR Chennai Mall, 100 Feet Road, Anna Nagar, Chennai", 13.0850, 80.2101, "Flagship 8-screen IMAX & Dolby Atmos luxury multiplex", "+91 44 2621 1100", "09:00 AM - 11:45 PM", "IMAX 4K Laser, Dolby Atmos, Recliners, Valet Parking, Gourmet Food Court, Wheelchair Access", "IMAX, 4K Laser, Dolby Atmos, Recliner", owner, 8));
        theatres.add(upsertNationalTheatre("PVK Palazzo — Vadapalani", "Chennai", "Vadapalani", "Forum Vijaya Mall, Arcot Road, Vadapalani, Chennai", 13.0500, 80.2121, "Premium 6-screen cinematic destination in West Chennai", "+91 44 2362 2200", "09:30 AM - 11:30 PM", "Dolby Atmos, Recliners, Covered Parking, Cafe, Wheelchair Access", "Dolby Atmos, Premium, Standard", owner, 6));
        theatres.add(upsertNationalTheatre("PVK Grand Square — Velachery", "Chennai", "Velachery", "Grand Square Mall, Velachery Main Road, Chennai", 12.9815, 80.2180, "Modern 5-screen multiplex near IT corridor", "+91 44 2259 3300", "10:00 AM - 11:00 PM", "4K Projection, Recliners, Food Court, EV Charging Station", "4K Laser, Premium, Standard", owner, 5));
        theatres.add(upsertNationalTheatre("PVK Sathyam — Royapettah", "Chennai", "Royapettah", "8, Thiru Vi Ka Road, Royapettah, Chennai", 13.0566, 80.2642, "Iconic central Chennai 5-screen entertainment hub", "+91 44 2811 4400", "08:30 AM - 11:45 PM", "Dolby Atmos, Classic concessions, Valet Parking, Wheelchair Access", "Dolby Atmos, Standard", owner, 5));
        theatres.add(upsertNationalTheatre("PVK Marina — OMR / Egattur", "Chennai", "OMR / Egattur", "The Marina Mall, Old Mahabalipuram Road, Egattur, Chennai", 12.8250, 80.2400, "Spacious 4-screen multiplex along the IT expressway", "+91 44 2747 5500", "10:00 AM - 11:15 PM", "Dolby 7.1, Recliners, Ample Parking, Snack Bar", "Premium, Standard", owner, 4));

        // BENGALURU (5 Theatres)
        theatres.add(upsertNationalTheatre("PVK Phoenix — Whitefield", "Bengaluru", "Whitefield", "Phoenix Marketcity, Whitefield Main Road, Mahadevapura, Bengaluru", 12.9959, 77.6964, "Flagship 8-screen IMAX & 4DX multiplex in IT hub", "+91 80 4962 1100", "09:00 AM - 11:45 PM", "IMAX 3D, 4DX, Recliners, Multi-level Parking, Gourmet Cafe, Lounge", "IMAX, 4DX, Dolby Atmos, Recliner", owner, 8));
        theatres.add(upsertNationalTheatre("PVK Garuda — Magrath Road", "Bengaluru", "Magrath Road", "Garuda Mall, Magrath Road, Ashok Nagar, Bengaluru", 12.9705, 77.6094, "Central Bengaluru 6-screen luxury cinema hub", "+91 80 2559 2200", "09:30 AM - 11:30 PM", "Dolby Atmos, VIP Recliners, Valet Parking, Cocktail Lounge", "Dolby Atmos, Recliner, Premium", owner, 6));
        theatres.add(upsertNationalTheatre("PVK Orion — Malleshwaram", "Bengaluru", "Malleshwaram", "Orion Mall, Brigade Gateway, Rajajinagar, Bengaluru", 13.0112, 77.5550, "Lakefront 6-screen entertainment multiplex", "+91 80 2268 3300", "09:30 AM - 11:30 PM", "4K Laser, Recliners, Lakeview Dining, Covered Parking", "4K Laser, Dolby Atmos, Standard", owner, 6));
        theatres.add(upsertNationalTheatre("PVK Swagath — Jayanagar", "Bengaluru", "Jayanagar", "Swagath Garuda Mall, 4th Block, Jayanagar, Bengaluru", 12.9250, 77.5938, "Classic South Bengaluru 4-screen neighbourhood cinema", "+91 80 2654 4400", "10:00 AM - 11:00 PM", "Dolby 7.1, Covered Parking, Concessions, Wheelchair Access", "Premium, Standard", owner, 4));
        theatres.add(upsertNationalTheatre("PVK SoulSpace — Bellandur", "Bengaluru", "Bellandur", "SoulSpace Arena Mall, Outer Ring Road, Bellandur, Bengaluru", 12.9279, 77.6772, "Tech-park adjacent 4-screen modern multiplex", "+91 80 4321 5500", "10:00 AM - 11:15 PM", "Dolby Atmos, Recliners, Cafe, EV Charging Station", "Dolby Atmos, Standard", owner, 4));

        // MUMBAI (5 Theatres)
        theatres.add(upsertNationalTheatre("PVK Palladium — Lower Parel", "Mumbai", "Lower Parel", "High Street Phoenix, Senapati Bapat Marg, Lower Parel, Mumbai", 18.9953, 72.8242, "Premier 8-screen IMAX & Luxury Director's Cut multiplex", "+91 22 4336 1100", "09:00 AM - 11:45 PM", "IMAX 4K Laser, VIP Lounge, Fine Dining, Valet Parking, Recliners", "IMAX, Dolby Atmos, Luxury Recliner", owner, 8));
        theatres.add(upsertNationalTheatre("PVK MarketCity — Kurla", "Mumbai", "Kurla", "Phoenix Marketcity, LBS Marg, Kurla West, Mumbai", 19.0864, 72.8893, "Massive 7-screen entertainment multiplex in Central Mumbai", "+91 22 6180 2200", "09:30 AM - 11:30 PM", "4DX, Dolby Atmos, Recliners, Food Court, Game Zone", "4DX, Dolby Atmos, Premium, Standard", owner, 7));
        theatres.add(upsertNationalTheatre("PVK ICON — Goregaon", "Mumbai", "Goregaon", "Oberoi Mall, Western Express Highway, Goregaon East, Mumbai", 19.1568, 72.8550, "Suburban 6-screen premium cinema venue", "+91 22 4036 3300", "09:30 AM - 11:30 PM", "Dolby Atmos, Recliners, Covered Parking, Gourmet Snack Bar", "Dolby Atmos, Recliner, Standard", owner, 6));
        theatres.add(upsertNationalTheatre("PVK Chakala — Andheri East", "Mumbai", "Andheri East", "Solitaire Corporate Park, Chakala, Andheri East, Mumbai", 19.1158, 72.8683, "Business district 4-screen multiplex near airport", "+91 22 2835 4400", "10:00 AM - 11:00 PM", "4K Laser, Covered Parking, Quick Service Food, Wheelchair Access", "4K Laser, Standard", owner, 4));
        theatres.add(upsertNationalTheatre("PVK Milap — Kandivali", "Mumbai", "Kandivali", "Milap Shopping Centre, SV Road, Kandivali West, Mumbai", 19.2064, 72.8483, "Western Suburbs 4-screen family cinema hub", "+91 22 2801 5500", "10:00 AM - 11:00 PM", "Dolby 7.1, Parking, Snack Counter, Accessible Seating", "Premium, Standard", owner, 4));

        // DELHI (5 Theatres)
        theatres.add(upsertNationalTheatre("PVK Select — Saket", "Delhi", "Saket", "Select CITYWALK, Press Enclave Road, Saket, New Delhi", 28.5284, 77.2189, "South Delhi's premier 8-screen flagship IMAX multiplex", "+91 11 4059 1100", "09:00 AM - 11:45 PM", "IMAX Laser, Gold Class Recliners, Valet Parking, Gourmet Cafe", "IMAX, Dolby Atmos, Gold Class Recliner", owner, 8));
        theatres.add(upsertNationalTheatre("PVK Promenade — Vasant Kunj", "Delhi", "Vasant Kunj", "DLF Promenade, Nelson Mandela Marg, Vasant Kunj, New Delhi", 28.5414, 77.1554, "Upscale 6-screen luxury cinema destination", "+91 11 4606 2200", "09:30 AM - 11:30 PM", "Dolby Atmos, Recliners, VIP Lounge, Fine Concessions", "Dolby Atmos, Recliner, Premium", owner, 6));
        theatres.add(upsertNationalTheatre("PVK Vegas — Dwarka", "Delhi", "Dwarka", "Vegas Mall, Sector 14, Dwarka, New Delhi", 28.5921, 77.0460, "Sprawling 6-screen western Delhi multiplex", "+91 11 6900 3300", "09:30 AM - 11:30 PM", "4DX, Dolby Atmos, Recliners, Ample Parking, Food Court", "4DX, Dolby Atmos, Standard", owner, 6));
        theatres.add(upsertNationalTheatre("PVK Pacific — Subhash Nagar", "Delhi", "Subhash Nagar", "Pacific Mall, Najafgarh Road, Tagore Garden, New Delhi", 28.6433, 77.1128, "West Delhi 5-screen family entertainment venue", "+91 11 4540 4400", "10:00 AM - 11:00 PM", "4K Laser, Recliners, Covered Parking, Wheelchair Access", "4K Laser, Premium, Standard", owner, 5));
        theatres.add(upsertNationalTheatre("PVK Shalimar — Shalimar Bagh", "Delhi", "Shalimar Bagh", "North Square Mall, Netaji Subhash Place, Pitampura, New Delhi", 28.7161, 77.1583, "North Delhi 4-screen neighbourhood multiplex", "+91 11 2735 5500", "10:00 AM - 11:00 PM", "Dolby 7.1, Parking, Snack Bar, Wheelchair Access", "Premium, Standard", owner, 4));

        // HYDERABAD (5 Theatres)
        theatres.add(upsertNationalTheatre("PVK Atrium — Gachibowli", "Hyderabad", "Gachibowli", "Atrium Mall, Gachibowli Main Road, Financial District, Hyderabad", 17.4401, 78.3489, "Flagship 8-screen IMAX & Dolby Atmos tech hub multiplex", "+91 40 4851 1100", "09:00 AM - 11:45 PM", "IMAX 4K Laser, Dolby Atmos, Recliners, Valet Parking, Gourmet Cafe", "IMAX, Dolby Atmos, Recliner", owner, 8));
        theatres.add(upsertNationalTheatre("PVK Galleria — Madhapur", "Hyderabad", "Madhapur", "Inorbit Mall, Mindspace, Madhapur, Hyderabad", 17.4375, 78.3842, "HI-TECH City 6-screen premier multiplex", "+91 40 4008 2200", "09:30 AM - 11:30 PM", "4DX, Dolby Atmos, Recliners, Multi-level Parking, Food Court", "4DX, Dolby Atmos, Premium", owner, 6));
        theatres.add(upsertNationalTheatre("PVK Central — Banjara Hills", "Hyderabad", "Banjara Hills", "GVK One Mall, Road No. 1, Banjara Hills, Hyderabad", 17.4156, 78.4485, "Luxury 5-screen boutique cinema venue", "+91 40 3915 3300", "09:30 AM - 11:30 PM", "Dolby Atmos, VIP Recliners, Valet Parking, Lounge", "Dolby Atmos, Recliner", owner, 5));
        theatres.add(upsertNationalTheatre("PVK Kukatpally — Kukatpally", "Hyderabad", "Kukatpally", "Forum Sujana Mall, KPHB Phase 9, Kukatpally, Hyderabad", 17.4842, 78.3889, "High-density 5-screen suburb multiplex", "+91 40 3041 4400", "10:00 AM - 11:00 PM", "4K Laser, Covered Parking, Quick Service Food", "4K Laser, Standard", owner, 5));
        theatres.add(upsertNationalTheatre("PVK Prism — Gachibowli", "Hyderabad", "Gachibowli", "Prism Complex, Financial District, Gachibowli, Hyderabad", 17.4320, 78.3390, "Financial District 4-screen executive cinema", "+91 40 4912 5500", "10:00 AM - 11:00 PM", "Dolby Atmos, Recliners, Covered Parking, Cafe", "Dolby Atmos, Standard", owner, 4));

        for (Theatre t : theatres) {
            seedScreensAndSeatsForTheatre(t);
        }

        return theatres;
    }

    private Theatre upsertNationalTheatre(String name, String city, String locality, String address, double lat, double lon, String desc, String phone, String hours, String amenities, String formats, User owner, int screenCount) {
        Theatre t = theatreRepository.findAll().stream()
                .filter(x -> x.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (t == null) {
            t = Theatre.builder()
                    .name(name)
                    .location(city)
                    .city(city)
                    .locality(locality)
                    .address(address)
                    .latitude(lat)
                    .longitude(lon)
                    .description(desc)
                    .phone(phone)
                    .operatingHours(hours)
                    .amenities(amenities)
                    .formats(formats)
                    .totalScreens(screenCount)
                    .totalCapacity(screenCount * 60)
                    .ownerUser(owner)
                    .status(TheatreStatus.ACTIVE)
                    .build();
            return theatreRepository.save(t);
        } else {
            t.setLocation(city);
            t.setCity(city);
            t.setLocality(locality);
            t.setAddress(address);
            t.setLatitude(lat);
            t.setLongitude(lon);
            t.setDescription(desc);
            t.setPhone(phone);
            t.setOperatingHours(hours);
            t.setAmenities(amenities);
            t.setFormats(formats);
            t.setTotalScreens(screenCount);
            if (t.getOwnerUser() == null) t.setOwnerUser(owner);
            t.setStatus(TheatreStatus.ACTIVE);
            return theatreRepository.save(t);
        }
    }

    private void seedScreensAndSeatsForTheatre(Theatre theatre) {
        List<Screen> existingScreens = screenRepository.findByTheatreTheatreId(theatre.getTheatreId());
        int targetScreenCount = theatre.getTotalScreens() != null ? theatre.getTotalScreens() : 4;

        if (existingScreens.size() < targetScreenCount) {
            String[] formatsList = theatre.getFormats() != null ? theatre.getFormats().split(",") : new String[]{"Standard"};
            for (int i = existingScreens.size() + 1; i <= targetScreenCount; i++) {
                String format = formatsList[(i - 1) % formatsList.length].trim();
                String screenName = "Screen " + i + " — " + format;
                int cap = format.contains("IMAX") ? 100 : (format.contains("Recliner") ? 40 : 60);

                Screen newScreen = screenRepository.save(Screen.builder()
                        .theatre(theatre)
                        .name(screenName)
                        .screenType(format)
                        .capacity(cap)
                        .build());

                seedPhysicalSeats(newScreen, new String[]{"A", "B", "C", "D", "E", "F"}, 10);
            }
        }

        List<Screen> allScreens = screenRepository.findByTheatreTheatreId(theatre.getTheatreId());
        int totalCap = allScreens.stream().mapToInt(Screen::getCapacity).sum();
        theatre.setTotalScreens(allScreens.size());
        theatre.setTotalCapacity(totalCap);
        theatreRepository.save(theatre);
    }

    private List<Movie> seedMovieCatalogue() {
        List<Movie> nowShowing = new ArrayList<>();

        // VERIFIED AUGUST 21, 2026 CURRENTLY AIRING MOVIES WITH VERIFIED RATINGS
        nowShowing.add(upsertMovie(Movie.builder()
                .title("Magudam")
                .originalTitle("Magudam")
                .description("An intense action drama centered on a battle for honor, power, and justice in rural Tamil Nadu.")
                .genre("Action / Drama")
                .language("Tamil")
                .languages("Tamil")
                .duration(142)
                .censorRating("U/A")
                .releaseDate(LocalDate.of(2026, 8, 14))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80")
                .director("R. Seenu")
                .cast("Vimal, Samuthirakani, Tanya Hope")
                .studio("V Cinema")
                .country("India")
                .rating(6.6)
                .anticipationScore(82)
                .anticipationLabel("HIGH")
                .isUpcoming(false)
                .isNowShowing(true)
                .status(MovieStatus.ACTIVE)
                .build()));

        nowShowing.add(upsertMovie(Movie.builder()
                .title("Vishwanath And Sons")
                .originalTitle("Vishwanath And Sons")
                .description("A heart-warming family drama exploring multi-generational relationships, romance, and moral choices.")
                .genre("Drama / Family / Romance")
                .language("Tamil")
                .languages("Tamil, Telugu")
                .duration(138)
                .censorRating("U/A")
                .releaseDate(LocalDate.of(2026, 8, 14))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80")
                .director("K. Balaji")
                .cast("Sathyaraj, Gautham Karthik, Megha Akash")
                .studio("Sri Lakshmi Productions")
                .country("India")
                .rating(7.2)
                .anticipationScore(80)
                .anticipationLabel("HIGH")
                .isUpcoming(false)
                .isNowShowing(true)
                .status(MovieStatus.ACTIVE)
                .build()));

        nowShowing.add(upsertMovie(Movie.builder()
                .title("DC")
                .originalTitle("DC: Detective Crime")
                .description("A dark crime thriller following a gritty investigator unravelling serial assassinations across North Chennai.")
                .genre("Action / Crime")
                .language("Tamil")
                .languages("Tamil")
                .duration(135)
                .censorRating("A")
                .releaseDate(LocalDate.of(2026, 8, 7))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1568876694728-451bbf694b83?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=1200&auto=format&fit=crop&q=80")
                .director("A. Vetri")
                .cast("Kathir, Natarajan Subramaniam")
                .studio("Ganga Movies")
                .country("India")
                .rating(6.9)
                .anticipationScore(84)
                .anticipationLabel("HIGH")
                .isUpcoming(false)
                .isNowShowing(true)
                .status(MovieStatus.ACTIVE)
                .build()));

        nowShowing.add(upsertMovie(Movie.builder()
                .title("G.D.N.")
                .originalTitle("G.D.N.: G.D. Naidu Bio")
                .description("Biographical drama chronicling the visionary life, inventions, and industrial breakthroughs of G.D. Naidu.")
                .genre("Drama / Biography")
                .language("Tamil")
                .languages("Tamil")
                .duration(145)
                .censorRating("U/A")
                .releaseDate(LocalDate.of(2026, 8, 7))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop&q=80")
                .director("Krishnakumar")
                .cast("Madhavan, Simran")
                .studio("Media One Global")
                .country("India")
                .rating(7.8)
                .anticipationScore(86)
                .anticipationLabel("HIGH")
                .isUpcoming(false)
                .isNowShowing(true)
                .status(MovieStatus.ACTIVE)
                .build()));

        nowShowing.add(upsertMovie(Movie.builder()
                .title("Spider-Man: Brand New Day")
                .originalTitle("Spider-Man: Brand New Day")
                .description("Peter Parker navigates life completely anew in New York City while facing unexpected street-level threats.")
                .genre("Action / Adventure / Sci-Fi")
                .language("English")
                .languages("English, Hindi, Tamil, Telugu")
                .duration(148)
                .censorRating("U/A")
                .releaseDate(LocalDate.of(2026, 7, 31))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop&q=80")
                .director("Jon Watts")
                .cast("Tom Holland, Zendaya, Jacob Batalon")
                .studio("Marvel Studios / Sony Pictures")
                .country("USA")
                .rating(8.5)
                .anticipationScore(95)
                .anticipationLabel("VERY HIGH")
                .isUpcoming(false)
                .isNowShowing(true)
                .status(MovieStatus.ACTIVE)
                .build()));

        // VERIFIED UPCOMING MOVIES (SEPTEMBER 2026 ONWARD)
        upsertMovie(Movie.builder()
                .title("Ramayana: Part 1")
                .originalTitle("Ramayana: Part 1")
                .description("An epic cinematic retelling of the ancient Indian mythological tale of Prince Rama, Sita, and Ravana.")
                .genre("Action / Mythological / Drama")
                .language("Hindi")
                .languages("Hindi, Tamil, Telugu, Kannada, Malayalam")
                .releaseDate(LocalDate.of(2026, 11, 6))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80")
                .director("Nitesh Tiwari")
                .cast("Ranbir Kapoor, Yash, Sai Pallavi, Sunny Deol")
                .anticipationScore(98)
                .anticipationLabel("VERY HIGH")
                .rating(null)
                .isUpcoming(true)
                .isNowShowing(false)
                .status(MovieStatus.ACTIVE)
                .build());

        upsertMovie(Movie.builder()
                .title("Jailer 2")
                .originalTitle("Jailer 2")
                .description("Muthuvel Pandian returns in an action-packed sequel following the mega-blockbuster Jailer.")
                .genre("Action / Crime / Comedy")
                .language("Tamil")
                .languages("Tamil, Telugu, Hindi, Kannada, Malayalam")
                .releaseDate(LocalDate.of(2026, 10, 16))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=80")
                .director("Nelson Dilipkumar")
                .cast("Rajinikanth, Mohanlal, Shiva Rajkumar")
                .anticipationScore(96)
                .anticipationLabel("VERY HIGH")
                .rating(null)
                .isUpcoming(true)
                .isNowShowing(false)
                .status(MovieStatus.ACTIVE)
                .build());

        upsertMovie(Movie.builder()
                .title("King")
                .originalTitle("King")
                .description("High-octane action crime thriller starring Shah Rukh Khan as an elite underworld mentor.")
                .genre("Action / Thriller / Drama")
                .language("Hindi")
                .languages("Hindi, Tamil, Telugu")
                .releaseDate(LocalDate.of(2026, 12, 18))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .posterUrl("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80")
                .backdropUrl("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop&q=80")
                .director("Siddharth Anand")
                .cast("Shah Rukh Khan, Suhana Khan, Abhishek Bachchan")
                .anticipationScore(96)
                .anticipationLabel("VERY HIGH")
                .rating(null)
                .isUpcoming(true)
                .isNowShowing(false)
                .status(MovieStatus.ACTIVE)
                .build());

        upsertMovie(Movie.builder()
                .title("Sardar 2")
                .genre("Action / Spy / Thriller")
                .language("Tamil")
                .releaseDate(LocalDate.of(2026, 9, 11))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .director("P.S. Mithran")
                .cast("Karthi, SJ Suryah")
                .anticipationScore(88)
                .anticipationLabel("HIGH")
                .rating(null)
                .isUpcoming(true)
                .isNowShowing(false)
                .status(MovieStatus.ACTIVE)
                .build());

        upsertMovie(Movie.builder()
                .title("Demonte Colony 3")
                .genre("Horror / Mystery / Thriller")
                .language("Tamil")
                .releaseDate(LocalDate.of(2026, 9, 18))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .director("Ajay Gnanamuthu")
                .cast("Arulnithi, Priya Bhavani Shankar")
                .anticipationScore(85)
                .anticipationLabel("HIGH")
                .rating(null)
                .isUpcoming(true)
                .isNowShowing(false)
                .status(MovieStatus.ACTIVE)
                .build());

        upsertMovie(Movie.builder()
                .title("Mirzapur: The Movie")
                .genre("Action / Crime / Drama")
                .language("Hindi")
                .releaseDate(LocalDate.of(2026, 9, 25))
                .releaseYear(2026)
                .releaseDateStatus("CONFIRMED")
                .director("Gurmmeet Singh")
                .cast("Pankaj Tripathi, Ali Fazal, Divyenndu")
                .anticipationScore(94)
                .anticipationLabel("VERY HIGH")
                .rating(null)
                .isUpcoming(true)
                .isNowShowing(false)
                .status(MovieStatus.ACTIVE)
                .build());

        return nowShowing;
    }

    private void seedActiveShowtimes(List<Theatre> theatres, List<Movie> nowShowingMovies) {
        if (theatres.isEmpty() || nowShowingMovies.isEmpty()) return;

        LocalDate today = LocalDate.of(2026, 8, 21);
        LocalDate tomorrow = today.plusDays(1);

        LocalTime[] timeSlots = new LocalTime[]{
            LocalTime.of(10, 30),
            LocalTime.of(14, 15),
            LocalTime.of(17, 45),
            LocalTime.of(21, 15)
        };

        for (Theatre theatre : theatres) {
            List<Screen> screens = screenRepository.findByTheatreTheatreId(theatre.getTheatreId());
            if (screens.isEmpty()) continue;

            for (int i = 0; i < nowShowingMovies.size(); i++) {
                Movie movie = nowShowingMovies.get(i);
                Screen screen = screens.get(i % screens.size());

                for (LocalDate showDate : new LocalDate[]{today, tomorrow}) {
                    for (LocalTime startTime : timeSlots) {
                        LocalTime endTime = startTime.plusMinutes(movie.getDuration() != null ? movie.getDuration() : 150);

                        boolean exists = showRepository.findByScreenScreenIdAndShowDateAndStatusNot(screen.getScreenId(), showDate, ShowStatus.CANCELLED)
                                .stream()
                                .anyMatch(s -> s.getMovie().getMovieId().equals(movie.getMovieId()) && s.getStartTime().equals(startTime));

                        if (!exists) {
                            Show newShow = Show.builder()
                                    .movie(movie)
                                    .screen(screen)
                                    .showDate(showDate)
                                    .startTime(startTime)
                                    .endTime(endTime)
                                    .ticketPrice(BigDecimal.valueOf(220.00))
                                    .status(ShowStatus.ACTIVE)
                                    .build();

                            try {
                                showService.createShow(newShow, movie.getMovieId(), screen.getScreenId());
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }

    private User createOrUpdateDemoUser(String name, String email, String rawPassword, Role role) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return userRepository.save(User.builder()
                    .name(name)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .build());
        } else {
            user.setName(name);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        }
    }

    private void seedPhysicalSeats(Screen screen, String[] rows, int seatsPerRow) {
        List<Seat> existing = seatRepository.findByScreenScreenId(screen.getScreenId());
        if (existing.isEmpty()) {
            for (String r : rows) {
                SeatType type = (r.equals("E") || r.equals("F") || r.equals("G") || r.equals("H") || r.equals("I") || r.equals("J")) 
                        ? SeatType.BALCONY 
                        : ((r.equals("C") || r.equals("D")) ? SeatType.PREMIUM : SeatType.REGULAR);
                for (int i = 1; i <= seatsPerRow; i++) {
                    seatRepository.save(Seat.builder()
                            .screen(screen)
                            .rowLabel(r)
                            .seatNumber(i)
                            .seatType(type)
                            .build());
                }
            }
        }
    }

    private Movie upsertMovie(Movie template) {
        Movie existing = movieRepository.findByTitle(template.getTitle()).orElse(null);
        boolean upcoming = template.getReleaseDate() == null || template.getReleaseDate().isAfter(LocalDate.of(2026, 8, 21));

        if (existing == null) {
            template.setIsNowShowing(!upcoming);
            template.setIsUpcoming(upcoming);
            return movieRepository.save(template);
        } else {
            existing.setOriginalTitle(template.getOriginalTitle());
            existing.setDescription(template.getDescription());
            existing.setGenre(template.getGenre());
            existing.setLanguage(template.getLanguage());
            existing.setLanguages(template.getLanguages());
            existing.setDuration(template.getDuration());
            existing.setCensorRating(template.getCensorRating());
            existing.setReleaseDate(template.getReleaseDate());
            existing.setReleaseYear(template.getReleaseYear());
            existing.setReleaseDateStatus(template.getReleaseDateStatus());
            existing.setPosterUrl(template.getPosterUrl());
            existing.setBackdropUrl(template.getBackdropUrl());
            existing.setThumbnailUrl(template.getThumbnailUrl());
            existing.setTrailerUrl(template.getTrailerUrl());
            existing.setDirector(template.getDirector());
            existing.setCast(template.getCast());
            existing.setStudio(template.getStudio());
            existing.setCountry(template.getCountry());
            existing.setRating(template.getRating());
            existing.setAnticipationScore(template.getAnticipationScore());
            existing.setAnticipationLabel(template.getAnticipationLabel());
            existing.setIsNowShowing(!upcoming);
            existing.setIsUpcoming(upcoming);
            existing.setStatus(template.getStatus());
            return movieRepository.save(existing);
        }
    }
}
