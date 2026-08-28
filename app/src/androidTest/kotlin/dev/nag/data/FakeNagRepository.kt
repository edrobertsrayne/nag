package dev.nag.data

import kotlinx.coroutines.flow.MutableStateFlow

class FakeNagRepository(initialStreak: Int = 0) : NagRepository {

    override val streak = MutableStateFlow(initialStreak)
}
