# Shipping on the Air — User Stories

**Drone Delivery System**

Personas:
- **Sender**: a registered user who originates a delivery request.
- **Admin**: an already-registered user with administrative privileges who monitors the fleet and manages delivery scheduling.
- **Drone (external system)**: the physical device that emits telemetry (position & status); it is an actor, not a human user, and does not authenticate as a user.

> **Note on terminology.** Throughout the user stories and their acceptance tests, delivery states use the canonical `DeliveryStatus` values — `REQUESTED`, `VALIDATED`, `REJECTED`, `SCHEDULED`, `ASSIGNED`, `IN_PROGRESS`, `DELIVERED`, `CANCELLED` — and drone states use the canonical `DroneStatus` values — `AVAILABLE`, `RESERVED`, `ASSIGNED`, `IN_DELIVERY`, `ARRIVED`, `OUT_OF_SERVICE`. These are the same values defined in the domain model.

---

## Registration and Access

```
As a new user,
I want to register an account on the delivery service
so that I can request drone deliveries.
```

```
As a registered user,
I want to log into the system
so that I can create deliveries and track them.
```

```
As an Admin,
I want to log into the system with my existing account
so that I can manage scheduling and monitor the fleet.
```

---

## Delivery Request Creation

```
As a logged-in Sender,
I want to create a delivery request specifying pickup location and destination
so that my package can be shipped from one place to another.
```

```
As a logged-in Sender,
I want to specify the weight of my package
so that the system can assign a drone able to carry it.
```

```
As a logged-in Sender,
I want to choose between immediate or scheduled delivery
so that my package is picked up either now or at a date/time I decide.
```

```
As a logged-in Sender,
I want to define a maximum delivery time (deadline)
so that I am only committed to a shipment that arrives within my deadline.
```

```
As a logged-in Sender,
I want to know immediately whether my request is accepted or rejected when I submit it
so that I do not commit to a shipment the system cannot fulfil.
```

---

## Delivery Tracking

```
As a logged-in Sender,
I want to track my delivery in real time
so that I know the current position of my package.
```

```
As a logged-in Sender,
I want to see the estimated time remaining for my delivery
so that I know when the package will arrive.
```

---

## Delivery Cancellation

```
As a logged-in Sender,
I want to cancel a delivery request before it is in flight
so that I can stop a shipment I no longer need.
```

---

## Fleet Monitoring (Admin)

```
As an Admin,
I want to monitor the position of every drone in real time
so that I have an overview of the fleet on the map.
```

```
As an Admin,
I want to see the operational status of every drone, including whether it is carrying a package,
so that I can supervise fleet operations.
```

---

## Scheduling Management (Admin)

```
As an Admin,
I want to manage the scheduling of delivery requests for each drone
so that the daily route of every drone is organised efficiently.
```

```
As an Admin,
I want a scheduled delivery to reserve a drone for the requested time slot
so that the drone is available to pick up the package when the Sender requested it.
```

---

## Telemetry (Drone external system)

```
As the Shipping on the Air system,
I want to ingest position and status updates emitted by the drones
so that delivery tracking and fleet monitoring reflect the real state of the fleet.
```

---

## Quality Features (Non-Functional)

```
As a Sender,
I want to use the system from any modern web browser
so that I can request deliveries without installing anything.
```

```
As a Sender,
I want the tracking view to reflect drone movement with low latency
so that the position and the estimated time remaining I see are trustworthy.
```

```
As the system owner,
I want each bounded context to scale and fail independently
so that a problem in one context (e.g. telemetry ingestion) does not bring down the others.
```

```
As the system owner,
I want telemetry ingestion to sustain frequent updates from the whole fleet
so that real-time tracking and monitoring remain responsive as the fleet grows.
```

```
As a user,
I want my credentials and my deliveries to be protected
so that only I (or an authorised Admin) can access my data.
```
