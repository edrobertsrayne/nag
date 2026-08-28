package dev.nag.data

import kotlinx.coroutines.flow.Flow

interface NagRepository {

    val streak: Flow<Int>
}
