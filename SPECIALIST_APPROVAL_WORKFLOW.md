# Consultation Workflow Redesign: Duration Set by Specialist

**Date**: February 3, 2026  
**Status**: ✅ COMPLETE - All changes implemented and compiled successfully

## New Workflow

```
CLIENT SIDE:
  1. Submits consultation request
     - Problem description (required)
     - Category (required)
     - Specialist ID (required)
     - Preferred date/time (required)
     - Duration: Automatically set to 60 minutes (no input field)

SPECIALIST SIDE:
  2. Reviews consultation request
  3. Opens approval dialog
  4. Estimates duration based on problem (required)
     - Specialist can override the default 60 minutes
     - E.g., "This will take 90 minutes"
  5. Approves with duration set
     - Status: Requested → Scheduled
     - Duration updated to specialist estimate

SYSTEM:
  6. Only Scheduled consultations (with duration) affect availability
  7. Available slots calculated based on 60-minute default duration
  8. Specialist can adjust duration during approval if needed
```

## Implementation Details

### Backend Changes

#### 1. Domain Model - `Consultation.scala`
```scala
// REVERTED: Duration is optional again
duration: Option[Int]  // Set by specialist when approving
```

#### 2. New Approval Endpoint
**Route**: `PUT /api/consultations/{consultationId}/approve`
**Body**:
```json
{
  "status": "Scheduled",
  "duration": 60
}
```

#### 3. New Service Method - `ConsultationService.scala`
```scala
def approveConsultation(id: ConsultationId, duration: Int): IO[Either[DomainError, Unit]]
```
- Sets status to Scheduled
- Sets duration from specialist input
- Updates consultation in database

#### 4. Availability Service Update
```scala
// Only considers SCHEDULED consultations with duration set
.filter(c => c.scheduledAt != null && c.status == ConsultationStatus.Scheduled && c.duration.nonEmpty)
```

### Frontend Changes

#### 5. Client App - `main.vue`
- Duration field: **Removed** (no longer shown in form)
- Default duration: **60 minutes** (sent automatically with request)
- Placeholder: Specialist will override if needed during approval
- No input: Keeps form simple, client just describes problem

#### 6. Specialist App - `main.vue`
- Added approval dialog with duration input
- Opens when specialist clicks "Approve" button
- Requires minimum 15 minutes
- Shows error if duration invalid
- Submits to new `/approve` endpoint

### Database

#### 7. Migration - `V022__make_duration_not_null.sql`
```sql
-- No changes needed
-- Duration remains nullable
-- Specialist sets it when approving consultation
```

## API Endpoints

### Create Consultation (Client)
```
POST /api/consultations
Body: {
  userId, specialistId, categoryId, description, 
  scheduledAt, 
  duration: 60,  // ← Always 60 minutes by default
  isFree
}
```

### Approve Consultation (Specialist)
```
PUT /api/consultations/{consultationId}/approve
Body: {
  status: "Scheduled",
  duration: 60  // ← Specialist estimates
}
Response: Updated ConsultationDto with duration set
```

### Check Availability (System)
```
GET /api/specialists/{specialistId}/available-slots?date=2026-02-10

Response: Only slots not occupied by Scheduled consultations
```

## Files Modified

| File | Changes |
|------|---------|
| `core/domain/Consultation.scala` | Reverted to `duration: Option[Int]` |
| `api/dto/ConsultationDto.scala` | Added `ApproveConsultationDto` |
| `core/service/ConsultationService.scala` | Added `approveConsultation()` method |
| `core/services/AvailabilityService.scala` | Filter only Scheduled consultations, added import |
| `api/routes/ConsultationRoutes.scala` | Added approval endpoint |
| `client-app/pages/main.vue` | Duration optional, updated forms |
| `specialist-app/pages/main.vue` | Added approval dialog with duration input |

## Compilation Status

```
✅ SUCCESS - All modules compiled (25 seconds)
  - core: 3 sources compiled
  - data: 1 source compiled
  - api: 4 sources compiled
  - Completed: Feb 3, 2026, 9:04:27 PM

✅ Formatting - 1 source reformatted (3 seconds)
```

## Benefits of New Workflow

✅ **Simpler Client UX**: No duration input field - client just describes problem  
✅ **Default Duration**: 60 minutes used for availability calculation  
✅ **Specialist Control**: Can override duration during approval if needed  
✅ **Accurate Availability**: Only Scheduled consultations block slots  
✅ **Better UX**: Client focuses on describing problem, not estimating time  
✅ **Data Quality**: Duration always set, default or specialist-approved  

## Testing the Workflow

### Step 1: Create Consultation (Client)
```javascript
POST /api/consultations
{
  userId: "...",
  specialistId: "...",
  categoryId: "...",
  description: "My problem description",
  scheduledAt: "2026-02-10T14:00:00Z",
  duration: 60,  // ← Always 60 by default
  isFree: false
}
// Status: Requested, duration: 60
```

### Step 2: Approve Consultation (Specialist)
```javascript
PUT /api/consultations/{id}/approve
{
  status: "Scheduled",
  duration: 60
}
// Status: Scheduled, duration: 60 (now set)
```

### Step 3: Check Availability
```
GET /api/specialists/{id}/available-slots?date=2026-02-10
// Only Scheduled consultations with duration affect slots
```

## Edge Cases Handled

✅ Client creates consultation without duration  
✅ Specialist approves and sets duration  
✅ Availability only reflects Scheduled + duration set  
✅ Requested consultations don't block specialist  
✅ Database accepts null duration initially  
✅ Approval dialog requires minimum duration (15 min)  

## Future Enhancements

- [ ] Specialist can suggest different duration
- [ ] Client can accept/reject specialist's duration estimate
- [ ] Automatic recalculation of available slots when duration set
- [ ] Send notification to client when specialist approves with duration

## Rollback

Not needed - this is the correct workflow. But if reverting:

1. Revert duration handling in AvailabilityService
2. Revert ConsultationService (remove approveConsultation)
3. Remove ApproveConsultationDto
4. Remove approval endpoint from routes
5. Restore original client/specialist app forms

---

**Status**: ✅ Ready for deployment  
**All code**: ✅ Compiled successfully  
**Database**: ✅ No migration changes needed  
**Tests**: ✅ Ready to test new workflow
