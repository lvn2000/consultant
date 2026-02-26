# Specialist Availability Feature

## Overview

The specialist availability feature allows clients to see **available time slots** when booking consultations. The system intelligently filters specialist availability by excluding time slots that are already booked by other clients.

This prevents double-booking and ensures specialists don't accept overlapping consultation requests.

## Architecture

### Components

#### 1. **Domain Model** (`core`)
- **SpecialistAvailability**: Represents a specialist's available time slot
  - `id: UUID` - Unique identifier
  - `specialistId: UUID` - The specialist who is available
  - `dayOfWeek: Int` - 1=Monday, 7=Sunday
  - `startTime: LocalTime` - When availability starts (e.g., 09:00)
  - `endTime: LocalTime` - When availability ends (e.g., 17:00)

#### 2. **Service Layer** (`core/service/AvailabilityService.scala`)
The service calculates **free time slots** by:
1. Fetching specialist's availability windows (e.g., Mon-Fri 9am-5pm)
2. Fetching specialist's existing consultations
3. Subtracting booked ranges from available time windows
4. Returning only gaps large enough for requested duration

**Key Methods:**
```scala
// Get all free slots for a date with minimum duration
getAvailableSlotsForDate(date: LocalDate, slotDuration: Int): List[(LocalTime, LocalTime)]

// Check if a specific time slot is bookable
isTimeSlotAvailable(date: LocalDate, startTime: LocalTime, durationMinutes: Int): Boolean

// Core algorithm: calculate free time gaps
calculateFreeSlots(dayStart: LocalTime, dayEnd: LocalTime, 
                   bookedRanges: List[(LocalTime, LocalTime)], 
                   slotDuration: Int): List[(LocalTime, LocalTime)]
```

#### 3. **API Endpoints** (`api/routes/AvailabilitySlotRoutes.scala`)

**GET /api/specialists/{specialistId}/availability/slots**
- Query parameters:
  - `date` (required): YYYY-MM-DD format (e.g., "2026-02-10")
  - `durationMinutes` (optional): Minimum duration, defaults to 60
- Returns: List of available time windows
```json
{
  "specialistId": "uuid",
  "date": "2026-02-10",
  "slots": [
    { "startTime": "09:00", "endTime": "10:00", "durationMinutes": 60 },
    { "startTime": "11:00", "endTime": "12:00", "durationMinutes": 60 },
    { "startTime": "14:00", "endTime": "17:00", "durationMinutes": 180 }
  ],
  "hasAvailableSlots": true
}
```

**POST /api/specialists/{specialistId}/availability/check**
- Body:
```json
{
  "specialistId": "uuid",
  "date": "2026-02-10",
  "startTime": "14:00",
  "durationMinutes": 60
}
```
- Returns: `{ "available": true/false }`

#### 4. **Database** (`data/repository/PostgresAvailabilityRepository.scala`)
Table: `specialist_availability`
```sql
CREATE TABLE specialist_availability (
  id UUID PRIMARY KEY,
  specialist_id UUID NOT NULL,
  day_of_week INT NOT NULL,  -- 1=Monday, 7=Sunday
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

#### 5. **Frontend Integration** (`client-app/components/ConsultationsBookTab.vue`)
The client-app consultation booking form now:
1. **Auto-fetches available slots** when specialist and date are selected
2. **Displays slot buttons** for easy selection
3. **Validates** selected time against available slots before booking
4. **Shows loading/error states** gracefully

## How It Works

### Booking Flow

```
Client selects specialist
    ↓
Client selects date
    ↓
Frontend calls: GET /api/specialists/{id}/availability/slots?date=...&durationMinutes=...
    ↓
Backend calculates free slots (availability - booked consultations)
    ↓
Frontend displays available time buttons
    ↓
Client clicks desired slot
    ↓
Client submits consultation request with selected time
```

### Example Scenario

**Specialist Setup:**
- Available: Mon-Fri 9am-5pm

**Existing Bookings:**
- Mon 10:00-11:30 (90 min consultation)
- Mon 2:00-3:30 PM (90 min consultation)

**Available Slots for Monday (60 min):**
- 9:00-10:00
- 11:30-2:00 (contains 150 min, so slots: 11:30-12:30, 12:30-1:30, 1:30-2:00)
- 3:30-5:00

## Configuration

### Add Availability (Specialist-App)
The specialist-app has a dedicated "Availability" section where specialists can:
1. Select day of week (Monday-Sunday)
2. Set start time (e.g., 09:00)
3. Set end time (e.g., 17:00)
4. Add multiple time windows per day
5. Edit/delete existing availability

### Default Availability Duration
When a client hasn't specified duration, the system defaults to **60 minutes** for slot availability checks.

## Time Zone Handling

- **Storage**: All times stored as `LocalTime` (no timezone)
- **Conversion**: Uses `ZoneId.systemDefault()` to convert `Instant` consultations to `LocalDate/LocalTime`
- **Important**: Ensure server and database use consistent timezone settings

## Edge Cases Handled

✅ **Specialist with no availability defined**
- Returns empty slots list

✅ **All time slots booked**
- Returns empty slots list with message

✅ **Partial overlap**
- Only splits free time into valid-sized slots

✅ **Back-to-back bookings**
- Correctly identifies gaps between consultations

✅ **Same-day overlapping consultations**
- Correctly merges overlapping booked ranges before calculation

## API Testing

### Using cURL

**Get available slots:**
```bash
curl "http://localhost:8080/api/specialists/{specialistId}/available-slots?date=2026-02-10&durationMinutes=60"
```

**Check specific time:**
```bash
curl -X POST http://localhost:8080/api/specialists/{specialistId}/check-availability \
  -H "Content-Type: application/json" \
  -d '{
    "specialistId": "{uuid}",
    "date": "2026-02-10",
    "startTime": "14:00",
    "durationMinutes": 60
  }'
```

### Using Swagger UI
Navigate to `http://localhost:8080/docs` and test endpoints interactively.

## Frontend Usage

### Auto-loading Slots
The form automatically loads available slots when:
- Specialist is selected
- Date is selected
- Duration changes

### Manual Loading
You can also manually call:
```typescript
const loadAvailableSlots = async () => {
  // Fetches available slots based on current form values
}
```

### Displaying Slots
Slots appear as a grid of buttons showing time ranges:
- Hovering shows blue highlight
- Clicking selects the slot and fills the time input
- Loading spinner appears while fetching
- Error message shows if no slots available

## Implementation Files

| File | Purpose |
|------|---------|
| `core/src/main/scala/com/consultant/core/service/AvailabilityService.scala` | Slot calculation logic |
| `core/src/main/scala/com/consultant/core/ports/AvailabilityRepository.scala` | Port interface |
| `data/src/main/scala/com/consultant/data/repository/PostgresAvailabilityRepository.scala` | Database access |
| `api/src/main/scala/com/consultant/api/routes/AvailabilitySlotRoutes.scala` | REST endpoints |
| `api/src/main/scala/com/consultant/api/dto/AvailableSlotsDto.scala` | Request/response DTOs |
| `specialist-app/pages/main.vue` | Availability management UI |
| `client-app/pages/main.vue` | Booking form with slot selection |

## Compilation & Deployment

### Build Backend
```bash
cd backend
sbt compile        # Verify compilation
sbt scalafmtAll    # Format code
```

### Deploy
```bash
docker-compose up --build    # Rebuild with new code
```

## Future Enhancements

- [ ] Break times within availability windows
- [ ] Timezone-aware availability (per specialist)
- [ ] Recurring availability patterns
- [ ] Block out unavailable dates (holidays, sick days)
- [ ] Minimum advance booking notice
- [ ] Buffer time between consultations
- [ ] Availability templates (copy previous week)

## Troubleshooting

### No slots appearing
1. Check specialist has availability defined (specialist-app)
2. Verify date is in the future
3. Check browser console for API errors
4. Verify backend is running: `curl http://localhost:8080/api/specialists/{id}/available-slots?date=2026-02-10`

### Wrong slots showing
1. Verify specialist availability covers selected date
2. Check existing consultations (may be booking the slot)
3. Verify duration parameter is correct

### Compilation errors
1. Ensure all files in correct packages:
   - `com.consultant.core.service.AvailabilityService`
   - `com.consultant.core.ports.AvailabilityRepository`
   - `com.consultant.data.repository.PostgresAvailabilityRepository`
   - `com.consultant.api.routes.AvailabilitySlotRoutes`
   - `com.consultant.api.dto.AvailableSlotsDto`
2. Run `sbt clean compile` to rebuild from scratch

---

**Status**: ✅ Complete and tested
**Last Updated**: Feb 3, 2026
**Compiled**: Successfully
