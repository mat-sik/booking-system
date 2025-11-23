from locust import HttpUser, task, between
import random
from datetime import datetime, timedelta
import uuid


def generate_dates(year, month, num_days):
    return [(datetime(year, month, 1) + timedelta(days=i)).strftime("%Y-%m-%d")
            for i in range(num_days)]


class BookingAPIUser(HttpUser):
    wait_time = between(1, 3)

    num_services = 5
    service_ids = [str(uuid.uuid4()) for _ in range(num_services)]

    year = 2024
    month = 12
    num_days = 15
    dates = generate_dates(year, month, num_days)

    def on_start(self):
        self.user_id = str(uuid.uuid4())
        self.created_bookings = []
        self.fetched_bookings = []
        self.available_slots = {}

    def get_random_date(self):
        return random.choice(self.dates)

    def get_random_time_slot(self):
        start = random.randint(540, 960)
        end = start + random.choice([30, 60, 90])
        return start, end

    @task(5)
    def get_available_time_ranges(self):
        service_id = random.choice(self.service_ids)
        date = self.get_random_date()
        service_duration = random.choice([30, 60, 90])

        with self.client.get(
                "/bookings/available",
                params={
                    "serviceId": service_id,
                    "date": date,
                    "serviceDuration": service_duration
                },
                catch_response=True,
                name="/bookings/available"
        ) as response:
            if response.status_code == 200:
                try:
                    time_ranges = response.json()
                    self.available_slots = {}
                    if time_ranges:
                        key = f"{service_id}_{date}"
                        self.available_slots[key] = time_ranges
                except:
                    pass
                response.success()
            else:
                response.failure(f"Got status code {response.status_code}")

    @task(3)
    def create_booking(self):
        service_id = random.choice(self.service_ids)
        date = self.get_random_date()

        key = f"{service_id}_{date}"
        if key in self.available_slots and self.available_slots[key]:
            slot = random.choice(self.available_slots[key])
            start = slot["start"]
            end = slot["end"]
        else:
            start, end = self.get_random_time_slot()

        payload = {
            "date": date,
            "serviceId": service_id,
            "userId": self.user_id,
            "start": start,
            "end": end
        }

        with self.client.post(
                "/bookings/create",
                json=payload,
                catch_response=True,
                name="/bookings/create"
        ) as response:
            if response.status_code == 200:
                self.created_bookings.append({
                    "date": date,
                    "serviceId": service_id,
                    "bookingId": str(uuid.uuid4()),
                    "userId": self.user_id
                })
                if key in self.available_slots:
                    try:
                        self.available_slots[key].remove(slot)
                    except:
                        pass
                response.success()
            elif response.status_code == 400:
                response.success()
            else:
                response.failure(f"Got unexpected status code {response.status_code}")

    @task(2)
    def get_user_bookings(self):
        limit = random.choice([5, 10, 20])

        with self.client.get(
                "/bookings",
                params={
                    "userId": self.user_id,
                    "limit": limit
                },
                catch_response=True,
                name="/bookings (list)"
        ) as response:
            if response.status_code == 200:
                try:
                    bookings = response.json()
                    if bookings:
                        self.fetched_bookings = bookings
                except:
                    pass
                response.success()
            else:
                response.failure(f"Got status code {response.status_code}")

    @task(2)
    def get_specific_user_booking(self):
        if self.fetched_bookings:
            booking = random.choice(self.fetched_bookings)
            service_id = booking["serviceId"]
            date = booking["date"]
            booking_id = booking["bookingId"]
        else:
            service_id = random.choice(self.service_ids)
            date = self.get_random_date()
            booking_id = str(uuid.uuid4())

        with self.client.get(
                "/bookings/user",
                params={
                    "serviceId": service_id,
                    "date": date,
                    "userId": self.user_id,
                    "bookingId": booking_id
                },
                catch_response=True,
                name="/bookings/user"
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Got status code {response.status_code}")

    @task(1)
    def delete_booking(self):
        if self.created_bookings:
            booking = random.choice(self.created_bookings)
            payload = booking
        else:
            payload = {
                "date": self.get_random_date(),
                "serviceId": random.choice(self.service_ids),
                "bookingId": str(uuid.uuid4()),
                "userId": self.user_id
            }

        with self.client.post(
                "/bookings/delete",
                json=payload,
                catch_response=True,
                name="/bookings/delete"
        ) as response:
            if response.status_code in [200, 400]:
                if response.status_code == 200 and payload in self.created_bookings:
                    self.created_bookings.remove(payload)
                response.success()
            else:
                response.failure(f"Got status code {response.status_code}")


class HeavyLoadUser(HttpUser):
    wait_time = between(0.5, 1.5)

    num_services = 3
    service_ids = [str(uuid.uuid4()) for _ in range(num_services)]

    year = 2024
    month = 12
    num_days = 7
    dates = generate_dates(year, month, num_days)

    def on_start(self):
        self.user_id = str(uuid.uuid4())

    @task
    def rapid_availability_check(self):
        service_id = random.choice(self.service_ids)
        date = random.choice(self.dates)

        self.client.get(
            "/bookings/available",
            params={
                "serviceId": service_id,
                "date": date,
                "serviceDuration": 30
            },
            name="/bookings/available (heavy)"
        )
