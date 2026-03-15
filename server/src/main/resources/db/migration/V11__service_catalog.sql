-- Service Categories
CREATE TABLE service_categories (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Canonical Services
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

-- Unmatched Service Signals
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
    CONSTRAINT valid_safety CHECK (safety_classification IN ('safe', 'unsafe', 'uncertain') OR safety_classification IS NULL)
);

CREATE INDEX idx_unmatched_signals_provisional ON unmatched_service_signals(provisional_service_id);
CREATE INDEX idx_unmatched_signals_created ON unmatched_service_signals(created_at);

-- System Configuration
CREATE TABLE system_configuration (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed categories (7 — excluding OTHER)
INSERT INTO service_categories (id, display_name, sort_order) VALUES
    ('CLEANING', 'Limpeza', 1),
    ('REPAIRS', 'Reparos', 2),
    ('PAINTING', 'Pintura', 3),
    ('GARDEN', 'Jardim', 4),
    ('EVENTS', 'Eventos', 5),
    ('BEAUTY', 'Beleza', 6),
    ('MOVING_AND_ASSEMBLY', 'Mudanças e Montagem', 7);

-- Seed services (22 — excluding other-general)
INSERT INTO canonical_services (id, display_name, description, category_id, aliases, status, created_by) VALUES
    ('clean-house', 'Limpeza Residencial', 'Limpeza de casas e apartamentos', 'CLEANING', '["diarista", "faxina", "limpeza de casa"]', 'active', 'migration'),
    ('clean-post-construction', 'Limpeza Pós-Obra', 'Limpeza pesada após construções ou reformas', 'CLEANING', '["pós obra", "limpeza pesada"]', 'active', 'migration'),
    ('clean-sofa', 'Limpeza de Sofá', 'Higienização de estofados, sofás e poltronas', 'CLEANING', '["lavagem de sofá", "impermeabilização", "estofados"]', 'active', 'migration'),
    ('clean-land', 'Limpeza de Terreno', 'Retirada de entulho e mato de terrenos', 'CLEANING', '["roçagem de terreno", "limpar lote"]', 'active', 'migration'),
    ('repair-electrician', 'Eletricista Residencial', 'Instalações e reparos elétricos', 'REPAIRS', '["eletricista", "troca de fiação", "curto circuito"]', 'active', 'migration'),
    ('repair-shower', 'Troca de Chuveiro', 'Instalação ou troca de chuveiros elétricos', 'REPAIRS', '["instalar chuveiro", "chuveiro queimado"]', 'active', 'migration'),
    ('repair-plumber', 'Encanador / Hidráulica', 'Reparos de vazamentos e tubulações', 'REPAIRS', '["encanador", "vazamento", "desentupimento"]', 'active', 'migration'),
    ('repair-handyman', 'Marido de Aluguel', 'Pequenos reparos diversos em residências', 'REPAIRS', '["pequenos consertos", "pendurar quadros", "reparos gerais"]', 'active', 'migration'),
    ('repair-furniture-assembly', 'Montagem de Móveis', 'Montagem e desmontagem de móveis em geral', 'REPAIRS', '["montador de móveis", "armário", "guarda-roupa"]', 'active', 'migration'),
    ('paint-residential', 'Pintura Residencial', 'Pintura de paredes, tetos e fachadas residenciais', 'PAINTING', '["pintor", "pintura de casa", "envernizamento"]', 'active', 'migration'),
    ('paint-commercial', 'Pintura Comercial', 'Serviços de pintura para lojas e escritórios', 'PAINTING', '["pintura de loja", "pintura de galpão"]', 'active', 'migration'),
    ('garden-maintenance', 'Jardinagem', 'Manutenção de jardins e vasos', 'GARDEN', '["jardineiro", "cuidar de plantas"]', 'active', 'migration'),
    ('garden-pruning', 'Poda de Árvores', 'Corte e manutenção de árvores e arbustos', 'GARDEN', '["podar árvore", "corte de galhos"]', 'active', 'migration'),
    ('garden-mowing', 'Roçagem / Corte de Grama', 'Corte de grama e manutenção de gramados', 'GARDEN', '["cortar grama", "roçador"]', 'active', 'migration'),
    ('event-waiter', 'Garçom para Eventos', 'Serviço de garçom para festas e recepções', 'EVENTS', '["garçom", "atendimento de mesas"]', 'active', 'migration'),
    ('event-bartender', 'Bartender / Drinks', 'Preparo de drinks e coquetéis para eventos', 'EVENTS', '["barman", "coquetelaria"]', 'active', 'migration'),
    ('event-kitchen-assistant', 'Ajudante de Cozinha', 'Auxílio no preparo de alimentos em eventos', 'EVENTS', '["auxiliar de cozinha", "copa"]', 'active', 'migration'),
    ('beauty-manicure', 'Manicure e Pedicure', 'Cuidados com as unhas das mãos e pés', 'BEAUTY', '["unhas", "esmaltação", "manicure"]', 'active', 'migration'),
    ('beauty-hairdresser', 'Cabeleireiro(a)', 'Corte, coloração e tratamentos capilares', 'BEAUTY', '["corte de cabelo", "escova", "luzes"]', 'active', 'migration'),
    ('beauty-makeup', 'Maquiador(a)', 'Maquiagem profissional para eventos e fotos', 'BEAUTY', '["maquiagem", "make"]', 'active', 'migration'),
    ('logistic-moving-help', 'Ajudante de Mudança', 'Auxílio no carregamento e transporte de mudanças', 'MOVING_AND_ASSEMBLY', '["carreto", "ajuda com mudança"]', 'active', 'migration'),
    ('logistic-freight', 'Pequenos Fretes / Carretos', 'Transporte de cargas leves e móveis', 'MOVING_AND_ASSEMBLY', '["carreto", "fretinho", "entrega"]', 'active', 'migration');

-- Default configuration: auto-provisioning disabled
INSERT INTO system_configuration (key, value) VALUES
    ('catalog.auto-provisioning.enabled', 'false');
