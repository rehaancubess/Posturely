# Backend Integration - Posture Data Recording

This document describes the backend integration that records posture data to Supabase every minute during phone tracking.

## Overview

The app now automatically records posture scores to Supabase every minute when using phone tracking (not desktop). This provides valuable data for tracking posture trends over time.

## How It Works

### 1. Data Collection
- **Frequency**: Every minute (60 seconds)
- **Data Points**: Collects up to 60 posture scores per minute
- **Calculation**: Averages all scores for the minute
- **Storage**: Sends to Supabase with timestamp and metadata

### 2. Database Schema

The `posture_records` table stores:
- `user_email`: User identifier
- `date`: Date (YYYY-MM-DD format)
- `time`: Time (HH:MM:SS format)
- `average_posture_score`: Average score for that minute (0-100)
- `tracking_source`: Source of tracking ('phone', 'laptop', or 'airpods')
- `timestamp`: Unix timestamp
- `samples_count`: Number of samples collected
- `created_at`: Record creation time

### 3. Security
- Row Level Security (RLS) enabled
- Users can only access their own data
- Authentication required for all operations

## Setup Instructions

### 1. Database Setup

Run the SQL script in your Supabase SQL editor:

```sql
-- Copy and paste the contents of supabase_setup.sql
```

This will:
- Create the `posture_records` table
- Set up indexes for performance
- Enable Row Level Security
- Create access policies
- Create a daily summary view

### 2. App Integration

The integration is already implemented in:
- `PostureDataService.kt`: Core service for data recording
- `LiveTrackingScreen.kt`: UI integration and status display
- `App.kt`: User email passing

### 3. Testing

To test the integration:

1. **Set up Supabase**: Run the SQL script
2. **Login**: Use the app's login flow
3. **Start tracking**: Begin phone posture tracking
4. **Monitor**: Watch for "Recording to Cloud" indicator
5. **Check database**: Verify records appear in Supabase

## API Endpoints

The service uses Supabase's PostgREST API:

### Insert Record
```kotlin
Supa.client.postgrest["posture_records"].insert(data)
```

### Query Records
```kotlin
// Get user's records for a date
Supa.client.postgrest["posture_records"]
    .select { eq("user_email", email) and eq("date", date) }
    .order("time", ascending = true)
```

### Daily Summary
```kotlin
// Get daily averages
Supa.client.postgrest["daily_posture_summary"]
    .select { eq("user_email", email) }
    .order("date", ascending = false)
```

## Data Flow

```
Phone Tracking → Posture Score → PostureDataService → Supabase
     ↓              ↓                    ↓              ↓
  Every 100ms   Smoothed Score    Collect 60 scores   Record every minute
```

## UI Indicators

The app shows recording status:
- **"Recording to Cloud"**: Active recording indicator
- **Last recorded score**: Shows most recent upload
- **Timestamp**: When the last record was sent

## Error Handling

- **Network failures**: Retries automatically
- **Invalid data**: Validates before sending
- **Duplicate records**: Prevents with unique constraints
- **Authentication**: Handles expired sessions

## Performance Considerations

- **Batch processing**: Collects 60 scores before sending
- **Async operations**: Non-blocking database calls
- **Indexed queries**: Fast data retrieval
- **Connection pooling**: Efficient Supabase usage

## Future Enhancements

1. **Offline support**: Queue records when offline
2. **Data compression**: Reduce payload size
3. **Analytics dashboard**: Visualize trends
4. **Export functionality**: Download data
5. **Real-time sync**: Live updates across devices

## Troubleshooting

### Common Issues

1. **No records appearing**
   - Check user authentication
   - Verify RLS policies
   - Check network connectivity

2. **Recording not starting**
   - Ensure user email is provided
   - Check Supabase connection
   - Verify table exists

3. **Duplicate records**
   - Check unique constraints
   - Verify timestamp handling
   - Review timezone settings

### Debug Logs

The service logs important events:
- `📊 PostureDataService: Started recording for user: email`
- `✅ PostureDataService: Recorded score X for date time`
- `❌ PostureDataService: Failed to record posture data`

### Database Queries

Useful queries for debugging:

```sql
-- Check recent records
SELECT * FROM posture_records 
WHERE user_email = 'user@example.com' 
ORDER BY timestamp DESC 
LIMIT 10;

-- Check daily summary
SELECT * FROM daily_posture_summary 
WHERE user_email = 'user@example.com' 
ORDER BY date DESC;

-- Check for duplicates
SELECT user_email, date, time, COUNT(*) 
FROM posture_records 
GROUP BY user_email, date, time 
HAVING COUNT(*) > 1;
```
