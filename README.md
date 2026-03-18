# Littlepay Transit — Java Coding Exercise

## Assumptions

1. **Passengers are matched by PAN only.** A PAN can have at most one active trip at a time, regardless of `CompanyId` or `BusId`.
2. **Input taps may arrive in any order.** They are sorted by `DateTimeUTC` ascending before processing.
3. **A second tap-ON for the same PAN** (before a tap-OFF arrives) forces the first tap-ON to become an INCOMPLETE trip, charged at the maximum possible fare from that stop.
4. **Orphaned tap-OFFs** (no matching tap-ON exists for that PAN) are silently ignored and produce no trip.
5. **INCOMPLETE trips** have blank `Finished` and `ToStopId` columns and `DurationSecs = 0` in the output.
6. **Only Stop1, Stop2, and Stop3 are valid stops**, as defined in the spec. The fare matrix is:
    - Stop1 ↔ Stop2: $3.25
    - Stop2 ↔ Stop3: $5.50
    - Stop1 ↔ Stop3: $7.30
7. **The input is well-formed**, as stated in the spec (no missing fields, valid dates, valid stop IDs).

---

## How to Run

**Prerequisites:** Java 25, Maven 3.6, Spring Boot 4

### Build

```bash
cd littlepay-transit
mvn clean package
```

### Start the server

```bash
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

### Process a taps file via the REST endpoint

```bash
curl -X POST http://localhost:8080/api/trips \
  -H "Content-Type: text/csv" \
  -H "Accept: text/csv" \
  --data-binary @taps.csv
```

The endpoint accepts a CSV of tap events and returns a CSV of processed trips.

---

## Output File for the Example Input

For the provided [`taps.csv`](taps.csv):

```
Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status
22-01-2023 09:20:00, , 0, Stop3, , $7.30, Company1, Bus36, 4111111111111111, INCOMPLETE
22-01-2023 13:00:00, 22-01-2023 13:05:00, 300, Stop1, Stop2, $3.25, Company1, Bus37, 5500005555555559, COMPLETED
23-01-2023 08:00:00, 23-01-2023 08:02:00, 120, Stop1, Stop1, $0.00, Company1, Bus37, 4111111111111111, CANCELLED
```

The generated output is also saved as [`trips.csv`](trips.csv).

**Tap 6** (OFF at Stop2, PAN `5500005555555559`, 24 Jan) is an orphaned tap-OFF — that PAN's trip was already completed on 22 Jan — so it is silently ignored.

---
