# Quick Reference: Specialist Availability Feature

## 🚀 Quick Start

### For Clients
1. Go to **My Consultations** tab
2. Select specialist and date
3. Available time slots appear automatically
4. Click a slot to book that time

### For Specialists
1. Go to **Availability** menu item
2. Select day of week (Mon-Sun)
3. Set start/end times (e.g., 09:00 - 17:00)
4. Click "Add Availability"

## 📍 API Endpoints

### Get Available Slots
```bash
GET /api/specialists/{specialistId}/availability/slots?date=2026-02-10&durationMinutes=60

Response:
{
  "specialistId": "uuid...",
  "date": "2026-02-10",
  "slots": [
    { "startTime": "09:00", "endTime": "10:00", "durationMinutes": 60 },
    { "startTime": "11:30", "endTime": "12:30", "durationMinutes": 60 }
  ],
  "hasAvailableSlots": true
}
```

### Check Specific Time
```bash
POST /api/specialists/{specialistId}/availability/check

Body:
{
  "specialistId": "uuid...",
  "date": "2026-02-10",
  "startTime": "14:00",
  "durationMinutes": 60
}

Response:
{ "available": true }
```

## 📁 Key Files

| File | Purpose |
|------|---------|
| `core/src/main/scala/com/consultant/core/service/AvailabilityService.scala` | Calculates free slots |
| `api/src/main/scala/com/consultant/api/routes/AvailabilitySlotRoutes.scala` | REST API endpoints |
| `data/src/main/scala/com/consultant/data/repository/PostgresAvailabilityRepository.scala` | Database access |
| `client-app/components/ConsultationsBookTab.vue` | Booking form with slot selection |
| `specialist-app/components/AvailabilitySection.vue` | Availability management |

## 🛠️ Development

### Build
```bash
cd backend
sbt compile          # Verify
sbt scalafmtAll     # Format
sbt test            # Test
```

### Run
```bash
bash start-https.sh
# Backend: http://localhost:8090
# Client: http://localhost:3000
# Admin: http://localhost:3001
# Specialist: http://localhost:3002
```

### Test API
```bash
# Get slots
curl "http://localhost:8090/api/specialists/{id}/availability/slots?date=2026-02-10"

# Check availability
curl -X POST http://localhost:8090/api/specialists/{id}/availability/check \
  -H "Content-Type: application/json" \
  -d '{"specialistId":"{id}","date":"2026-02-10","startTime":"14:00","durationMinutes":60}'
```

## 💡 How It Works

```
Specialist defines availability:
  "Monday 9am-5pm"
        ↓
Client books consultation:
  "Monday 2pm-3:30pm"
        ↓
Backend calculates free slots:
  9-9:30, 9:30-10, ... (gaps around the 2-3:30 booking)
        ↓
Client sees only free time: "Select 10am", "Select 11am", etc.
```

## 🔧 Common Tasks

### Add New Specialist Availability
```
1. Login as Specialist
2. Click "Availability" menu
3. Select Monday
4. Set "09:00" to "17:00"
5. Click "Add Availability"
```

### Check Available Slots
```
1. Login as Client
2. Click "My Consultations"
3. Select Specialist
4. Select Date
5. See available slots appear
```

### Debug: No Slots Showing?
```
1. Verify specialist has availability (go to specialist-app)
2. Check date is not in the past
3. Check existing consultations (might block all slots)
4. Check browser console for errors
5. Refresh page
```

## 📊 Data Flow

```
Frontend (Nuxt 3)
    ↓
GET /api/specialists/{id}/available-slots
    ↓
Backend (Http4s + Tapir)
    ↓
AvailabilitySlotRoutes
    ↓
AvailabilityService (calculates slots)
    ↓
AvailabilityRepository (DB: availability)
+ ConsultationService (DB: consultations)
    ↓
Response: [{"startTime":"09:00","endTime":"10:00"}...]
    ↓
Frontend: Display slot buttons
```

## ⚙️ Configuration

### Default Duration
60 minutes (when not specified by client)

### Timezone
Uses server timezone (set in Docker container ENV)

### Database Table
```sql
specialist_availability
├── id (UUID)
├── specialist_id (UUID)
├── day_of_week (1-7, Mon-Sun)
├── start_time (09:00)
├── end_time (17:00)
├── created_at
└── updated_at
```

## 🎯 Key Algorithm

**Availability Service - calculateFreeSlots()**

```
Input:
  - dayStart: 09:00 (specialist available from)
  - dayEnd: 17:00 (specialist available until)
  - bookedRanges: [(10:00-11:30), (14:00-15:30)]
  - slotDuration: 60 (minutes)

Process:
  1. Sort booked ranges by start time
  2. Find gaps between ranges
  3. Only return gaps ≥ 60 minutes
  
Output:
  - [(09:00-10:00), (11:30-14:00), (15:30-17:00)]
  - Each gap large enough for 60-min slot
```

## ✅ Status

| Component | Status | Last Check |
|-----------|--------|-----------|
| Backend Compilation | ✅ SUCCESS | Feb 3, 8:49 PM |
| Code Formatting | ✅ SUCCESS | Feb 3, 8:41 PM |
| Frontend Integration | ✅ READY | Feb 3, 8:52 PM |
| Database Schema | ✅ DEFINED | Ready for migration |
| API Documentation | ✅ COMPLETE | See AVAILABILITY_FEATURE.md |

## 📚 Full Documentation

- **[AVAILABILITY_FEATURE.md](./AVAILABILITY_FEATURE.md)** - Complete feature docs
- **[CLIENT_APP_INTEGRATION.md](./CLIENT_APP_INTEGRATION.md)** - Frontend integration
- **[IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)** - Full technical summary

## 🎓 For Developers

### Adding a New Feature
1. Add model in `core/domain/model/`
2. Add port in `core/ports/`
3. Add repository in `data/repository/`
4. Add service in `core/service/`
5. Add routes in `api/routes/`
6. Update `api/Server.scala`
7. Run `sbt compile`

### Testing
```bash
# Unit tests
sbt test

# Integration tests
sbt it:test

# Format code
sbt scalafmtAll
```

### Code Standards
- All code/comments in **English**
- Scala 3 with **functional programming** style
- Use **case classes** for data
- Use **IO[T]** for effects
- Use **Either[Error, T]** for errors
- Use **Option[T]** for nullable values

## 🚨 Troubleshooting

| Issue | Fix |
|-------|-----|
| No slots appearing | Add availability in specialist-app first |
| "No available slots" | Date might be fully booked |
| API returns 404 | Check endpoint path spelling |
| Compilation fails | Run `sbt clean compile` |
| Slots not updating | Clear browser cache, refresh |
| Wrong timezone | Check Docker ENV TZ setting |

## 📞 Support

Need help? Check:
1. Browser console (F12) for errors
2. Backend logs: `docker-compose logs backend`
3. Documentation: See links under "Full Documentation"
4. Code comments: Each file has inline documentation

---

**Last Updated**: Feb 3, 2026  
**Status**: ✅ Complete and Ready  
**Maintained By**: Consultant Backend Team
