# Booking Service

This project implements a booking system that allows users to reserve services for specific dates and times. Users can
choose a service, specify a desired date, and book a time slot for a specified duration (e.g., booking a service on
10.10.2024 from 13:00 for 1 hour).

This project is composed of multiple microservices that work together to implement a booking system. It
follows the **CQRS** (Command Query Responsibility Segregation) and **Data Sourcing** patterns. The **Data Sourcing**
pattern and its associated log are used to control concurrent bookings for the same date and time by multiple users,
ensuring data consistency and preventing conflicts.

## Request Types

### Command Requests

Command requests are used to modify the state of the MongoDB datastore. In other words, they are responsible for
creating and deleting bookings.

### Query Requests

Query requests are used to retrieve the current state of the MongoDB datastore. They provide users with a point in time
view of available and booked time slots. This view might be stale the moment it is viewed by users.

## Diagram

![Application Architecture](./diagrams/bookings.drawio.png)

## Command Service

The **Command Service** consumes records from **Kafka** Topic Partitions, with each partition acting as a **Data
Sourcing**
log, serving as the authoritative source of truth for the application. This log preserves the chronological order of
booking commands, which is essential for ensuring fairness and consistency. By maintaining this order, it prevents race
conditions and conflicts between concurrent booking requests.

As the records are processed, the **Command Service** updates the state representation in **MongoDB**, creating
queryable data view for interested parties.

### Topic Partitioning

The partition key is based on the **dd/mm/yyyy** format, ensuring that all bookings for a specific date are routed to
the same partition. This approach preserves the chronological order of bookings for that date, enabling the system to
accurately determine which user booked first.

## Query Service

The **Query Service** allows users to see their bookings. This serves as a way for checking whether a given booking
request was successful.

The service is also used for providing the users with information which bookings are currently not booked. This
information might very quickly become stale, but this is acceptable.

The **Booking Service** interacts with **Query Service** to perform users query requests.

## Booking Service

The **Booking Services** serves as an entry point for the application. It provides REST API to interact with the system.

## MongoDB data model

`Bookings Collection`

```json
{
  "_id": "objectid",
  "date": "string",
  "serviceId": "objectid",
  "bookings": [
    {
      "_id": "objectid",
      "userId": "objectid",
      "start": "date",
      "end": "date"
    }
  ]
}
```

The `date` field represents the date in the format: **dd/mm/yyyy** the same one as in the **Kafka** partition key.

The field In combination with the`serviceId` field, uniquely identifies a list of bookings of a services for a
single day.

The `bookings` field contains a list with objects representing all present bookings.

## Command Requests

### Inserting new booking

To fulfill the request of creating a new booking, the availability of the booking needs to be checked.

Given input parameters:

- `date`- **dd/mm/yyyy**
- `serviceId` - id of the servcie for the booking
- `start`- start time of the booing
- `end`  - end time of the booing
- `userId` - id of the user issuing the booking
- `_id` - new objectid of the booking.

For document matching `date` and `serviceId`

For every `booking` : `bookings`

Check `input.start < booking.end && input.end > booking.start`

If none is found, push the input object into `bookings` array.

**Implementation**:

```
const newBooking = {
  _id: new ObjectId(),
  userId: new ObjectId(),
  start: ISODate("2024-12-03T10:00:00Z"),
  end: ISODate("2024-12-03T11:00:00Z"),
};

db.collection.updateOne(
  {
    date: "03/12/2024",
    serviceId: ObjectId("your-service-id"),
    $expr: {
      $not: {
        $anyElementTrue: {
          $map: {
            input: "$bookings",
            in: {
              $and: [
                { $lt: [newBooking.start, "$$this.end"] },
                { $gt: [newBooking.end, "$$this.start"] },
              ],
            },
          },
        },
      },
    },
  },
  {
    $push: { bookings: newBooking },
  },
  { upsert: true }
);
```

For the best performance, `date` and `serviceId` fields should be used in a composite index, to allow fast fetching of
the documents.

The check for overlap of bookings will be performed in memory. There won't be that many bookings in a single day, so
it is acceptable.

The `upsert: true` option is used, to create the document if no document with a given `date` and `serviceId` fields
exist.

### Deleting an existing booking

Deleting a booking is simpler than inserting one because there is no need to check for conflicts.

The parameters for `date`, `serviceId` and `_id` of the booking are needed.

```
const bookingIdToDelete = new ObjectId("booking-id-to-delete");

db.collection.updateOne(
  {
    date: "03/12/2024",
    serviceId: ObjectId("your-service-id"),
  },
  {
    $pull: { bookings: { _id: bookingIdToDelete } }
  }
);
```

### Updating an existing booking

To fulfill this requirement, the booking should first be deleted, and then a new booking should be created. However,
there is no guarantee that the creation will succeed.
