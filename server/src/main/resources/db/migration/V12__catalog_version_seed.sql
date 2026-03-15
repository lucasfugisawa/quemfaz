-- Seed initial catalog version counter
INSERT INTO system_configuration (key, value) VALUES ('catalog.version', '1')
ON CONFLICT (key) DO NOTHING;
