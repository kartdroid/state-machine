package me.kartdroid

import me.kartdroid.StateMachineModule.logger
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


/**
 * @author [Karthick Chinnathambi](https://github.com/kartdroid)
 * @since 20/05/22.
 */
internal class HierarchicalTrafficSignalMachineTest {

    object EmptyContext
    enum class RoadSignalState {
        RED,
        YELLOW,
        GREEN,
    }

    enum class PedestrianSignalState {
        WALK,
        WAIT,
        STOP
    }

    sealed class SignalEvent {
        object TIMER : SignalEvent()
        object PEDTIMER : SignalEvent()
    }

    private val trafficMachine =
        StateMachine.create<EmptyContext, RoadSignalState, SignalEvent>(EmptyContext) {
            initial(RoadSignalState.GREEN)
            subMachine<PedestrianSignalState, SignalEvent>(RoadSignalState.RED) {
                initial(PedestrianSignalState.WALK)
                transitionHandler { _: EmptyContext, state: PedestrianSignalState, event: SignalEvent ->
                    when (state) {
                        PedestrianSignalState.WALK -> {
                            when (event) {
                                SignalEvent.PEDTIMER -> TransitionInfo(PedestrianSignalState.WAIT) {
                                    logger.info { "PED: Transforming to Wait state" }
                                }
                                else -> null
                            }
                        }
                        PedestrianSignalState.WAIT -> {
                            when (event) {
                                SignalEvent.PEDTIMER -> TransitionInfo(PedestrianSignalState.STOP) {
                                    logger.info { "PED: Transforming to Stop state" }
                                }
                                else -> null
                            }
                        }
                        PedestrianSignalState.STOP -> {
                            null
                        }
                    }
                }
            }
            transitionHandler { _, roadState, event ->
                when (roadState) {
                    RoadSignalState.GREEN -> {
                        when (event) {
                            SignalEvent.TIMER -> TransitionInfo(RoadSignalState.YELLOW) {
                                logger.info { "Transforming to YELLOW state" }
                            }
                            else -> null
                        }
                    }
                    RoadSignalState.YELLOW -> {
                        when (event) {
                            SignalEvent.TIMER -> TransitionInfo(RoadSignalState.RED) {
                                logger.info { "Transforming to RED state" }
                            }
                            else -> null
                        }
                    }
                    RoadSignalState.RED -> {
                        when (event) {
                            SignalEvent.TIMER -> TransitionInfo(RoadSignalState.GREEN) {
                                logger.info { "Transforming to GREEN state" }
                            }
                            else -> null
                        }
                    }
                }
            }
        }

    @Test
    fun `given empty state, on timer, initial state should be returned`() {
        // given
        val state = State.STATE_EMPTY
        logger.info { "currentState: $state" }

        // when
        val nextState = trafficMachine.transition(state, SignalEvent.TIMER)
        logger.info { "nextState: $nextState" }
        // then
        Assertions.assertTrue(nextState.activeValues().size == 1)
        Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.GREEN)
        Assertions.assertEquals(nextState.subStateForAssociatedType(nextState.activeValues()[0]), State.STATE_EMPTY)
    }

    @Test
    fun `given Green state, on timer, yellow state should be returned`() {
        // given
        val state = State.from(RoadSignalState.GREEN)
        logger.info { "currentState: $state" }

        // when
        val nextState = trafficMachine.transition(state, SignalEvent.TIMER)
        logger.info { "nextState: $nextState" }

        // then
        Assertions.assertTrue(nextState.activeValues().size == 1)
        Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.YELLOW)
        Assertions.assertEquals(nextState.subStateForAssociatedType(nextState.activeValues()[0]), State.STATE_EMPTY)
    }

    @Test
    fun `given Yellow state, on timer, Red state should be returned`() {
        // given
        val state = State.from(RoadSignalState.YELLOW)
        logger.info { "currentState: $state" }

        // when
        val nextState = trafficMachine.transition(state, SignalEvent.TIMER)
        logger.info { "nextState: $nextState" }

        // then
        Assertions.assertTrue(nextState.activeValues().size == 1)
        Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.RED)
        Assertions.assertEquals(
            nextState.subStateForAssociatedType(nextState.activeValues()[0]),
            State.from(PedestrianSignalState.WALK)
        )
    }

    @Test
    fun `given Red state, on timer, Green state should be returned`() {
        // given
        val state = State.from(RoadSignalState.RED)
        logger.info { "currentState: $state" }

        // when
        val nextState = trafficMachine.transition(state, SignalEvent.TIMER)
        logger.info { "nextState: $nextState" }

        // then
        Assertions.assertTrue(nextState.activeValues().size == 1)
        Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.GREEN)
        Assertions.assertEquals(nextState.subStateForAssociatedType(nextState.activeValues()[0]), State.STATE_EMPTY)
    }

    @Nested
    inner class PedestrianStateTest {

        @Test
        fun `given empty state, on PEDTIMER, WALK state should be returned`() {
            // given
            val state = State.from(RoadSignalState.RED)
            logger.info { "currentState: $state" }

            // when
            val nextState = trafficMachine.transition(state, SignalEvent.PEDTIMER)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == 1)
            Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.RED)
            Assertions.assertEquals(
                nextState.subStateForAssociatedType(nextState.activeValues()[0]),
                State.from(PedestrianSignalState.WALK)
            )
        }

        @Test
        fun `given WALK state, on PEDTIMER, WAIT state should be returned`() {
            // given
            val state = State.from(RoadSignalState.RED to State.from(PedestrianSignalState.WALK))
            logger.info { "currentState: $state" }
            // when
            val nextState = trafficMachine.transition(state, SignalEvent.PEDTIMER)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == 1)
            Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.RED)
            Assertions.assertEquals(
                nextState.subStateForAssociatedType(nextState.activeValues()[0]),
                State.from(PedestrianSignalState.WAIT)
            )
        }

        @Test
        fun `given WAIT state, on PEDTIMER, STOP state should be returned`() {
            // given
            val state = State.from(RoadSignalState.RED to State.from(PedestrianSignalState.WAIT))
            logger.info { "currentState: $state" }
            // when
            val nextState = trafficMachine.transition(state, SignalEvent.PEDTIMER)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == 1)
            Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.RED)
            Assertions.assertEquals(
                nextState.subStateForAssociatedType(nextState.activeValues()[0]),
                State.from(PedestrianSignalState.STOP)
            )
        }

        @Test
        fun `given STOP state, on PEDTIMER, no transition should occur`() {
            // given
            val state = State.from(RoadSignalState.RED to State.from(PedestrianSignalState.STOP))
            logger.info { "currentState: $state" }
            // when
            val nextState = trafficMachine.transition(state, SignalEvent.PEDTIMER)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == 1)
            Assertions.assertTrue(nextState.activeValues()[0] == RoadSignalState.RED)
            Assertions.assertEquals(state, nextState)
        }
    }
}
