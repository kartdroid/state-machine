package me.kartdroid

import me.kartdroid.StateMachineModule.logger
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


/**
 * @author [Karthick Chinnathambi](https://github.com/kartdroid)
 * @since 20/05/22.
 */
internal class SimpleTrafficSignalMachineTest {

    enum class TrafficLightState {
        RED,
        YELLOW,
        GREEN
    }

    class TrafficContext

    sealed class TrafficLightEvent {
        object TIMER : TrafficLightEvent()
    }

    private val trafficMachine =
        StateMachine.create<TrafficContext, TrafficLightState, TrafficLightEvent>(TrafficContext()) {
            initial(TrafficLightState.RED)
            transitionHandler { _, state, event ->
                when (state) {
                    TrafficLightState.RED -> {
                        when (event) {
                            TrafficLightEvent.TIMER -> {
                                TransitionInfo(TrafficLightState.YELLOW) {
                                    logger.info { "Switched to YELLOW" }
                                }
                            }
                        }
                    }
                    TrafficLightState.YELLOW -> {
                        when (event) {
                            TrafficLightEvent.TIMER -> {
                                TransitionInfo(TrafficLightState.GREEN) {
                                    logger.info { "Switched to GREEN" }
                                }
                            }
                        }
                    }
                    TrafficLightState.GREEN -> {
                        when (event) {
                            TrafficLightEvent.TIMER -> {
                                TransitionInfo(TrafficLightState.RED) {
                                    logger.info { "Switched to RED" }
                                }
                            }
                        }
                    }
                }
            }
        }

    @Test
    fun `given red state , on timer, transform to yellow`() {

        // given
        val state = State.from(TrafficLightState.RED)

        // when
        val nextState = trafficMachine.transition(state, TrafficLightEvent.TIMER)

        // then
        assertTrue(nextState.activeValues().size == 1)
        assertTrue(nextState.activeValues()[0] == TrafficLightState.YELLOW)
    }

    @Test
    fun `given yellow state , on timer, transform to green`() {

        // given
        val state = State.from(TrafficLightState.YELLOW)

        // when
        val nextState = trafficMachine.transition(state, TrafficLightEvent.TIMER)

        // then
        assertTrue(nextState.activeValues().size == 1)
        assertTrue(nextState.activeValues()[0] == TrafficLightState.GREEN)
    }

    @Test
    fun `given green state , on timer, transform to red`() {

        // given
        val state = State.from(TrafficLightState.GREEN)

        // when
        val nextState = trafficMachine.transition(state, TrafficLightEvent.TIMER)

        // then
        assertTrue(nextState.activeValues().size == 1)
        assertTrue(nextState.activeValues()[0] == TrafficLightState.RED)
    }

    enum class WrongState {
        WRONG
    }

    @Test
    fun `given wrong state , on timer, do not transform`() {
        // given
        val state = State.from(WrongState.WRONG)

        // when
        val nextState = trafficMachine.transition(state, TrafficLightEvent.TIMER)

        // then
        assertTrue(nextState.activeValues().size == 1)
        assertTrue(nextState.activeValues()[0] == TrafficLightState.RED)
    }
}
