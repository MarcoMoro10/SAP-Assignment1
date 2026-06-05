#### Software Architecture and Platforms - a.y. 2025-2026
## Drone Delivery Service Case Study - Ubiquitous Language

### Glossary

#### General

- **Drone Delivery Service**
  - the online system that allows registered users to request, schedule and track parcel deliveries performed by a fleet of autonomous drones
  - it is organized into three bounded contexts: **Users Context**, **Deliveries Context** and **Fleet Context**

---

### Users Context

- **User**
  - the actor that accesses the drone delivery service
- **Account**
  - in order to access the features of the service, a user must register into the system, creating an account by filling a registration form
  - users that have an account are also referred as registered users
- **Sender**
  - a registered user who acts as the originator of a delivery request
- **Register Form**
  - the form that a user fills in to provide the data needed to create a new account
- **To register an account**
  - the action by which a user submits the register form to create a new account in the system
  - it produces either an **Account Created** event (success) or a **Registration Failed** event (failure)
- **To login**
  - the action that a registered user must perform, specifying credentials, in order to start a session to interact with the service
  - it produces either a **User Logged In** event (success) or a **Login Failed** event (failure)
- **Domain Events**
  - **Account Created**: a new account has been successfully created
  - **Registration Failed**: the registration of a new account could not be completed
  - **User Logged In**: a registered user has successfully started a session
  - **Login Failed**: a login attempt did not succeed

---

### Deliveries Context

- **Delivery**
  - the core aggregate of this context: the transport of a parcel from the sender to a destination performed by a drone
  - a delivery has a lifecycle that goes from request and validation, through scheduling and execution, up to completion
- **Package**
  - the parcel to be delivered, characterized by its **weight**
  - the weight is one of the attributes used to determine whether a suitable drone can carry the package
- **Delivery Request**
  - the request, issued by a sender, to perform a new delivery
  - it is created through the **Create Delivery Request** action and can later be **Cancelled**
  - it carries the data needed to evaluate and perform the delivery:
    - **Package**: the parcel to deliver, with its **weight**
    - **Pickup Location**: the place where the package must be collected
    - **Destination**: the place where the package must be delivered
    - **Requested Date/Time**: when the delivery should take place; it can be **Immediate** (as soon as possible) or **Scheduled** (a specific future date/time)
    - **Deadline**: the maximum amount of time within which the delivery must be completed
- **Estimated Time Remaining (ETR)**
  - the value object that expresses how much time is still expected before a delivery in progress is completed
  - it is computed as a function of the drone's **current position**, the delivery **destination** and the drone's travel speed
  - it is part of the **Delivery Tracking View** and is the value the sender reads while tracking the package
  - it is **recalculated every time a Position Updated event is received** from the Fleet Context, producing an **Estimated Time Updated** event
  - when the delivery is completed (drone arrived), the Estimated Time Remaining is **zero**
- **To create a delivery request**
  - the action a sender performs to ask the system for a new delivery
  - it produces a **Delivery Request Created** event
- **To cancel a request**
  - the action a sender performs to withdraw a delivery request that has **not yet started** (early cancellation, e.g. while still in the request/validation phase)
  - it produces a **Delivery Request Cancelled** event
- **To cancel a delivery**
  - the action a sender performs to cancel a delivery that has been **assigned but has not yet started its flight** (late cancellation, status *Assigned*)
  - it produces a **Delivery Cancelled** event and triggers the **release of the drone reservation** in the Fleet Context
  - a delivery already **in progress** (in flight) **cannot** be cancelled
- **To validate a delivery**
  - the action by which the system checks whether a delivery request can be accepted
  - the validation evaluates all the attributes carried by the delivery request, in particular:
    - **Weight feasibility**: whether the package weight is within the payload capacity of an available drone
    - **Route feasibility**: whether the distance between pickup location and destination can be covered by a drone
    - **Time feasibility**: whether a suitable time slot is available for the requested date/time (immediate or scheduled)
    - **Deadline feasibility**: whether the estimated delivery duration fits within the requested deadline
  - all these checks are **confluent**: regardless of which specific condition fails, a failed validation produces a single, unified **Validation Delivery Rejected** event
  - it produces either a **Validation Delivery Passed** event or a **Validation Delivery Rejected** event
- **Delivery Tracking View**
  - the read model / view that lets a sender follow the progress of a delivery
- **To track a delivery**
  - the action a sender performs to monitor a delivery in progress
- **To schedule a delivery**
  - the action by which the system schedules a validated delivery, producing a **Delivery Scheduled** event
- **To complete a delivery**
  - the action by which the system marks a delivery as finished, producing a **Delivery Completed** event
- **To request fleet feasibility**
  - the action by which the system asks the Fleet Context whether the delivery can be served by an available drone
- **Policies**
  - **Whenever Delivery Duration Estimated, then check it against the requested deadline**
  - **Whenever Position Updated, then recalculate the Estimated Time Remaining and emit Estimated Time Updated**
  - **Whenever drone assigned, then delivery began**
  - **Whenever Drone Arrived, then complete the delivery**
  - **Whenever a Drone goes Out Of Service during a delivery, then abolish (cancel) that delivery** *(scope decision: reassignment/abort is out of scope for the prototype — a drone failure simply abolishes the affected delivery)*
  - **Whenever a sender cancels an assigned delivery, then cancel the delivery and release the drone reservation**
- **Domain Events**
  - **Delivery Request Created**: a sender has issued a new delivery request
  - **Delivery Request Cancelled**: a delivery request has been withdrawn before execution (early cancellation)
  - **Delivery Cancelled**: an assigned delivery has been cancelled before its flight started — either on the sender's request or because the assigned drone went out of service; it triggers the release of the drone reservation
  - **Validation Delivery Passed**: a delivery request passed validation
  - **Validation Delivery Rejected**: a delivery request failed validation; this single event represents the rejection regardless of the failing condition (weight, route, time slot or deadline)
  - **Delivery Scheduled**: a validated delivery has been scheduled
  - **Delivery Began**: the execution of a delivery has started
  - **Estimated Time Updated**: the estimated time remaining of a delivery has been recalculated (typically after a Position Updated event)
  - **Delivery Completed**: a delivery has been successfully finished
  - **Drone Out Of Service During Delivery**: the assigned drone became unavailable while a delivery was in progress, causing the delivery to be abolished

---

### Fleet Context

- **Drone**
  - the core aggregate of this context: an autonomous vehicle of the fleet that physically performs deliveries
  - a drone has a lifecycle through availability checking, reservation, assignment, execution and arrival
  - it is characterized by a **payload capacity** (maximum transportable weight), against which the package weight is checked during validation
- **Admin**
  - a registered user with administrative privileges who monitors and manages the fleet
- **Fleet Monitoring View**
  - the read model / view that lets an admin observe the state of the whole fleet
- **To track drones**
  - the action an admin performs to monitor the drones of the fleet
- **To check drone availability**
  - the action by which the system verifies whether drones are available, producing a **Drone Availability Checked** event
- **To check slot feasibility**
  - the action by which the system verifies whether a delivery time slot can be served
- **To reserve a slot for a drone**
  - the action by which the system reserves a delivery slot on a drone
  - it produces either a **Drone Reserved** event or a **No Drone Available For Slot** event
- **To release a reservation**
  - the action by which the system frees a previously reserved slot, making the drone available again
  - it is triggered when an assigned delivery is cancelled (late cancellation) or abolished (drone out of service)
  - it produces a **Reservation Released** event
- **To estimate delivery duration**
  - the action by which the system computes the expected duration of a delivery, producing a **Delivery Duration Estimated** event
- **To assign a drone**
  - the action by which the system assigns a drone to a validated and scheduled delivery
  - it produces either a **Drone Assigned** event or a **Drone Not Available** event
- **Policies**
  - **Whenever feasibility requested, then check availability**
  - **Whenever a delivery is scheduled, then reserve a slot**
  - **Whenever Validation Delivery Passed, then assign a drone**
  - **Whenever Validation Delivery Rejected, then reject the request**
  - **Whenever a delivery is cancelled or abolished, then release the drone reservation**
  - **Whenever a drone goes Out Of Service during a delivery, then mark the drone unavailable and notify the Deliveries Context**
- **Domain Events**
  - **Drone Availability Checked**: the availability of drones has been verified
  - **Drone Reserved**: a slot on a drone has been successfully reserved
  - **No Drone Available For Slot**: no drone could be reserved for the requested slot
  - **Reservation Released**: a previously reserved slot on a drone has been freed, making the drone available again
  - **Delivery Duration Estimated**: the duration of a delivery has been estimated
  - **Drone Assigned**: a drone has been assigned to a delivery
  - **Drone Not Available**: no drone could be assigned to a delivery
  - **Status Updated**: the status of a drone has been updated
  - **Position Updated**: the position of a drone has been updated
  - **Drone Arrived**: the drone reached the delivery destination
  - **Drone Out Of Service**: a drone became unavailable; if it occurs during a delivery, it causes that delivery to be abolished
