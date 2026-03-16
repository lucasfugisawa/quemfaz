-- Migrate existing "uncertain" values to "safe" before tightening constraint
UPDATE unmatched_service_signals
SET safety_classification = 'safe'
WHERE safety_classification = 'uncertain';

-- Replace the constraint to only allow safe/unsafe
ALTER TABLE unmatched_service_signals DROP CONSTRAINT valid_safety;
ALTER TABLE unmatched_service_signals ADD CONSTRAINT valid_safety
    CHECK (safety_classification IN ('safe', 'unsafe') OR safety_classification IS NULL);
