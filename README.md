# Littlepay Transit — Java Coding Exercise

## Assumptions

1. **Passengers are matched by PAN only.** A PAN can have at most one active trip at a time, regardless of `CompanyId`
   or `BusId`.
2. **Input taps may arrive in any order.** They are sorted by `DateTimeUTC` ascending before processing.
3. **A second tap-ON for the same PAN** (before a tap-OFF arrives) forces the first tap-ON to become an INCOMPLETE trip,
   charged at the maximum possible fare from that stop.
4. **Orphaned tap-OFFs** (no matching tap-ON exists for that PAN) are silently ignored and produce no trip.
5. **INCOMPLETE trips** have blank `Finished` and `ToStopId` columns and `DurationSecs = 0` in the output.
6. **Only Stop1, Stop2, and Stop3 are valid stops**, as defined in the spec. The fare matrix is:
    - Stop1 ↔ Stop2: $3.25
    - Stop2 ↔ Stop3: $5.50
    - Stop1 ↔ Stop3: $7.30
7. **The input is well-formed**, as stated in the spec (no missing fields, valid dates, valid stop IDs).
8. **Two endpoints are provided.** `POST /api/trips/upload` is the primary endpoint that satisfies the requirements — it accepts a `.csv` file upload and returns a downloadable `trips.csv`. `POST /api/trips` is a convenience endpoint for manual testing — it accepts and returns raw CSV text in the request/response body.

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

```
POST /api/trips
Content-Type: text/csv
Accept:       text/csv
```

**Request body** — CSV with the following header and one row per tap event:

```
ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559
2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5500005555555559
3, 22-01-2023 09:20:00, ON, Stop3, Company1, Bus36, 4111111111111111
4, 23-01-2023 08:00:00, ON, Stop1, Company1, Bus37, 4111111111111111
5, 23-01-2023 08:02:00, OFF, Stop1, Company1, Bus37, 4111111111111111
6, 24-01-2023 16:30:00, OFF, Stop2, Company1, Bus37, 5500005555555559

```

**Response body** — CSV of processed trips:

```
Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status
22-01-2023 09:20:00, , 0, Stop3, , $7.30, Company1, Bus36, 4111111111111111, INCOMPLETE
22-01-2023 13:00:00, 22-01-2023 13:05:00, 300, Stop1, Stop2, $3.25, Company1, Bus37, 5500005555555559, COMPLETED
23-01-2023 08:00:00, 23-01-2023 08:02:00, 120, Stop1, Stop1, $0.00, Company1, Bus37, 4111111111111111, CANCELLED

```

**Using curl:**

```bash
curl -X POST http://localhost:8080/api/trips \
  -H "Content-Type: text/csv" \
  -H "Accept: text/csv" \
  --data-binary @taps.csv
```

**Using Postman:**
- Method: `POST`, URL: `http://localhost:8080/api/trips`
- Headers: `Content-Type: text/csv`, `Accept: text/csv`
- Body: select **raw**, paste your CSV (including the header row)

### Upload a taps file (file upload endpoint)

```
POST /api/trips/upload
Content-Type: multipart/form-data
```

Returns `trips.csv` as a file download.

**Using curl:**

```bash
curl -X POST http://localhost:8080/api/trips/upload \
  -F "file=@taps.csv" \
  -o trips.csv
```

**Using Postman:**
1. Method: `POST`, URL: `http://localhost:8080/api/trips/upload`
2. **Body** tab → select **form-data**
3. Add key `file` → change type dropdown from **Text** to **File**
4. Click **Select Files** and attach your `.csv`
5. Hit **Send**
6. To save the response: click **Save Response** → **Save to a file** → save as `trips.csv`

---

## Test Harness

```bash
mvn test
```

The test suite has 29 tests across five classes:

| Class | Type | What it covers |
|---|---|---|
| `FareCalculatorTest` | Unit | All routes, symmetry, max fares, unknown route error |
| `TripProcessorTest` | Unit | COMPLETED / INCOMPLETE / CANCELLED trips, orphaned OFFs, consecutive tap-ONs, out-of-order input, multiple PANs, full spec example |
| `IntegrationTest` | Integration | Full CSV-string pipeline: parse → process → write → assert output, header, fares, duration, null fields |
| `TripServiceTest` | Unit (Mockito) | Verifies `TripService.process()` delegates to `TapReader` then `TripProcessor` |
| `TransitApplicationTest` | `@SpringBootTest` | Context loads; `main()` calls `SpringApplication.run()` |

---