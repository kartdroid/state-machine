package me.kartdroid

import me.kartdroid.StateMachineModule.logger
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


/**
 * @author [Karthick Chinnathambi](https://github.com/kartdroid)
 * @since 20/05/22.
 */
internal class ParallelWordMachineTest {

    object EmptyContext

    enum class WordState {
        LIST,
        UNDERLINE,
        BOLD,
        ITALIC
    }

    enum class ListState {
        NONE,
        BULLETS,
        NUMBERS
    }

    enum class UnderlineState {
        OFF,
        ON
    }

    enum class BoldState {
        OFF,
        ON
    }

    enum class ItalicState {
        OFF,
        ON
    }

    sealed class WordEvent {

        sealed class ListEvent : WordEvent() {
            object None : ListEvent()
            object Bullets : ListEvent()
            object Numbers : ListEvent()
        }

        sealed class UnderlineEvent : WordEvent() {
            object ToggleUnderline : UnderlineEvent()
        }

        sealed class BoldEvent : WordEvent() {
            object ToggleBold : BoldEvent()
        }

        sealed class ItalicsEvent : WordEvent() {
            object ToggleItalic : ItalicsEvent()
        }
    }

    private val listTransitionHandler: TransitionHandler<EmptyContext, ListState, WordEvent.ListEvent> =
        { _, state, event ->
            when (state) {
                ListState.NONE -> when (event) {
                    WordEvent.ListEvent.Bullets -> TransitionInfo(ListState.BULLETS)
                    WordEvent.ListEvent.Numbers -> TransitionInfo(ListState.NUMBERS)
                    else -> null
                }
                ListState.BULLETS -> when (event) {
                    WordEvent.ListEvent.None -> TransitionInfo(ListState.NONE)
                    WordEvent.ListEvent.Numbers -> TransitionInfo(ListState.NUMBERS)
                    else -> null
                }
                ListState.NUMBERS -> when (event) {
                    WordEvent.ListEvent.Bullets -> TransitionInfo(ListState.BULLETS)
                    WordEvent.ListEvent.None -> TransitionInfo(ListState.NONE)
                    else -> null
                }
            }
        }

    private val underlineTransitionHandler: TransitionHandler<EmptyContext, UnderlineState, WordEvent.UnderlineEvent> =
        { _, state, event ->
            when (state) {
                UnderlineState.OFF -> when (event) {
                    WordEvent.UnderlineEvent.ToggleUnderline -> TransitionInfo(UnderlineState.ON)
                }
                UnderlineState.ON -> when (event) {
                    WordEvent.UnderlineEvent.ToggleUnderline -> TransitionInfo(UnderlineState.OFF)
                }
            }
        }
    private val boldTransitionHandler: TransitionHandler<EmptyContext, BoldState, WordEvent.BoldEvent> =
        { _, state, event ->
            when (state) {
                BoldState.OFF -> when (event) {
                    WordEvent.BoldEvent.ToggleBold -> TransitionInfo(BoldState.ON)
                }
                BoldState.ON -> when (event) {
                    WordEvent.BoldEvent.ToggleBold -> TransitionInfo(BoldState.OFF)
                }
            }
        }
    private val italicsTransitionHandler: TransitionHandler<EmptyContext, ItalicState, WordEvent.ItalicsEvent> =
        { _, state, event ->
            when (state) {
                ItalicState.OFF -> when (event) {
                    WordEvent.ItalicsEvent.ToggleItalic -> TransitionInfo(ItalicState.ON)
                }
                ItalicState.ON -> when (event) {
                    WordEvent.ItalicsEvent.ToggleItalic -> TransitionInfo(ItalicState.OFF)
                }
            }
        }

    val wordMachine = StateMachine.create<EmptyContext, WordState, WordEvent>(EmptyContext) {
        subMachine<ListState, WordEvent.ListEvent>(WordState.LIST) {
            initial(ListState.NONE)
            transitionHandler(listTransitionHandler)
        }
        subMachine<UnderlineState, WordEvent.UnderlineEvent>(WordState.UNDERLINE) {
            initial(UnderlineState.OFF)
            transitionHandler(underlineTransitionHandler)
        }
        subMachine<BoldState, WordEvent.BoldEvent>(WordState.BOLD) {
            initial(BoldState.OFF)
            transitionHandler(boldTransitionHandler)
        }
        subMachine<ItalicState, WordEvent.ItalicsEvent>(WordState.ITALIC) {
            initial(ItalicState.OFF)
            transitionHandler(italicsTransitionHandler)
        }
    }

    @Test
    fun `given empty state, when any event is passed, machine should enter all states`() {
        // given
        val state = State.STATE_EMPTY
        logger.info { "currentState: $state" }
        // when
        val nextState = wordMachine.transition(state, "")
        logger.info { "nextState: $nextState" }
        // then

        Assertions.assertTrue(nextState.activeValues().size == WordState.values().size)
    }

    @Nested
    inner class ListMachineTest {

        @Test
        fun `given NONE state, when Numbers-event is passed, machine should enter NUMBERS state`() {
            // given
            val state = State.from(WordState.LIST to State.from(ListState.NONE))
            logger.info { "currentState: $state" }
            // when
            val nextState = wordMachine.transition(state, WordEvent.ListEvent.Numbers)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == WordState.values().size)
            Assertions.assertTrue(nextState.valueMap[WordState.LIST] == State.from(ListState.NUMBERS))
            Assertions.assertTrue(nextState.valueMap[WordState.UNDERLINE] == State.from(UnderlineState.OFF))
            Assertions.assertTrue(nextState.valueMap[WordState.BOLD] == State.from(BoldState.OFF))
            Assertions.assertTrue(nextState.valueMap[WordState.ITALIC] == State.from(ItalicState.OFF))
        }

        @Test
        fun `given NUMBERS state, when bullets-event is passed, machine should enter BULLETS state`() {
            // given
            val state = State.from(WordState.LIST to State.from(ListState.NUMBERS))
            logger.info { "currentState: $state" }
            // when
            val nextState = wordMachine.transition(state, WordEvent.ListEvent.Bullets)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == WordState.values().size)
            Assertions.assertTrue(nextState.valueMap[WordState.LIST] == State.from(ListState.BULLETS))
            Assertions.assertTrue(nextState.valueMap[WordState.UNDERLINE] == State.from(UnderlineState.OFF))
            Assertions.assertTrue(nextState.valueMap[WordState.BOLD] == State.from(BoldState.OFF))
            Assertions.assertTrue(nextState.valueMap[WordState.ITALIC] == State.from(ItalicState.OFF))
        }
    }

    @Nested
    inner class BoldMachineTest {

        @Test
        fun `given ON state, when wrong event is passed, machine should remain in ON state`() {
            // given
            val state = State.from(WordState.BOLD to State.from(BoldState.ON))
            logger.info { "currentState: $state" }
            // when
            val nextState = wordMachine.transition(state, WordEvent.ListEvent.Bullets)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == WordState.values().size)
            Assertions.assertTrue(nextState.valueMap[WordState.LIST] == State.from(ListState.NONE))
            Assertions.assertTrue(nextState.valueMap[WordState.UNDERLINE] == State.from(UnderlineState.OFF))
            Assertions.assertTrue(nextState.valueMap[WordState.BOLD] == State.from(BoldState.ON))
            Assertions.assertTrue(nextState.valueMap[WordState.ITALIC] == State.from(ItalicState.OFF))
        }

        @Test
        fun `given OFF state, when toggle-bold-event is passed, machine should switch to ON state`() {
            // given
            val state = State.from(WordState.BOLD to State.from(BoldState.OFF))
            logger.info { "currentState: $state" }
            // when
            val nextState = wordMachine.transition(state, WordEvent.BoldEvent.ToggleBold)
            logger.info { "nextState: $nextState" }

            // then
            Assertions.assertTrue(nextState.activeValues().size == WordState.values().size)
            Assertions.assertTrue(nextState.valueMap[WordState.LIST] == State.from(ListState.NONE))
            Assertions.assertTrue(nextState.valueMap[WordState.UNDERLINE] == State.from(UnderlineState.OFF))
            Assertions.assertTrue(nextState.valueMap[WordState.BOLD] == State.from(BoldState.ON))
            Assertions.assertTrue(nextState.valueMap[WordState.ITALIC] == State.from(ItalicState.OFF))
        }
    }
}
