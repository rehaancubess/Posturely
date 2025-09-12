-- Supabase Database Setup for Posture Tracking
-- Run this in your Supabase SQL editor

-- Create the posture_records table
CREATE TABLE IF NOT EXISTS public.posture_records (
    id BIGSERIAL PRIMARY KEY,
    user_email TEXT NOT NULL,
    date DATE NOT NULL,
    time TIME NOT NULL,
    average_posture_score INTEGER NOT NULL CHECK (average_posture_score >= 0 AND average_posture_score <= 100),
    tracking_source TEXT NOT NULL CHECK (tracking_source IN ('phone', 'laptop', 'airpods')),
    timestamp BIGINT NOT NULL,
    samples_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Composite unique constraint to prevent duplicate records for same user, date, and time
    UNIQUE(user_email, date, time)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_posture_records_user_email ON public.posture_records(user_email);
CREATE INDEX IF NOT EXISTS idx_posture_records_date ON public.posture_records(date);
CREATE INDEX IF NOT EXISTS idx_posture_records_timestamp ON public.posture_records(timestamp);
CREATE INDEX IF NOT EXISTS idx_posture_records_user_date ON public.posture_records(user_email, date);
CREATE INDEX IF NOT EXISTS idx_posture_records_tracking_source ON public.posture_records(tracking_source);

-- Enable Row Level Security (RLS)
ALTER TABLE public.posture_records ENABLE ROW LEVEL SECURITY;

-- Create policy to allow users to insert their own records
CREATE POLICY "Users can insert their own posture records" ON public.posture_records
    FOR INSERT WITH CHECK (auth.jwt() ->> 'email' = user_email);

-- Create policy to allow users to view their own records
CREATE POLICY "Users can view their own posture records" ON public.posture_records
    FOR SELECT USING (auth.jwt() ->> 'email' = user_email);

-- Create policy to allow users to update their own records
CREATE POLICY "Users can update their own posture records" ON public.posture_records
    FOR UPDATE USING (auth.jwt() ->> 'email' = user_email);

-- Create policy to allow users to delete their own records
CREATE POLICY "Users can delete their own posture records" ON public.posture_records
    FOR DELETE USING (auth.jwt() ->> 'email' = user_email);

-- Create a view for daily posture summaries
CREATE OR REPLACE VIEW public.daily_posture_summary AS
SELECT 
    user_email,
    date,
    COUNT(*) as total_records,
    AVG(average_posture_score) as daily_average_score,
    MIN(average_posture_score) as min_score,
    MAX(average_posture_score) as max_score,
    SUM(samples_count) as total_samples,
    STRING_AGG(DISTINCT tracking_source, ', ') as tracking_sources_used
FROM public.posture_records
GROUP BY user_email, date
ORDER BY user_email, date DESC;

-- Grant permissions
GRANT ALL ON public.posture_records TO authenticated;
GRANT ALL ON public.daily_posture_summary TO authenticated;
GRANT USAGE ON SEQUENCE public.posture_records_id_seq TO authenticated;

-- Insert some sample data for testing (optional)
-- INSERT INTO public.posture_records (user_email, date, time, average_posture_score, tracking_source, timestamp, samples_count)
-- VALUES 
--     ('test@example.com', '2024-01-15', '10:00:00', 85, 'phone', 1705312800000, 60),
--     ('test@example.com', '2024-01-15', '10:01:00', 78, 'laptop', 1705312860000, 60),
--     ('test@example.com', '2024-01-15', '10:02:00', 92, 'airpods', 1705312920000, 60);
