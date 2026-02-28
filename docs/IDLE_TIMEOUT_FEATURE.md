# Idle Timeout Feature - Implementation Guide

## Overview

Automatic logout after user inactivity with admin-configurable timeout settings.

## Features

✅ **Configurable Timeout** - Admin can set idle timeout duration (default: 30 minutes)
✅ **Warning Modal** - Users see countdown warning before logout (default: 5 minutes before)
✅ **Stay Logged In** - Users can extend session by clicking "Stay Logged In"
✅ **Activity Tracking** - Tracks mouse, keyboard, scroll, and touch events
✅ **Cross-Platform** - Works in all 3 frontend apps (client, admin, specialist)

---

## Backend Implementation

### 1. Database Migration

**File:** `data/src/main/resources/db/migration/V003__add_system_settings.sql`

Creates `system_settings` table with default values:
- `idle_timeout_minutes` = 30 (default)
- `idle_warning_minutes` = 5 (default)

### 2. API Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/settings/idle-timeout` | Get timeout config | Yes |
| PUT | `/api/settings/idle-timeout` | Update timeout config | Admin |
| GET | `/api/settings` | Get all public settings | Yes |
| GET | `/api/settings/admin` | Get all settings (including private) | Admin |
| PUT | `/api/settings/admin/:key` | Update specific setting | Admin |

### 3. Request/Response Examples

**GET /api/settings/idle-timeout**
```json
{
  "idleTimeoutMinutes": 30,
  "idleWarningMinutes": 5
}
```

**PUT /api/settings/idle-timeout**
```json
{
  "idleTimeoutMinutes": 60,
  "idleWarningMinutes": 10
}
```

**Response:**
```json
{
  "idleTimeoutMinutes": 60,
  "idleWarningMinutes": 10
}
```

---

## Frontend Implementation

### 1. Composable: `useIdleTimeout`

**File:** `specialist-app/composables/useIdleTimeout.ts`

**Usage:**
```typescript
const {
  isWarningVisible,
  formatRemainingTime,
  hideWarning,
  performLogout,
} = useIdleTimeout();
```

**Features:**
- Auto-starts on component mount
- Tracks activity events (mouse, keyboard, scroll, touch)
- Shows warning modal before timeout
- Counts down remaining time
- Auto-logout when timer expires

### 2. Warning Modal Component

**File:** `specialist-app/components/IdleTimeoutModal.vue`

**Props:**
- `visible` - Show/hide modal
- `formatRemainingTime` - Formatted countdown (MM:SS)

**Events:**
- `@stay` - User clicked "Stay Logged In"
- `@logout` - User clicked "Logout Now"

### 3. Integration Example

**File:** `specialist-app/pages/main.vue`

```vue
<template>
  <IdleTimeoutModal
    :visible="isWarningVisible"
    :format-remaining-time="formatRemainingTime"
    @stay="hideWarning"
    @logout="performLogout"
  />
</template>

<script setup>
const {
  isWarningVisible,
  formatRemainingTime,
  hideWarning,
  performLogout,
} = useIdleTimeout();
</script>
```

---

## Admin UI Configuration

### Settings Page (To be implemented)

**File:** `admin-app/pages/settings.vue` (create new)

```vue
<template>
  <section>
    <h2>Idle Timeout Settings</h2>
    
    <div class="form-group">
      <label>Idle Timeout (minutes)</label>
      <input 
        v-model="idleTimeout" 
        type="number" 
        min="5" 
        max="480"
      />
    </div>
    
    <div class="form-group">
      <label>Warning Before Timeout (minutes)</label>
      <input 
        v-model="idleWarning" 
        type="number" 
        min="1" 
        :max="idleTimeout - 1"
      />
    </div>
    
    <button @click="saveSettings">Save Settings</button>
  </section>
</template>

<script setup>
const idleTimeout = ref(30);
const idleWarning = ref(5);

const loadSettings = async () => {
  const config = await $fetch('/api/settings/idle-timeout');
  idleTimeout.value = config.idleTimeoutMinutes;
  idleWarning.value = config.idleWarningMinutes;
};

const saveSettings = async () => {
  await $fetch('/api/settings/idle-timeout', {
    method: 'PUT',
    body: {
      idleTimeoutMinutes: idleTimeout.value,
      idleWarningMinutes: idleWarning.value
    }
  });
};
</script>
```

---

## Activity Events Tracked

The following user activities reset the idle timer:

- `mousedown` - Mouse clicks
- `mousemove` - Mouse movement
- `keypress` - Keyboard input
- `scroll` - Page scrolling
- `touchstart` - Touch screen interaction
- `click` - Click events
- `wheel` - Mouse wheel scrolling

---

## User Flow

```
User Logs In
    ↓
Idle Timer Starts (30 min)
    ↓
User Inactive for 25 min
    ↓
Warning Modal Appears (5 min countdown)
    ↓
┌───────────────┴───────────────┐
│                               │
User clicks             Timer reaches
"Stay Logged In"            00:00
│                               │
↓                               ↓
Timer Resets            Auto Logout
(30 min)                Redirect to Login
```

---

## Configuration

### Default Values

| Setting | Default | Min | Max |
|---------|---------|-----|-----|
| Idle Timeout | 30 minutes | 5 min | 480 min (8 hours) |
| Warning Time | 5 minutes | 1 min | Timeout - 1 |

### Environment Variables (Optional)

Can be set via `.env` for initial deployment:

```bash
IDLE_TIMEOUT_MINUTES=30
IDLE_WARNING_MINUTES=5
```

---

## Security Considerations

1. **Server-side Session Expiry** - JWT tokens should also expire server-side
2. **Refresh Token Rotation** - Invalidate refresh tokens on idle logout
3. **Multiple Tabs** - Timer should work across multiple browser tabs
4. **Public Computers** - Shorter timeout recommended (15-30 min)
5. **Admin Users** - Consider shorter timeout for admin accounts

---

## Testing

### Manual Testing

1. **Set short timeout for testing:**
```sql
UPDATE system_settings 
SET setting_value = '1' 
WHERE setting_key = 'idle_timeout_minutes';

UPDATE system_settings 
SET setting_value = '0' 
WHERE setting_key = 'idle_warning_minutes';
```

2. **Test scenarios:**
   - Login and stay inactive for timeout period
   - Verify warning modal appears
   - Click "Stay Logged In" - verify timer resets
   - Let timer expire - verify redirect to login
   - Verify session is cleared after logout

### API Testing

```bash
# Get current timeout settings
curl http://localhost:8090/api/settings/idle-timeout \
  -H "Authorization: Bearer $TOKEN"

# Update timeout (admin only)
curl -X PUT http://localhost:8090/api/settings/idle-timeout \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"idleTimeoutMinutes": 60, "idleWarningMinutes": 10}'
```

---

## Future Enhancements

- [ ] Different timeout per user role (admin vs client)
- [ ] Remember device option (trusted devices)
- [ ] Email notification before timeout
- [ ] Session activity log
- [ ] Concurrent session limits
- [ ] Custom inactivity messages

---

## Files Modified/Created

### Backend
- `data/src/main/resources/db/migration/V003__add_system_settings.sql` ✨ NEW
- `core/src/main/scala/com/consultant/core/domain/SystemSetting.scala` ✨ NEW
- `core/src/main/scala/com/consultant/core/ports/SystemSettingRepository.scala` ✨ NEW
- `core/src/main/scala/com/consultant/core/service/SystemSettingService.scala` ✨ NEW
- `data/src/main/scala/com/consultant/data/repository/PostgresSystemSettingRepository.scala` ✨ NEW
- `api/src/main/scala/com/consultant/api/dto/SystemSettingDto.scala` ✨ NEW
- `api/src/main/scala/com/consultant/api/routes/SystemSettingRoutes.scala` ✨ NEW
- `api/src/main/scala/com/consultant/api/Server.scala` ✏️ MODIFIED

### Frontend - Specialist App
- `specialist-app/composables/useIdleTimeout.ts` ✨ NEW
- `specialist-app/components/IdleTimeoutModal.vue` ✨ NEW
- `specialist-app/pages/main.vue` ✏️ MODIFIED
- `specialist-app/i18n/locales/en.json` ✏️ MODIFIED

### Frontend - Client App
- `client-app/composables/useIdleTimeout.ts` ✨ NEW
- `client-app/components/IdleTimeoutModal.vue` ✨ NEW
- `client-app/pages/main.vue` ✏️ MODIFIED

### Frontend - Admin App
- `admin-app/composables/useIdleTimeout.ts` ✨ NEW
- `admin-app/components/IdleTimeoutModal.vue` ✨ NEW
- `admin-app/components/SettingsSection.vue` ✨ NEW
- `admin-app/pages/main.vue` ✏️ MODIFIED
- `admin-app/i18n/locales/en.json` ✏️ MODIFIED

---

## Quick Start

1. **Run migrations:**
```bash
cd backend
./run.sh  # Flyway will auto-run V003 migration
```

2. **Verify settings:**
```sql
SELECT * FROM system_settings;
```

3. **Test frontend:**
```bash
cd specialist-app
npm run dev
```

4. **Configure timeout (as admin):**
```bash
curl -X PUT http://localhost:8090/api/settings/idle-timeout \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"idleTimeoutMinutes": 15, "idleWarningMinutes": 3}'
```

---

## Support

For issues or questions about the idle timeout feature, check:
- Backend logs for API errors
- Browser console for frontend errors
- Database for settings values
- Network tab for API calls to `/api/settings/idle-timeout`
