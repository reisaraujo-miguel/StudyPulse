package com.reisaraujo_miguel.studypulse

import com.reisaraujo_miguel.studypulse.components.ChronometerViewModel
import com.reisaraujo_miguel.studypulse.components.SessionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChronometerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: ChronometerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ChronometerViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() {
        val state = viewModel.uiState.value
        assertEquals(SessionMode.STUDY, state.currentMode)
        assertFalse(state.isRunning)
        assertEquals(0L, state.totalStudyMillis)
        assertEquals(0L, state.totalRestMillis)
        assertEquals(0L, state.currentSessionMillis)
        assertEquals(0, state.chunksCount)
    }

    @Test
    fun startStudy_updatesState() {
        viewModel.startStudy()
        val state = viewModel.uiState.value
        assertTrue(state.isRunning)
        assertEquals(1, state.chunksCount)
    }

    @Test
    fun switchToMode_incrementsChunksForStudy() {
        viewModel.startStudy()
        viewModel.switchToMode(SessionMode.REST)
        assertEquals(SessionMode.REST, viewModel.uiState.value.currentMode)
        assertEquals(1, viewModel.uiState.value.chunksCount)

        viewModel.switchToMode(SessionMode.STUDY)
        assertEquals(SessionMode.STUDY, viewModel.uiState.value.currentMode)
        assertEquals(2, viewModel.uiState.value.chunksCount)
    }

    @Test
    fun reset_resetsAllState() {
        viewModel.startStudy()
        viewModel.switchToMode(SessionMode.REST)
        viewModel.reset()

        val state = viewModel.uiState.value
        assertEquals(SessionMode.STUDY, state.currentMode)
        assertFalse(state.isRunning)
        assertEquals(0L, state.totalStudyMillis)
        assertEquals(0L, state.totalRestMillis)
        assertEquals(0, state.chunksCount)
    }

    @Test
    fun milestoneMessage_doesNotCrashWithShortLists() {
        val state = viewModel.uiState.value
        val shortStudyMilestones = listOf(1, 2)
        val shortRestReminders = listOf(1, 2)

        val studyMsg = state.milestoneMessage(shortStudyMilestones, shortRestReminders)
        assertEquals("", studyMsg)
    }
}
