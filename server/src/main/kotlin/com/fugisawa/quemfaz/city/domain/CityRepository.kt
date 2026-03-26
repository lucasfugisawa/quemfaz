package com.fugisawa.quemfaz.city.domain

import com.fugisawa.quemfaz.domain.city.City

interface CityRepository {
    fun findById(id: String): City?

    fun findByName(name: String): City?

    fun listActive(): List<City>
}
