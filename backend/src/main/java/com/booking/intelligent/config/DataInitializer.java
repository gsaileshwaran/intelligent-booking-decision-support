package com.booking.intelligent.config;

import com.booking.intelligent.entity.*;
import com.booking.intelligent.enums.*;
import com.booking.intelligent.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

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

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Roles
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_CUSTOMER).build()));

        Role providerRole = roleRepository.findByRoleName(RoleName.ROLE_SERVICE_PROVIDER)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_SERVICE_PROVIDER).build()));

        Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().roleName(RoleName.ROLE_ADMIN).build()));

        // 2. Seed Default Users (Password: password123)
        User customer = userRepository.findByEmail("customer@example.com").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .name("John Customer")
                    .email("customer@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(customerRole)
                    .status(UserStatus.ACTIVE)
                    .build());
        });

        User provider = userRepository.findByEmail("provider@example.com").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .name("Cineplex Operator")
                    .email("provider@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(providerRole)
                    .status(UserStatus.ACTIVE)
                    .build());
        });

        User admin = userRepository.findByEmail("admin@example.com").orElseGet(() -> {
            return userRepository.save(User.builder()
                    .name("System Administrator")
                    .email("admin@example.com")
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(adminRole)
                    .status(UserStatus.ACTIVE)
                    .build());
        });

        // 3. Seed Theatres
        if (theatreRepository.count() == 0) {
            Theatre theatre1 = theatreRepository.save(Theatre.builder()
                    .ownerUser(provider)
                    .name("Grand Cinema Downtown")
                    .location("Metropolis Central")
                    .address("123 Main Boulevard, Downtown")
                    .status(TheatreStatus.ACTIVE)
                    .build());

            Theatre theatre2 = theatreRepository.save(Theatre.builder()
                    .ownerUser(provider)
                    .name("Grand Cinema Westside IMAX")
                    .location("Westside Mall")
                    .address("456 West Avenue, Shopping District")
                    .status(TheatreStatus.ACTIVE)
                    .build());

            // 4. Seed Screens
            Screen screen1 = screenRepository.save(Screen.builder()
                    .theatre(theatre1)
                    .name("Auditorium 1")
                    .capacity(30)
                    .build());

            Screen screen2 = screenRepository.save(Screen.builder()
                    .theatre(theatre1)
                    .name("Auditorium 2 (VIP)")
                    .capacity(20)
                    .build());

            Screen screen3 = screenRepository.save(Screen.builder()
                    .theatre(theatre2)
                    .name("IMAX Hall A")
                    .capacity(40)
                    .build());

            // 5. Seed Physical Seats for Screen 1 (Rows A, B, C)
            String[] rows = {"A", "B", "C"};
            for (String r : rows) {
                SeatType type = r.equals("C") ? SeatType.BALCONY : (r.equals("B") ? SeatType.PREMIUM : SeatType.REGULAR);
                for (int i = 1; i <= 10; i++) {
                    seatRepository.save(Seat.builder()
                            .screen(screen1)
                            .rowLabel(r)
                            .seatNumber(i)
                            .seatType(type)
                            .build());
                }
            }

            // 6. Seed Movies
            Movie movie1 = movieRepository.save(Movie.builder()
                    .title("Cyber Odyssey 2099")
                    .genre("Sci-Fi / Thriller")
                    .language("English")
                    .duration(145)
                    .status(MovieStatus.ACTIVE)
                    .build());

            Movie movie2 = movieRepository.save(Movie.builder()
                    .title("The Midnight Cipher")
                    .genre("Mystery / Drama")
                    .language("English")
                    .duration(120)
                    .status(MovieStatus.ACTIVE)
                    .build());

            // 7. Seed Shows
            Show show1 = showRepository.save(Show.builder()
                    .movie(movie1)
                    .screen(screen1)
                    .showDate(LocalDate.now())
                    .startTime(LocalTime.of(18, 0))
                    .endTime(LocalTime.of(20, 25))
                    .ticketPrice(BigDecimal.valueOf(15.00))
                    .status(ShowStatus.ACTIVE)
                    .build());

            Show show2 = showRepository.save(Show.builder()
                    .movie(movie2)
                    .screen(screen2)
                    .showDate(LocalDate.now())
                    .startTime(LocalTime.of(19, 0))
                    .endTime(LocalTime.of(21, 0))
                    .ticketPrice(BigDecimal.valueOf(22.00))
                    .status(ShowStatus.ACTIVE)
                    .build());

            // 8. Generate ShowSeats for Show 1
            Iterable<Seat> seats = seatRepository.findAll();
            for (Seat s : seats) {
                if (s.getScreen().getScreenId().equals(screen1.getScreenId())) {
                    BigDecimal price = s.getSeatType() == SeatType.BALCONY ? BigDecimal.valueOf(20.00)
                            : (s.getSeatType() == SeatType.PREMIUM ? BigDecimal.valueOf(18.00) : BigDecimal.valueOf(15.00));
                    showSeatRepository.save(ShowSeat.builder()
                            .show(show1)
                            .seat(s)
                            .status(ShowSeatStatus.AVAILABLE)
                            .price(price)
                            .build());
                }
            }
        }
    }
}
