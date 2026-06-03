# Shipping on the Air — User Stories

**Drone Delivery System**

Personas:
- **Sender**: user that want to send a package.
- **Admin**: user that tracks all drones, manage the deliveries and manage users.

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
so that I can create deliveries, track them and access my profile.
```

---

## Delivery Request Creation

```
As a logged-in Sender,
I want to create a delivery request specifying pickup and destination addresses
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
I want to define a maximum delivery time
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
I want to cancel a delivery request
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

## User Management (Admin)

```
As an Admin,
I want to manage users and their profiles
so that I can administer who has access to the service.
```

---

## Quality Features

```
As a Sender,
I want to use the system from any modern web browser
so that I can request deliveries without installing anything.
```
