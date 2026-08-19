# Final Test Verification Report

**Project Title:** AI Decision Engine for Intelligent Booking (Movie Theatre PoC)  
**Document Version:** 1.0  
**Date:** August 20, 2026  

---

## 1. Test Suite Results Overview

The backend integration suite (`mvn test`) and frontend production build (`npm run build`) were executed in the local environment:

```
[INFO] Running com.booking.intelligent.IntelligentBookingApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- Time elapsed: 7.123 s
[INFO] Running com.booking.intelligent.security.AuthIntegrationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- Time elapsed: 0.756 s
[INFO] Running com.booking.intelligent.service.BookingConcurrencyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- Time elapsed: 0.350 s
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 2. Double-Booking Concurrency Verification

* **Scenario**: User A and User B concurrently submit seat hold requests for the exact same `ShowSeat` (`showSeatId = 1`) at the exact same instant.
* **Mechanism**: `ShowSeatRepository.findAllByIdForUpdate()` acquires a `PESSIMISTIC_WRITE` lock (`SELECT ... FOR UPDATE` in SQL).
* **Execution & Result**:
  * **User A**: Request granted. Seat status updated to `HELD` with 10-minute expiration. Booking reference created.
  * **User B**: Request rejected with `SeatNotAvailableException: Seat A1 is no longer available.`.
  * **Final Database State**: Exactly 1 valid hold reservation created. Zero double-booking allocations.

---

## 3. Frontend Production Build Verification

```
> vite build
✓ 1577 modules transformed.
dist/index.html                   0.83 kB │ gzip:  0.46 kB
dist/assets/index-D63_GKOs.css    4.70 kB │ gzip:  1.61 kB
dist/assets/index-DrjPZEG6.js   283.01 kB │ gzip: 85.66 kB
✓ built in 14.21s
```
