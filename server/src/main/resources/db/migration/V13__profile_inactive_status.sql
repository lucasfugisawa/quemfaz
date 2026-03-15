-- Add INACTIVE status to profile_status enum for user-initiated profile disabling.
-- INACTIVE profiles are hidden from search results but retain all data,
-- allowing re-activation by adding services again.
ALTER TYPE profile_status ADD VALUE IF NOT EXISTS 'INACTIVE';
