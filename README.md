# Hotel Booking System

A Spring Boot backend for a hotel booking platform. Owners onboard properties, guests search for available
rooms and book them, then pay for the booking and can later cancel it.

- Java 21 (target), Spring Boot 4.1, Maven wrapper included
- In-memory persistence behind repository interfaces
- Mock payment gateway behind our own abstraction
- OpenAPI / Swagger UI

## Build and run

You need JDK 21 or later. You don't need Maven installed.

```bash
./mvnw clean verify                                        # compile + run all tests
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo     # start on :8080 with sample hotels
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

The `demo` profile seeds a standalone hotel (Asha Residency, Bengaluru) and a chain (Lotus Hospitality Group:
Lotus Grand in Bengaluru and Lotus Bay in Mumbai). Their ids are printed in the startup log. Without the profile
the app starts empty.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/owners` | Onboard an owner with 1..n properties (a standalone hotel sends one) |
| POST | `/api/v1/owners/{ownerId}/properties` | Add a property to an existing owner |
| GET | `/api/v1/owners/{ownerId}` | Owner and its property ids |
| GET | `/api/v1/properties/{propertyId}` | Property details |
| GET | `/api/v1/properties/search` | Search. Required: `city`, `checkIn`, `checkOut`. Optional: `guests` (default 1), `locality`, `minPrice`, `maxPrice`, `amenities`, `minRating` |
| POST | `/api/v1/bookings` | Create a booking. It holds a room and starts in `PENDING_PAYMENT` |
| GET | `/api/v1/bookings/{bookingId}` | Booking and its state |
| POST | `/api/v1/bookings/{bookingId}/payments` | Pay with `CARD` / `UPI` / `WALLET`. Requires the `Idempotency-Key` header |
| POST | `/api/v1/bookings/{bookingId}/cancellation` | Cancel, refund per policy and release the room |

Errors are returned as RFC 7807 problem details:

| Status | Meaning |
|---|---|
| 400 | Invalid input. Bean-validation errors include `fieldErrors` |
| 404 | Unknown id |
| 409 | Room unavailable, or an illegal state change (e.g. paying for a cancelled booking) |
| 422 | Unsupported payment method |
| 502 | The gateway refused a refund |

### Try it

```bash
# 1. Onboard a standalone hotel
curl -s -X POST localhost:8080/api/v1/owners -H 'Content-Type: application/json' -d '{
  "name": "Sea Breeze",
  "properties": [{
    "name": "Sea Breeze Inn",
    "address": {"line": "12 Beach Rd", "locality": "Calangute", "city": "Goa"},
    "starRating": 4, "amenities": ["WIFI", "POOL"],
    "roomTypes": [{"name": "Deluxe", "maxOccupancy": 2, "totalUnits": 1, "pricePerNight": 4000}]
  }]}'

# 2. Search (dates must not be in the past)
curl -s 'localhost:8080/api/v1/properties/search?city=Goa&checkIn=2027-03-01&checkOut=2027-03-03&guests=2&amenities=POOL&maxPrice=5000'

# 3. Book (use propertyId / roomTypeId from step 1)
curl -s -X POST localhost:8080/api/v1/bookings -H 'Content-Type: application/json' -d '{
  "propertyId": "<propertyId>", "roomTypeId": "<roomTypeId>",
  "guestName": "Asha", "guests": 2, "checkIn": "2027-03-01", "checkOut": "2027-03-03"}'

# 4. Pay. Repeating the call with the same key returns the same payment ("replayed": true)
curl -s -X POST localhost:8080/api/v1/bookings/<bookingId>/payments \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: 7f1c' \
  -d '{"method": "UPI", "details": {"vpa": "asha@okbank"}}'

# 5. Cancel
curl -s -X POST localhost:8080/api/v1/bookings/<bookingId>/cancellation
```

Payment details by method:
- `CARD`: `{"cardToken": "..."}`
- `UPI`: `{"vpa": "name@bank"}`
- `WALLET`: `{"walletId": "..."}`

To simulate a decline, use any instrument that starts with `fail` (e.g. `"cardToken": "fail_tok"`).

## Architecture

Dependencies point inward. The domain is plain Java and declares the interfaces it needs. Infrastructure
implements those interfaces, and Spring only wires the pieces together.

```
 api (controllers, DTOs, ProblemDetail mapping)
   │
   ▼
 application (use cases: Onboarding, Search, Booking, Payment, Cancellation services)
   │
   ▼
 domain (entities, value objects, state machine, ports)   ◄──implements──  infrastructure
   Property, RoomType, PropertyOwner, Booking, Payment,                    InMemory*Repository
   Money, DateRange, BookingStatus                                         InMemoryInventoryLedger
   ports: *Repository, InventoryLedger, PaymentProcessor,                  Card/Upi/WalletPaymentProcessor
          PaymentGateway, PricingStrategy, CancellationPolicy,             MockPaymentGateway
          PropertyFilterFactory                                            TieredCancellationPolicy, pricing,
                                                                           search filter factories
```

```
com.rupeek.hotelbooking
├── domain                 plain Java: no Spring, no persistence
│   ├── common             Money, DateRange, Address, DomainException hierarchy, Preconditions
│   ├── property           PropertyOwner, Property, RoomType, Amenity
│   ├── inventory          InventoryLedger (port), RoomUnavailableException
│   ├── booking            Booking, BookingStatus (state machine), InvalidBookingStateException
│   ├── payment            Payment, PaymentStatus, PaymentProcessor, PaymentGateway (ports), PaymentRequest/Result
│   ├── cancellation       CancellationPolicy (port), RefundDecision
│   ├── pricing            PricingStrategy (port)
│   ├── search             SearchCriteria, RoomOffer, PropertyFilter, PropertyFilterFactory (port)
│   └── repository         Owner/Property/Booking/PaymentRepository (ports)
├── application            OnboardingService, SearchService, BookingService, PaymentService,
│                          CancellationService, PaymentProcessorRegistry, commands, LockManager (port)
├── infrastructure
│   ├── persistence        InMemory*Repository, InMemoryInventoryLedger
│   ├── payment            Card/Upi/WalletPaymentProcessor, MockPaymentGateway
│   ├── policy             TieredCancellationPolicy, BaseNightlyPricing, WeekendSurchargePricing
│   ├── search             PriceRange/Amenities/StarRating filter factories
│   ├── concurrency        StripedLockManager
│   └── config             DomainConfig (bean selection), HotelProperties, DemoDataSeeder
└── api                    controllers, dto (request/response records), error (GlobalExceptionHandler)
```

### Domain model

```
 +-------------------+ 1    1..* +-------------------------+ 1    1..* +---------------------+
 | PropertyOwner     |---------->| Property                |<>-------->| RoomType            |
 |-------------------|   owns    |-------------------------|   has     |---------------------|
 | id, name          |           | id, ownerId, name       |           | id, name            |
 | propertyIds       |           | Address(city, locality) |           | maxOccupancy        |
 | isMultiProperty() |           | starRating (1..5)       |           | totalUnits          |
 +-------------------+           | Set<Amenity>            |           | basePricePerNight   |
                                 +------------+------------+           +----------+----------+
                                              ^ propertyId                        ^ roomTypeId
                                 +------------+-----------------------------------+--+
                                 | Booking                                           |
                                 |---------------------------------------------------|
                                 | id, guestName, guests                             |
                                 | DateRange stay [checkIn, checkOut)  <- value obj  |
                                 | Money amount                        <- value obj  |
                                 | BookingStatus status, paymentId, refundAmount     |
                                 | confirm() / markPaymentFailed() / cancel(refund)  |
                                 +-------------------------+-------------------------+
                                                           ^ bookingId
                                 +-------------------------+-------------------------+
                                 | Payment                                           |
                                 | id, idempotencyKey, method, Money amount,         |
                                 | PaymentStatus (SUCCEEDED | FAILED | REFUNDED),    |
                                 | gatewayReference, failureReason, refundedAmount   |
                                 +---------------------------------------------------+

 Port (domain)            Implementation (infrastructure)
 ----------------------   -------------------------------------------------------------
 PaymentProcessor     <|.. CardPaymentProcessor, UpiPaymentProcessor, WalletPaymentProcessor
 PaymentGateway       <|.. MockPaymentGateway
 CancellationPolicy   <|.. TieredCancellationPolicy
 PricingStrategy      <|.. BaseNightlyPricing, WeekendSurchargePricing
 PropertyFilterFactory<|.. PriceRangeFilterFactory, AmenitiesFilterFactory, StarRatingFilterFactory
 InventoryLedger      <|.. InMemoryInventoryLedger
 *Repository          <|.. InMemory*Repository
 LockManager          <|.. StripedLockManager
```

### Booking lifecycle

```
             create (room held)
                    │
                    ▼
            ┌─────────────────┐  payment declined     ┌────────────────┐
            │ PENDING_PAYMENT │──────────────────────►│ PAYMENT_FAILED │  (room released)
            └─────────────────┘                       └────────────────┘
               │           │ cancel (no refund, room released)
   payment ok  ▼           ▼
        ┌───────────┐   ┌───────────┐
        │ CONFIRMED │──►│ CANCELLED │  (policy refund, room released)
        └───────────┘   └───────────┘
```

Any other transition throws `InvalidBookingStateException`, which the API returns as 409. `BookingStatus` is
the only place where the allowed transitions are defined.

### Flows

**1. Onboarding.** A standalone hotel and a chain take the same path.
```
Owner          OwnerController        OnboardingService       OwnerRepository   PropertyRepository
  | POST /owners       |                      |                      |                  |
  | {name,             |                      |                      |                  |
  |  properties[1..n]} |                      |                      |                  |
  |------------------->| @Valid -> commands   |                      |                  |
  |                    |--------------------->| build ALL properties first (one bad one rejects all)
  |                    |                      |--- save(owner) ----->|                  |
  |                    |                      |--------------- save(each property) ---->|
  |<-- 201 Created ----|<-- owner + props ----|                      |                  |
  (POST /owners/{id}/properties adds one more property to an existing owner)
```

**2. Search.** Only rooms that are free for the whole stay are returned.
```
Guest      PropertyController      SearchService       FilterFactories  PropertyRepo  InventoryLedger  PricingStrategy
  | GET /properties/search?city&checkIn&checkOut&guests[&minPrice&maxPrice&amenities&minRating]
  |------------->|                       |                    |              |              |               |
  |              |-- search(criteria,    |                    |              |              |               |
  |              |     all params) ----->|-- create(params) ->| (only those whose params are present)       |
  |              |                       |<- filters ---------|              |              |               |
  |              |                       |-- findByCity ---------------------->|            |               |
  |              |                       | for each property (locality match) x room type:  |               |
  |              |                       |   maxOccupancy >= guests?         |              |               |
  |              |                       |-- availableUnits(rt, stay) ------------------->  |               |
  |              |                       |   (min over all nights) > 0?      |              |               |
  |              |                       |-- quote(rt, stay, guests) ------------------------------------->  |
  |              |                       | RoomOffer -> apply all filters -> group by property, cheapest first
  |<-- 200 [property + available room types + prices] ---------------------------------------------------   |
```

**3. Booking.** The room hold is concurrency-safe.
```
Guest      BookingController     BookingService     PropertyRepo   InventoryLedger            BookingRepo
  | POST /bookings {propertyId, roomTypeId, guestName, guests, checkIn, checkOut}
  |------------->|-- create ---------->|                 |               |                          |
  |              |                     |-- find -------->| (404 if property / room type missing)    |
  |              |                     | validate: not in past, guests <= occupancy, quote price      |
  |              |                     |   (all before touching inventory: bad requests hold nothing) |
  |              |                     |-- reserve(roomType, stay) ----->|                          |
  |              |                     |                 |  synchronized(roomType calendar) {       |
  |              |                     |                 |    any night full? -> 409 RoomUnavailable|
  |              |                     |                 |    else hold 1 unit on EVERY night }     |
  |              |                     |-- save(Booking PENDING_PAYMENT) -------------------------->|
  |<-- 201 {id, amount, status: PENDING_PAYMENT} --------|               |                          |
```

**4. Payment.** Retries with the same key are idempotent, and the outcome drives the booking state.
```
Guest   PaymentController  PaymentService  LockManager  PaymentRepo  ProcessorRegistry  Card/UPI/Wallet  Gateway  InventoryLedger
  | POST /bookings/{id}/payments  Idempotency-Key: k1  {method, details}
  |--------->|--- pay ------->|-- lock(booking) ->|          |               |                |            |          |
  |          |                |-- findByIdempotencyKey(k1) ->|               |                |            |          |
  |          |                | [found]  -> return it, replayed=true (no second charge)       |            |          |
  |          |                | [new]    -> booking must be PENDING_PAYMENT (else 409)        |            |          |
  |          |                |-- processorFor(method) ----------------------->| (422 if unknown)         |          |
  |          |                |-- process(request) ----------------------------------------->| validate   |          |
  |          |                |                   |          |               |                |-- charge ->|          |
  |          |                |-- save(Payment) ------------>|               |                |            |          |
  |          |                | SUCCEEDED -> booking.confirm(paymentId)       -> CONFIRMED     |            |          |
  |          |                | FAILED    -> booking.markPaymentFailed()      -> PAYMENT_FAILED + release room ----->|
  |<-- 200 {paymentId, paymentStatus, bookingStatus, replayed} ------------------------------------------------------ |
```

**5. Cancellation.** The refund policy is pluggable and the room is released.
```
Guest   BookingController  CancellationService  LockManager  BookingRepo  CancellationPolicy  Gateway  PaymentRepo  InventoryLedger
  | POST /bookings/{id}/cancellation
  |--------->|-- cancel ---->|-- lock(booking) ->|            |               |              |           |            |
  |          |               |-- load ---------------------->| (404 if missing)             |           |            |
  |          |               | must be allowed to move to CANCELLED (else 409, before any money moves)  |            |
  |          |               | CONFIRMED? -- evaluate(booking, now) ------->| -> RefundDecision         |            |
  |          |               |    else    -> refund 0 ("not paid")          |              |           |            |
  |          |               | refund > 0? -- refund(gatewayRef, amount) ------------------->|           |            |
  |          |               |             -- payment.markRefunded ---------------------------------------->|        |
  |          |               |-- booking.cancel(refund, now) -> CANCELLED   |              |           |            |
  |          |               |-- release(roomType, stay) ------------------------------------------------------------>|
  |<-- 200 {booking, refundAmount, refundReason} ------------------------------------------------------------------ |
```

**End to end:**
```
POST /owners ─> GET /properties/search ─> POST /bookings ─> POST .../payments ─> POST .../cancellation ─> search again
  onboard        room listed               PENDING_PAYMENT    CONFIRMED            CANCELLED + refund       room listed
                                           (room held)                             (room released)
```

## Key design decisions

- **One onboarding path for single and multi-property owners.** Every property belongs to a `PropertyOwner`.
  A standalone hotel is an owner with one property, and a chain is an owner with several. Nothing is
  special-cased; `isMultiProperty()` is derived from the number of properties. If any property in a request is
  invalid, the whole onboarding is rejected.
- **Inventory is a per-night ledger, not a list of bookings.** `InventoryLedger` counts held units per room type
  per night. A booking holds one unit on every night of `[checkIn, checkOut)`, so back-to-back stays don't
  clash. Search and booking read the same ledger, so search only shows rooms that are bookable.
- **Concurrency.**
  - `reserve` checks every night and then holds every night while holding that room type's monitor. It is
    all-or-nothing and two guests can never take the last unit. Room types don't contend with each other.
  - Payment and cancellation of the same booking are serialised with a `LockManager` (striped in-memory locks
    today, a distributed lock later). Without that, a cancellation could slip in between charging the card and
    confirming the booking.
- **Inventory is held when the booking is created.** The guest is guaranteed the room and the quoted price
  while they pay. A declined payment or a cancellation releases the room.
- **Strategies at the extension points.** Each of these is an interface in the domain, and adding a new
  implementation doesn't change existing code:
  - `PaymentProcessor`: card, UPI and wallet. A new processor bean is picked up automatically by
    `PaymentProcessorRegistry`.
  - `PropertyFilterFactory`: each filter reads its own query parameters, so a new filter needs no change to the
    controller or `SearchService`.
  - `CancellationPolicy`: the tiered policy's thresholds are configurable.
  - `PricingStrategy`: `base`, or `weekend-surcharge` via `hotel.pricing.strategy`.
  - `PaymentGateway`: mocked.
- **The payment outcome drives booking state.** `PaymentService` records the `Payment` and then calls
  `booking.confirm(...)` or `booking.markPaymentFailed()`.
- **Idempotent payments.** A retry with the same `Idempotency-Key` returns the original payment without charging
  again. Reusing a key for a different booking is rejected.
- **Rich value objects.**
  - `Money` uses `BigDecimal` with a currency and refuses to mix currencies.
  - `DateRange` checks that check-out is after check-in and that the stay is at most 30 nights.
  - Entities check their own invariants, such as guests ≤ occupancy and a star rating of 1–5.
- **Time is injected.** A `java.time.Clock` makes the cancellation-window and past-date rules testable.

## Assumptions

- One booking reserves one room of one room type. The price is per room per night, not per guest.
- There is a single currency per room type (default INR). The time zone (`Asia/Kolkata`) and the 12:00 check-in
  time are configurable.
- Default refund policy:

  | Notice before check-in | Refund |
  |---|---|
  | 48 h or more | 100% |
  | 24–48 h | 50% |
  | Less than 24 h | 0% |

  An unpaid booking can be cancelled at no charge.
- A declined payment ends the booking (`PAYMENT_FAILED`) and frees the room. The guest books again to retry.
- Card payments use a token from a client-side vault; raw card numbers never reach this service.
- Guests are identified only by name, and owners and properties can't be edited. Authentication is out of scope.

## Testing

The suite has 81 tests and runs with `./mvnw verify`.

- **Domain:** `Money`, `DateRange`, the full `BookingStatus` transition table and `Booking` invariants.
- **Inventory:**
  - Overlapping vs back-to-back stays.
  - A failed reservation holds nothing.
  - Release.
  - 32 threads racing for the last room produce exactly one winner. `BookingServiceTest` runs the same race
    through the service.
- **Services:** they are wired with the real in-memory adapters and a fixed clock (`TestHotel`), so tests check
  behaviour rather than mock interactions:
  - Search: availability, occupancy, every filter, and a cancelled booking reappearing.
  - Payment: success, decline, idempotent retry with exactly one gateway charge, and validation before any charge.
  - Cancellation: each refund tier, unpaid cancellation, and double-cancel rejection.
- **Policies and filters:** refund tiers at their boundaries, weekend pricing, and filter parsing.
- **End to end:** `BookingFlowIntegrationTest` runs onboard → search → book → double-book 409 → pay → replay →
  cancel → 409 on re-cancel → search again over HTTP with MockMvc.

## With more time

- Expire `PENDING_PAYMENT` holds after N minutes with a scheduled job, so abandoned checkouts release rooms.
- A JPA/H2 (then Postgres) adapter, using row locks or optimistic locking on an inventory table instead of
  monitors, and a unique index on the idempotency key. For multiple instances, a distributed `LockManager`.
- Outbox/domain events (`BookingConfirmed`, `BookingCancelled`) for notifications and analytics, plus async
  gateway webhooks instead of a synchronous charge.
- Multi-room and multi-room-type bookings, rate plans, taxes, and per-property cancellation policies chosen by a
  policy resolver.
- Search: pagination, sorting options, geo/locality hierarchies, and a read model so search doesn't scan a
  city's properties.
- Editing or deactivating properties, auth for owners and guests, and rate limiting.
