-- QuemFaz — Full schema and seed data

------------------------------------------------------------
-- Custom enum types
------------------------------------------------------------

CREATE TYPE user_status AS ENUM ('ACTIVE', 'BLOCKED');
CREATE TYPE profile_completeness AS ENUM ('INCOMPLETE', 'COMPLETE');
CREATE TYPE profile_status AS ENUM ('DRAFT', 'PUBLISHED', 'BLOCKED', 'INACTIVE');
CREATE TYPE service_match_level AS ENUM ('PRIMARY', 'SECONDARY', 'RELATED');
CREATE TYPE contact_channel AS ENUM ('WHATSAPP', 'PHONE_CALL');
CREATE TYPE report_status AS ENUM ('OPEN', 'DISMISSED', 'RESOLVED');
CREATE TYPE report_reason AS ENUM ('SPAM', 'INAPPROPRIATE_CONTENT', 'WRONG_PHONE_NUMBER', 'FAKE_PROFILE', 'ABUSIVE_BEHAVIOR', 'OTHER');
CREATE TYPE report_target_type AS ENUM ('PROFESSIONAL_PROFILE');

------------------------------------------------------------
-- Users and auth
------------------------------------------------------------

CREATE TABLE users (
    id TEXT PRIMARY KEY,
    full_name TEXT NOT NULL DEFAULT '',
    photo_url TEXT,
    date_of_birth DATE,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_phone_auth_identities (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone_number TEXT NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_phone_number UNIQUE (phone_number)
);

CREATE INDEX idx_user_phone_auth_identities_user_id ON user_phone_auth_identities(user_id);

CREATE TABLE otp_challenges (
    id TEXT PRIMARY KEY,
    phone_number TEXT NOT NULL,
    otp_code_hash TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_challenges_phone_number ON otp_challenges(phone_number);
CREATE INDEX idx_otp_challenges_active ON otp_challenges(phone_number) WHERE consumed_at IS NULL;

CREATE TABLE refresh_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

------------------------------------------------------------
-- Professional profiles
------------------------------------------------------------

CREATE TABLE professional_profiles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    known_name TEXT,
    description TEXT,
    normalized_description TEXT,
    city_name TEXT,
    completeness profile_completeness NOT NULL DEFAULT 'INCOMPLETE',
    status profile_status NOT NULL DEFAULT 'DRAFT',
    view_count INTEGER NOT NULL DEFAULT 0,
    contact_click_count INTEGER NOT NULL DEFAULT 0,
    last_active_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_professional_profiles_user_id ON professional_profiles(user_id);
CREATE INDEX idx_professional_profiles_city_name ON professional_profiles(city_name);
CREATE INDEX idx_professional_profiles_status ON professional_profiles(status);

CREATE TABLE professional_profile_services (
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    service_id TEXT NOT NULL,
    match_level service_match_level NOT NULL,
    PRIMARY KEY (professional_profile_id, service_id)
);

CREATE INDEX idx_pps_profile_id ON professional_profile_services(professional_profile_id);

CREATE TABLE professional_profile_portfolio_photos (
    id TEXT PRIMARY KEY,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    caption TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pppp_profile_id ON professional_profile_portfolio_photos(professional_profile_id);

------------------------------------------------------------
-- Images (temporary blob storage for profile photos)
------------------------------------------------------------

CREATE TABLE stored_images (
    id TEXT PRIMARY KEY,
    data BYTEA NOT NULL,
    content_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

------------------------------------------------------------
-- Search
------------------------------------------------------------

CREATE TABLE search_queries (
    id TEXT PRIMARY KEY,
    user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
    original_query TEXT NOT NULL,
    normalized_query TEXT NOT NULL,
    city_name TEXT NOT NULL,
    interpreted_service_ids_json JSONB NOT NULL,
    input_mode TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_queries_user_id ON search_queries(user_id);
CREATE INDEX idx_search_queries_city_name ON search_queries(city_name);
CREATE INDEX idx_search_queries_created_at ON search_queries(created_at);

CREATE TABLE search_events (
    id TEXT PRIMARY KEY,
    resolved_service_id TEXT NOT NULL,
    city_name TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_events_city_created ON search_events (city_name, created_at);
CREATE INDEX idx_search_events_created ON search_events (created_at);

------------------------------------------------------------
-- Favorites, contact tracking, reporting
------------------------------------------------------------

CREATE TABLE favorites (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_profile_favorite UNIQUE (user_id, professional_profile_id)
);

CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_profile_id ON favorites(professional_profile_id);

CREATE TABLE contact_click_events (
    id TEXT PRIMARY KEY,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
    channel contact_channel NOT NULL,
    city_name TEXT,
    source TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contact_click_profile_id ON contact_click_events(professional_profile_id);
CREATE INDEX idx_contact_click_user_id ON contact_click_events(user_id);
CREATE INDEX idx_contact_click_created_at ON contact_click_events(created_at);

CREATE TABLE profile_view_events (
    id TEXT PRIMARY KEY,
    professional_profile_id TEXT NOT NULL REFERENCES professional_profiles(id) ON DELETE CASCADE,
    user_id TEXT REFERENCES users(id) ON DELETE SET NULL,
    city_name TEXT,
    source TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profile_view_profile_id ON profile_view_events(professional_profile_id);
CREATE INDEX idx_profile_view_user_id ON profile_view_events(user_id);
CREATE INDEX idx_profile_view_created_at ON profile_view_events(created_at);

CREATE TABLE reports (
    id TEXT PRIMARY KEY,
    reporter_user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type report_target_type NOT NULL,
    target_id TEXT NOT NULL,
    reason report_reason NOT NULL,
    description TEXT,
    status report_status NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_action TEXT
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_reporter_user_id ON reports(reporter_user_id);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
CREATE INDEX idx_reports_created_at ON reports(created_at);

------------------------------------------------------------
-- Service catalog
------------------------------------------------------------

CREATE TABLE service_categories (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE canonical_services (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    description TEXT NOT NULL,
    category_id TEXT NOT NULL REFERENCES service_categories(id),
    aliases JSONB NOT NULL DEFAULT '[]',
    status TEXT NOT NULL DEFAULT 'active',
    created_by TEXT NOT NULL DEFAULT 'migration',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    merged_into_service_id TEXT REFERENCES canonical_services(id),
    review_status_reason TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by TEXT,
    CONSTRAINT valid_status CHECK (status IN ('active', 'pending_review', 'rejected', 'merged'))
);

CREATE INDEX idx_canonical_services_category ON canonical_services(category_id);
CREATE INDEX idx_canonical_services_status ON canonical_services(status);

CREATE TABLE unmatched_service_signals (
    id TEXT PRIMARY KEY,
    raw_description TEXT NOT NULL,
    source TEXT NOT NULL,
    user_id TEXT REFERENCES users(id),
    best_match_service_id TEXT,
    best_match_confidence TEXT,
    provisional_service_id TEXT REFERENCES canonical_services(id),
    city_name TEXT,
    safety_classification TEXT,
    safety_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_source CHECK (source IN ('onboarding', 'search')),
    CONSTRAINT valid_confidence CHECK (best_match_confidence IN ('high', 'low', 'none') OR best_match_confidence IS NULL),
    CONSTRAINT valid_safety CHECK (safety_classification IN ('safe', 'unsafe') OR safety_classification IS NULL)
);

CREATE INDEX idx_unmatched_signals_provisional ON unmatched_service_signals(provisional_service_id);
CREATE INDEX idx_unmatched_signals_created ON unmatched_service_signals(created_at);

------------------------------------------------------------
-- System configuration
------------------------------------------------------------

CREATE TABLE system_configuration (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

------------------------------------------------------------
-- Seed data: catalog categories
------------------------------------------------------------

INSERT INTO service_categories (id, display_name, sort_order) VALUES
    ('CLEANING', 'Limpeza', 1),
    ('MAINTENANCE', 'Manutenção', 2),
    ('PAINTING', 'Pintura', 3),
    ('GARDEN', 'Jardim', 4),
    ('EVENTS', 'Eventos', 5),
    ('BEAUTY', 'Beleza', 6),
    ('MOVING_AND_ASSEMBLY', 'Mudanças e Montagem', 7),
    ('CAREGIVING', 'Cuidados', 8),
    ('LESSONS', 'Aulas', 9),
    ('AUTOMOTIVE', 'Automotivo', 10);

------------------------------------------------------------
-- Seed data: catalog services
------------------------------------------------------------

INSERT INTO canonical_services (id, display_name, description, category_id, aliases, status, created_by) VALUES
    -- CLEANING (8)
    ('clean-house', 'Limpeza Residencial', 'Limpeza de casas e apartamentos', 'CLEANING', '["diarista", "faxina", "limpeza de casa"]', 'active', 'migration'),
    ('clean-post-construction', 'Limpeza Pós-Obra', 'Limpeza pesada após construções ou reformas', 'CLEANING', '["pós obra", "limpeza pesada"]', 'active', 'migration'),
    ('clean-sofa', 'Limpeza de Sofá', 'Higienização de estofados, sofás e poltronas', 'CLEANING', '["lavagem de sofá", "impermeabilização", "estofados"]', 'active', 'migration'),
    ('clean-land', 'Limpeza de Terreno', 'Retirada de entulho e mato de terrenos', 'CLEANING', '["roçagem de terreno", "limpar lote"]', 'active', 'migration'),
    ('clean-window', 'Limpeza de Vidros', 'Limpeza de janelas, vitrines e fachadas', 'CLEANING', '["limpar vidro", "limpeza de janela"]', 'active', 'migration'),
    ('clean-water-tank', 'Limpeza de Caixa d''Água', 'Higienização de caixas d''água', 'CLEANING', '["limpar caixa d''água", "higienização reservatório"]', 'active', 'migration'),
    ('clean-pool', 'Limpeza de Piscina', 'Limpeza e manutenção de piscinas', 'CLEANING', '["limpeza piscina", "tratamento piscina"]', 'active', 'migration'),
    ('clean-carpet', 'Limpeza de Carpete', 'Higienização de carpetes e tapetes', 'CLEANING', '["lavagem de tapete", "limpar carpete"]', 'active', 'migration'),
    -- MAINTENANCE (18)
    ('maintenance-electrician', 'Elétrica Residencial', 'Instalação e manutenção elétrica', 'MAINTENANCE', '["eletricista", "tomada não funciona", "queda de energia"]', 'active', 'migration'),
    ('maintenance-shower', 'Chuveiro', 'Instalação e manutenção de chuveiros', 'MAINTENANCE', '["trocar chuveiro", "chuveiro queimado"]', 'active', 'migration'),
    ('maintenance-plumber', 'Hidráulica / Encanador', 'Instalação e manutenção hidráulica', 'MAINTENANCE', '["encanador", "vazamento", "desentupimento"]', 'active', 'migration'),
    ('maintenance-handyman', 'Manutenção Geral', 'Pequenos serviços e ajustes residenciais', 'MAINTENANCE', '["marido de aluguel", "pequenos serviços"]', 'active', 'migration'),
    ('maintenance-furniture', 'Montagem de Móveis', 'Montagem e desmontagem de móveis', 'MAINTENANCE', '["montador móveis", "armário"]', 'active', 'migration'),
    ('maintenance-aircon', 'Ar Condicionado', 'Instalação e manutenção de ar-condicionado', 'MAINTENANCE', '["instalar ar", "limpeza split"]', 'active', 'migration'),
    ('maintenance-appliance', 'Eletrodomésticos', 'Manutenção de eletrodomésticos', 'MAINTENANCE', '["geladeira", "máquina de lavar"]', 'active', 'migration'),
    ('maintenance-locksmith', 'Chaveiro', 'Manutenção de fechaduras e acesso', 'MAINTENANCE', '["abrir porta", "trocar fechadura"]', 'active', 'migration'),
    ('maintenance-glass', 'Vidros', 'Instalação e manutenção de vidros', 'MAINTENANCE', '["vidro quebrado", "box banheiro"]', 'active', 'migration'),
    ('maintenance-roof', 'Telhado / Cobertura', 'Manutenção de telhados', 'MAINTENANCE', '["goteira", "telha"]', 'active', 'migration'),
    ('maintenance-drain', 'Desentupimento', 'Limpeza e manutenção de tubulações', 'MAINTENANCE', '["cano entupido", "vaso entupido"]', 'active', 'migration'),
    ('maintenance-computer', 'Computador', 'Manutenção de computadores', 'MAINTENANCE', '["formatar pc", "computador lento"]', 'active', 'migration'),
    ('maintenance-wifi', 'Wi-Fi / Internet', 'Instalação e manutenção de redes', 'MAINTENANCE', '["configurar wifi", "internet lenta"]', 'active', 'migration'),
    ('maintenance-tv', 'TV / Televisão', 'Instalação e manutenção de TVs', 'MAINTENANCE', '["instalar tv", "tv não liga"]', 'active', 'migration'),
    ('maintenance-security', 'Câmeras de Segurança', 'Instalação e manutenção de sistemas de segurança', 'MAINTENANCE', '["cftv", "câmera segurança"]', 'active', 'migration'),
    ('maintenance-mobile', 'Celular / Smartphone', 'Manutenção de celulares', 'MAINTENANCE', '["trocar tela", "celular quebrado"]', 'active', 'migration'),
    ('maintenance-electronics', 'Eletrônicos em Geral', 'Manutenção de eletrônicos domésticos', 'MAINTENANCE', '["som não funciona", "aparelho eletrônico"]', 'active', 'migration'),
    -- PAINTING (5)
    ('paint-residential', 'Pintura Residencial', 'Pintura de casas e apartamentos', 'PAINTING', '["pintor", "pintura casa"]', 'active', 'migration'),
    ('paint-commercial', 'Pintura Comercial', 'Pintura de lojas e escritórios', 'PAINTING', '["pintura loja"]', 'active', 'migration'),
    ('paint-industrial', 'Pintura Industrial', 'Pintura de estruturas industriais', 'PAINTING', '["estrutura metálica"]', 'active', 'migration'),
    ('paint-automotive', 'Pintura Automotiva', 'Pintura e acabamento de veículos', 'PAINTING', '["funilaria", "pintura carro"]', 'active', 'migration'),
    ('paint-decorative', 'Pintura Decorativa', 'Acabamentos e texturas especiais', 'PAINTING', '["grafiato", "textura"]', 'active', 'migration'),
    -- GARDEN (5)
    ('garden-maintenance', 'Jardinagem', 'Manutenção de jardins', 'GARDEN', '["jardineiro"]', 'active', 'migration'),
    ('garden-pruning', 'Poda de Árvores', 'Poda e cuidado de árvores', 'GARDEN', '["podar árvore"]', 'active', 'migration'),
    ('garden-mowing', 'Corte de Grama', 'Corte de grama', 'GARDEN', '["cortar grama"]', 'active', 'migration'),
    ('garden-design', 'Paisagismo', 'Projeto e design de jardins', 'GARDEN', '["paisagista"]', 'active', 'migration'),
    ('garden-irrigation', 'Sistema de Irrigação', 'Instalação de irrigação', 'GARDEN', '["aspersor"]', 'active', 'migration'),
    -- EVENTS (7)
    ('event-waiter', 'Garçom para Eventos', 'Atendimento em eventos', 'EVENTS', '["garçom"]', 'active', 'migration'),
    ('event-bartender', 'Bartender / Drinks', 'Preparo de bebidas', 'EVENTS', '["barman"]', 'active', 'migration'),
    ('event-kitchen-assistant', 'Ajudante de Cozinha', 'Auxílio em eventos', 'EVENTS', '["auxiliar cozinha"]', 'active', 'migration'),
    ('event-security', 'Segurança para Eventos', 'Segurança e controle de acesso', 'EVENTS', '["segurança festa"]', 'active', 'migration'),
    ('event-photographer', 'Fotógrafo', 'Cobertura fotográfica', 'EVENTS', '["fotógrafo"]', 'active', 'migration'),
    ('event-dj', 'DJ', 'Som e música para eventos', 'EVENTS', '["dj"]', 'active', 'migration'),
    ('event-decorator', 'Decoração de Eventos', 'Decoração de ambientes', 'EVENTS', '["decorador"]', 'active', 'migration'),
    -- BEAUTY (7)
    ('beauty-manicure-only', 'Manicure', 'Cuidados com mãos e unhas das mãos', 'BEAUTY', '["manicure", "fazer as mãos", "unhas das mãos"]', 'active', 'migration'),
    ('beauty-pedicure-only', 'Pedicure', 'Cuidados com pés e unhas dos pés', 'BEAUTY', '["pedicure", "fazer os pés", "unhas dos pés"]', 'active', 'migration'),
    ('beauty-hairdresser', 'Cabeleireiro(a)', 'Corte e tratamento capilar', 'BEAUTY', '["corte cabelo"]', 'active', 'migration'),
    ('beauty-makeup', 'Maquiador(a)', 'Maquiagem profissional', 'BEAUTY', '["maquiagem"]', 'active', 'migration'),
    ('beauty-eyebrow', 'Design de Sobrancelhas', 'Modelagem de sobrancelhas', 'BEAUTY', '["sobrancelha"]', 'active', 'migration'),
    ('beauty-massage', 'Massagem', 'Massagens relaxantes', 'BEAUTY', '["massagem"]', 'active', 'migration'),
    -- MOVING_AND_ASSEMBLY (5)
    ('logistic-moving-help', 'Ajudante de Mudança', 'Auxílio em mudanças', 'MOVING_AND_ASSEMBLY', '["ajuda mudança"]', 'active', 'migration'),
    ('logistic-freight', 'Fretes / Carretos', 'Transporte de cargas', 'MOVING_AND_ASSEMBLY', '["carreto"]', 'active', 'migration'),
    ('logistic-moto-delivery', 'Entrega por Moto', 'Entrega rápida', 'MOVING_AND_ASSEMBLY', '["motoboy"]', 'active', 'migration'),
    ('logistic-small-delivery', 'Pequenas Entregas', 'Entrega local', 'MOVING_AND_ASSEMBLY', '["entrega encomenda"]', 'active', 'migration'),
    ('logistic-heavy-moving', 'Mudança Completa', 'Serviço completo de mudança', 'MOVING_AND_ASSEMBLY', '["mudança completa"]', 'active', 'migration'),
    -- CAREGIVING (4)
    ('care-babysitter', 'Babá', 'Cuidado de crianças', 'CAREGIVING', '["babysitter"]', 'active', 'migration'),
    ('care-elder', 'Cuidador de Idosos', 'Cuidado de idosos', 'CAREGIVING', '["cuidador idoso"]', 'active', 'migration'),
    ('care-pet-sitter', 'Cuidador de Pets', 'Cuidado de animais', 'CAREGIVING', '["pet sitter"]', 'active', 'migration'),
    ('care-dog-walker', 'Passeador de Cachorro', 'Passeio com cães', 'CAREGIVING', '["dog walker"]', 'active', 'migration'),
    -- LESSONS (4)
    ('lesson-private-tutor', 'Aulas Particulares', 'Reforço escolar', 'LESSONS', '["professor particular"]', 'active', 'migration'),
    ('lesson-music', 'Aula de Música', 'Aulas de instrumentos', 'LESSONS', '["aula violão"]', 'active', 'migration'),
    ('lesson-fitness', 'Personal Trainer', 'Treino físico', 'LESSONS', '["personal trainer"]', 'active', 'migration'),
    ('lesson-sports', 'Instrutor de Esportes', 'Aulas de esportes (tênis, beach tênis, vôlei, etc.)', 'LESSONS', '["aula tênis", "beach tennis", "aula vôlei"]', 'active', 'migration'),
    -- AUTOMOTIVE (3)
    ('auto-mechanic', 'Mecânico', 'Manutenção automotiva', 'AUTOMOTIVE', '["mecânico"]', 'active', 'migration'),
    ('auto-electric', 'Elétrica Automotiva', 'Manutenção elétrica automotiva', 'AUTOMOTIVE', '["bateria carro"]', 'active', 'migration'),
    ('auto-car-wash', 'Lavagem de Carro', 'Limpeza automotiva', 'AUTOMOTIVE', '["lava jato"]', 'active', 'migration');

------------------------------------------------------------
-- Seed data: system configuration
------------------------------------------------------------

INSERT INTO system_configuration (key, value) VALUES
    ('catalog.auto-provisioning.enabled', 'false'),
    ('catalog.version', '1');
