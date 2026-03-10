-- V4: Search Vertical Schema

CREATE TABLE search_queries (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
    original_query TEXT NOT NULL,
    normalized_query TEXT NOT NULL,
    city_name TEXT NOT NULL,
    neighborhoods_json JSONB NOT NULL,
    interpreted_service_ids_json JSONB NOT NULL,
    input_mode TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_queries_user_id ON search_queries(user_id);
CREATE INDEX idx_search_queries_city_name ON search_queries(city_name);
CREATE INDEX idx_search_queries_created_at ON search_queries(created_at);
