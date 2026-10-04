package com.reisaraujo_miguel.studypulse

import com.reisaraujo_miguel.studypulse.components.ChronometerViewModel
import com.reisaraujo_miguel.studypulse.components.SessionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
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
        assertThat(state.currentMode, `is`(SessionMode.STUDY))
        assertThat(state.isRunning, `is`(false))
        assertThat(state.totalStudyMillis, `is`(0L))
        assertThat(state.totalRestMillis, `is`(0L))
        assertThat(state.currentSessionMillis, `is`(0L))
        assertThat(state.chunksCount, `is`(0))
    }

    @Test
    fun startStudy_updatesState() {
        viewModel.startStudy()
        val state = viewModel.uiState.value
        assertThat(state.isRunning, `is`(true))
        assertThat(state.chunksCount, `is`(1))
    }

    @Test
    fun switchToMode_incrementsChunksForStudy() {
        viewModel.startStudy()
        viewModel.switchToMode(SessionMode.REST)
        assertThat(viewModel.uiState.value.currentMode, `is`(SessionMode.REST))
        assertThat(viewModel.uiState.value.chunksCount, `is`(1))

        viewModel.switchToMode(SessionMode.STUDY)
        assertThat(viewModel.uiState.value.currentMode, `is`(SessionMode.STUDY))
        assertThat(viewModel.uiState.value.chunksCount, `is`(2))
    }

    @Test
    fun reset_resetsAllState() {
        viewModel.startStudy()
        viewModel.switchToMode(SessionMode.REST)
        viewModel.reset()

        val state = viewModel.uiState.value
        assertThat(state.currentMode, `is`(SessionMode.STUDY))
        assertThat(state.isRunning, `is`(false))
        assertThat(state.totalStudyMillis, `is`(0L))
        assertThat(state.totalRestMillis, `is`(0L))
        assertThat(state.chunksCount, `is`(0))
    }

    @Test
    fun milestoneMessage_doesNotCrashWithShortLists() {
        val state = viewModel.uiState.value
        val shortStudyMilestones = listOf(1, 2)
        val shortRestReminders = listOf(1, 2)

        val studyMsg = state.milestoneMessage(shortStudyMilestones, shortRestReminders)
        assertThat(studyMsg, `is`(""))
    }

    @Test
    fun onTimerTick_accumulatesStudyTimeInStudyMode() {
        viewModel.startStudy()
        viewModel.onTimerTick(1000L)

        val state = viewModel.uiState.value
        assertThat(state.currentSessionMillis, `is`(1000L))
        assertThat(state.totalStudyMillis, `is`(1000L))
        assertThat(state.totalRestMillis, `is`(0L))
    }

    @Test
    fun onTimerTick_accumulatesRestTimeInRestMode() {
        viewModel.startStudy()
        viewModel.switchToMode(SessionMode.REST)
        viewModel.onTimerTick(2000L)

        val state = viewModel.uiState.value
        assertThat(state.currentSessionMillis, `is`(2000L))
        assertThat(state.totalStudyMillis, `is`(0L))
        assertThat(state.totalRestMillis, `is`(2000L))
    }

    @Test
    fun dismissAlarmDialog_hidesDialog() {
        viewModel.dismissAlarmDialog()
        assertThat(viewModel.uiState.value.showAlarmDialog, `is`(false))
    }

    @Test
    fun milestoneMessage_handlesEmptyMilestonesGracefully() {
        val state = viewModel.uiState.value.copy(currentSessionMillis = 60_000L)
        val msg = state.milestoneMessage(emptyList(), emptyList())
        assertThat(msg, `is`("Keep on studying!"))
    }
}
