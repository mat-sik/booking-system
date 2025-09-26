CREATE KEYSPACE IF NOT EXISTS booking_system
            WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS booking_system.bookings_by_service_and_date
(
    service_id uuid,
    date       date,
    booking_id uuid,
    user_id    uuid,
    start      int,
    end        int,
    PRIMARY KEY ((service_id, date), booking_id)
);

CREATE TABLE IF NOT EXISTS booking_system.bookings_by_user
(
    user_id    uuid,
    service_id uuid,
    date       date,
    booking_id uuid,
    start      int,
    end        int,
    PRIMARY KEY ((user_id), service_id, date, booking_id)
);