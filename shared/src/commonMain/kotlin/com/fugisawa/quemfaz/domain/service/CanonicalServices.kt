package com.fugisawa.quemfaz.domain.service

import com.fugisawa.quemfaz.core.id.CanonicalServiceId

/**
 * Central registry of all canonical services available in the platform.
 *
 * This object provides O(1) lookups for services by ID and display name,
 * and efficient searches by alias.
 *
 * Note: In production, this data should ideally be stored in a database
 * with caching. This in-memory approach is suitable for small, stable taxonomies.
 */
object CanonicalServices {
    private fun s(id: String, name: String, desc: String, cat: ServiceCategory, aliases: List<String> = emptyList()) =
        CanonicalService(CanonicalServiceId(id), name, desc, cat, aliases)

    /**
     * Complete list of all available canonical services.
     */
    val all = listOf(
        // Cleaning
        s("clean-house", "Limpeza Residencial", "Limpeza de casas e apartamentos", ServiceCategory.CLEANING, listOf("diarista", "faxina", "limpeza de casa")),
        s("clean-post-construction", "Limpeza Pós-Obra", "Limpeza pesada após construções ou reformas", ServiceCategory.CLEANING, listOf("pós obra", "limpeza pesada")),
        s("clean-sofa", "Limpeza de Sofá", "Higienização de estofados, sofás e poltronas", ServiceCategory.CLEANING, listOf("lavagem de sofá", "impermeabilização", "estofados")),
        s("clean-land", "Limpeza de Terreno", "Retirada de entulho e mato de terrenos", ServiceCategory.CLEANING, listOf("roçagem de terreno", "limpar lote")),

        // Repairs
        s("repair-electrician", "Eletricista Residencial", "Instalações e reparos elétricos", ServiceCategory.REPAIRS, listOf("eletricista", "troca de fiação", "curto circuito")),
        s("repair-shower", "Troca de Chuveiro", "Instalação ou troca de chuveiros elétricos", ServiceCategory.REPAIRS, listOf("instalar chuveiro", "chuveiro queimado")),
        s("repair-plumber", "Encanador / Hidráulica", "Reparos de vazamentos e tubulações", ServiceCategory.REPAIRS, listOf("encanador", "vazamento", "desentupimento")),
        s("repair-handyman", "Marido de Aluguel", "Pequenos reparos diversos em residências", ServiceCategory.REPAIRS, listOf("pequenos consertos", "pendurar quadros", "reparos gerais")),
        s("repair-furniture-assembly", "Montagem de Móveis", "Montagem e desmontagem de móveis em geral", ServiceCategory.REPAIRS, listOf("montador de móveis", "armário", "guarda-roupa")),

        // Painting
        s("paint-residential", "Pintura Residencial", "Pintura de paredes, tetos e fachadas residenciais", ServiceCategory.PAINTING, listOf("pintor", "pintura de casa", "envernizamento")),
        s("paint-commercial", "Pintura Comercial", "Serviços de pintura para lojas e escritórios", ServiceCategory.PAINTING, listOf("pintura de loja", "pintura de galpão")),

        // Garden
        s("garden-maintenance", "Jardinagem", "Manutenção de jardins e vasos", ServiceCategory.GARDEN, listOf("jardineiro", "cuidar de plantas")),
        s("garden-pruning", "Poda de Árvores", "Corte e manutenção de árvores e arbustos", ServiceCategory.GARDEN, listOf("podar árvore", "corte de galhos")),
        s("garden-mowing", "Roçagem / Corte de Grama", "Corte de grama e manutenção de gramados", ServiceCategory.GARDEN, listOf("cortar grama", "roçador")),

        // Events
        s("event-waiter", "Garçom para Eventos", "Serviço de garçom para festas e recepções", ServiceCategory.EVENTS, listOf("garçom", "atendimento de mesas")),
        s("event-bartender", "Bartender / Drinks", "Preparo de drinks e coquetéis para eventos", ServiceCategory.EVENTS, listOf("barman", "coquetelaria")),
        s("event-kitchen-assistant", "Ajudante de Cozinha", "Auxílio no preparo de alimentos em eventos", ServiceCategory.EVENTS, listOf("auxiliar de cozinha", "copa")),

        // Beauty
        s("beauty-manicure", "Manicure e Pedicure", "Cuidados com as unhas das mãos e pés", ServiceCategory.BEAUTY, listOf("unhas", "esmaltação", "manicure")),
        s("beauty-hairdresser", "Cabeleireiro(a)", "Corte, coloração e tratamentos capilares", ServiceCategory.BEAUTY, listOf("corte de cabelo", "escova", "luzes")),
        s("beauty-makeup", "Maquiador(a)", "Maquiagem profissional para eventos e fotos", ServiceCategory.BEAUTY, listOf("maquiagem", "make")),

        // Moving / Logistics
        s("logistic-moving-help", "Ajudante de Mudança", "Auxílio no carregamento e transporte de mudanças", ServiceCategory.MOVING_AND_ASSEMBLY, listOf("carreto", "ajuda com mudança")),
        s("logistic-freight", "Pequenos Fretes / Carretos", "Transporte de cargas leves e móveis", ServiceCategory.MOVING_AND_ASSEMBLY, listOf("carreto", "fretinho", "entrega")),

        // Others
        s("other-general", "Outros Serviços", "Serviços não listados anteriormente", ServiceCategory.OTHER, listOf("diverso"))
    )

    // O(1) lookup indices
    private val byId: Map<CanonicalServiceId, CanonicalService> = all.associateBy { it.id }
    private val byDisplayName: Map<String, CanonicalService> = all.associateBy { it.displayName.lowercase() }

    // Alias index for efficient searching
    private val byAlias: Map<String, List<CanonicalService>> = buildMap<String, MutableList<CanonicalService>> {
        all.forEach { service ->
            service.baseAliases.forEach { alias ->
                getOrPut(alias.lowercase()) { mutableListOf() }.add(service)
            }
        }
    }

    /**
     * Finds a service by its unique ID.
     * @return The service if found, null otherwise.
     */
    fun findById(id: CanonicalServiceId): CanonicalService? = byId[id]

    /**
     * Finds a service by its display name (case-insensitive).
     * @return The service if found, null otherwise.
     */
    fun findByDisplayName(name: String): CanonicalService? = byDisplayName[name.lowercase()]

    /**
     * Finds all services that match the given alias (case-insensitive).
     * @return List of matching services, empty if none found.
     */
    fun findByAlias(alias: String): List<CanonicalService> =
        byAlias[alias.lowercase()] ?: emptyList()

    /**
     * Finds all services in a given category.
     * @return List of services in the category.
     */
    fun findByCategory(category: ServiceCategory): List<CanonicalService> =
        all.filter { it.category == category }

    /**
     * Searches for services matching the query string.
     * Checks display name, aliases, and description.
     * @return List of matching services, ordered by relevance.
     */
    fun search(query: String): List<CanonicalService> {
        if (query.isBlank()) return emptyList()

        val lowerQuery = query.lowercase().trim()
        val results = mutableSetOf<Pair<CanonicalService, Int>>() // (service, score)

        all.forEach { service ->
            var score = 0

            // Exact display name match (highest priority)
            if (service.displayName.lowercase() == lowerQuery) {
                score += 100
            } else if (service.displayName.lowercase().contains(lowerQuery)) {
                score += 50
            }

            // Alias matches
            service.baseAliases.forEach { alias ->
                if (alias.lowercase() == lowerQuery) {
                    score += 80
                } else if (alias.lowercase().contains(lowerQuery)) {
                    score += 40
                }
            }

            // Description match (lowest priority)
            if (service.description.lowercase().contains(lowerQuery)) {
                score += 10
            }

            if (score > 0) {
                results.add(service to score)
            }
        }

        return results.sortedByDescending { it.second }.map { it.first }
    }
}
