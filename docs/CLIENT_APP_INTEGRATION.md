# Client-App Integration Guide: Specialist Availability

## Quick Start

The client-app has been enhanced with **automatic availability slot loading and selection** when booking consultations.

## Features Added

### 1. Auto-Loading Slots
When a client:
- ✅ Selects a specialist
- ✅ Selects a date
- ✅ Changes duration

The form **automatically fetches available time slots** from the backend.

### 2. Visual Slot Selection
Available slots appear as a **grid of buttons** showing:
- Time range (e.g., "09:00 - 10:00")
- Visual feedback on hover (blue highlight)
- Selected state (blue background, white text)

### 3. Smart Time Input
Clicking a slot **automatically fills** the "Scheduled Time" input field.

### 4. Error Handling
If no slots are available:
- Shows informative error message
- Allows manual time entry as fallback

## How to Use

### From Client Perspective

1. **Go to "My Consultations" tab**
   - Click menu: "My Consultations"

2. **Fill in consultation details**
   - Select Specialist (required)
   - Select Category (auto-loads based on specialist)
   - Add Description
   - Select Date (required)
   - Select Duration (defaults to 60 minutes)

3. **Available slots appear automatically**
   - Wait for slots to load (spinner shows while loading)
   - See all available 60-minute windows

4. **Click a slot button to select**
   - "09:00 - 10:00" → Time field becomes "09:00"
   - Or manually enter time in the field

5. **Click "Book Consultation"**
   - System validates time against available slots
   - If valid, consultation is created

## Technical Details

### New State Variables
```typescript
const availableSlots = ref<any[]>([])  // Loaded slots
const slotsLoading = ref(false)        // Loading state
const slotsError = ref('')             // Error message
```

### New Functions
```typescript
// Auto-fetch available slots
const loadAvailableSlots = async () {
  // Calls: GET /api/specialists/{id}/availability/slots?date=...&durationMinutes=...
}

// Handle slot button click
const selectSlot = (slot: any) => {
  consultationForm.value.scheduledTime = slot.startTime
}
```

### Watchers
```typescript
// Auto-load slots when specialist, date, or duration change
watch([
  () => consultationForm.value.specialistId,
  () => consultationForm.value.scheduledDate,
  () => consultationForm.value.duration
], () => {
  loadAvailableSlots()
})
```

### UI Components
```vue
<!-- Shows loading spinner -->
<div v-if="slotsLoading" class="slots-container">
  <div class="slots-spinner">Loading available slots...</div>
</div>

<!-- Shows error message -->
<div v-else-if="slotsError" class="slots-container error">
  <p>{{ slotsError }}</p>
</div>

<!-- Shows slot buttons -->
<div v-else-if="availableSlots.length > 0" class="slots-container">
  <div class="slots-grid">
    <button 
      v-for="(slot, idx) in availableSlots" 
      :key="idx"
      class="slot-button"
      :class="{ selected: consultationForm.scheduledTime === slot.startTime }"
      @click="selectSlot(slot)"
    >
      {{ slot.startTime }} - {{ slot.endTime }}
    </button>
  </div>
</div>

<!-- Shows empty state -->
<div v-else class="slots-container empty">
  <p>No available slots for the selected date and duration</p>
</div>
```

## API Communication

The client-app makes requests to:

```
GET /api/specialists/{specialistId}/availability/slots?date=2026-02-10&durationMinutes=60

Response:
{
  "specialistId": "uuid",
  "date": "2026-02-10",
  "slots": [
    { "startTime": "09:00", "endTime": "10:00", "durationMinutes": 60 },
    { "startTime": "11:30", "endTime": "12:30", "durationMinutes": 60 },
    ...
  ],
  "hasAvailableSlots": true
}
```

## Styling

New CSS classes for slots display:

| Class | Purpose |
|-------|---------|
| `.slots-container` | Main container for slots section |
| `.slots-grid` | Grid layout for slot buttons |
| `.slot-button` | Individual time slot button |
| `.slot-button.selected` | Selected slot styling (blue) |
| `.slots-spinner` | Loading indicator |

### Appearance
- **Unselected slot**: White background, gray border, dark text
- **Hover**: Light blue background, blue border
- **Selected**: Blue background, white text
- **Error state**: Light red background, red text
- **Loading**: Spinner animation with text

## Testing

### Manual Testing

1. **Start the backend**
   ```bash
   cd backend
   sbt run
   ```

2. **Start the client-app**
   ```bash
   cd client-app
   npm run dev
   ```

3. **Test the flow**
   - Login as client
   - Go to "My Consultations"
   - Select a specialist with availability defined
   - Select a future date
   - Verify slots appear

### Common Issues

❌ **No slots appearing?**
- Ensure specialist has availability defined in specialist-app
- Check browser console for errors
- Verify date is not in the past
- Try refreshing the page

❌ **"No available slots" message?**
- All slots for that date might be booked
- Try a different date
- Try a shorter duration
- Check specialist availability (might not work that day)

❌ **Time selection not working?**
- Clear browser cache
- Refresh page
- Check browser console for JavaScript errors

## Future Enhancements

- [ ] Show duration of each slot in button
- [ ] Allow filtering by duration (15min, 30min, 60min, 90min)
- [ ] Show specialist timezone in slots
- [ ] Add "Show more times" if many slots
- [ ] Keyboard navigation (arrow keys to select slots)
- [ ] Accessibility improvements (ARIA labels, screen reader support)

## File Changes

**Modified Files:**
- `client-app/components/ConsultationsBookTab.vue` - Main consultation page

**No new files created** - All changes integrated into existing component.

## Deployment

### Building
```bash
cd client-app
npm run build
```

### Docker
```bash
# From root directory
docker-compose up --build
```

## Related Documentation

- See [AVAILABILITY_FEATURE.md](./AVAILABILITY_FEATURE.md) for backend details
- Backend API documentation: `http://localhost:8080/docs` (Swagger UI)

---

**Status**: ✅ Complete and integrated  
**Tested**: Frontend loads slots correctly  
**Compilation**: Backend ✅ Success, Frontend ✅ (Nuxt compiles)  
**Last Updated**: Feb 3, 2026
