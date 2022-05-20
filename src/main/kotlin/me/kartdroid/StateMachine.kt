package me.kartdroid

import java.util.LinkedList
import kotlin.reflect.KClass

// NOTE : Dokka Documentation is pending on this file

/**
 *  Type that represents a lambda that processes all the Transitions for a particular StateMachine.
 *
 * @author [Karthick Chinnathambi](https://github.com/kartdroid)
 * @since 20/05/22.
 */
typealias TransitionHandler<TContext, TState, TEvent> = (
    context: TContext,
    state: TState,
    event: TEvent
) -> TransitionInfo<TState>?

/**
 * Type that Represents a SideEffect for particular Transition
 */
typealias SideEffect = () -> Unit

private val EMPTY_SIDE_EFFECT: SideEffect = {}

/**
 * Represents the New State and Side Effect pair for any transition
 */
data class TransitionInfo<TState : Enum<TState>>(
    val value: TState,
    val sideEffect: SideEffect = EMPTY_SIDE_EFFECT
)

/**
 * The State information on State Machine
 */
data class State<TState : Enum<TState>>(
    val valueMap: Map<TState, State<*>> // List<out Pair<out TState, State<*>>>
) {

    // constructor(vararg valuePairs: Pair<TState, State<*>>) : this(mapOf(*valuePairs))

    fun <T : Enum<T>> belongsToType(type: KClass<T>): Boolean {
        if (this == STATE_EMPTY) {
            return false
        }
        return valueMap.keys.isEmpty() || type == valueMap.keys.iterator().next()::class
    }

    fun activeValues(): List<TState> {
        if (this == STATE_EMPTY) {
            return emptyList()
        }
        return valueMap.keys.toList()
    }

    fun subStateForAssociatedType(enumType: Enum<*>): State<*> {
        do {
            if (this == STATE_EMPTY) {
                break
            }
            for (value in valueMap) {
                if (value.key == enumType) {
                    return value.value
                }
            }
        } while (false)

        return STATE_EMPTY
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("{\n")
        valueMap.forEach {
            builder.append("${it.key} : ${if (it.value == STATE_EMPTY) "(NA)\n" else it.value.toString()}")
        }
        builder.append("}\n")
        // return if (valueMap.isEmpty()) "*" else "\n${valueMap}"
        // return prettyPrint(this)
        return builder.toString()
    }

    companion object {

        val STATE_EMPTY: State<*> = State<Nothing>(valueMap = emptyMap())

        fun <TState : Enum<TState>> from(value: TState): State<TState> {
            return State(mapOf(value to STATE_EMPTY))
        }

        fun <TState : Enum<TState>> from(values: Array<TState>): State<TState> {
            val valuePairs = values.map { it to STATE_EMPTY }
            return State(valuePairs.toMap())
        }

        fun <TState : Enum<TState>> from(valuePair: Pair<TState, State<*>>): State<TState> {
            return State(mapOf(valuePair.first to valuePair.second))
        }

        fun <TState : Enum<TState>> from(valuePairs: Array<Pair<TState, State<*>>>): State<TState> {
            return State(valuePairs.toMap())
        }

        fun <TState : Enum<TState>> from(valuePairs: List<Pair<TState, State<*>>>): State<TState> {
            val valueMap = mutableMapOf<TState, State<*>>()
            valuePairs.forEach {
                valueMap[it.first] = it.second
            }
            return State(valueMap)
        }
    }
}

/**
 *  Every StateMachine has a UNIQUE parent State ENUM among it's Peers
 */
class StateMachine<TContext : Any, TState : Enum<TState>, TEvent : Any> private constructor(
    builder: StateMachineBuilder<TContext, TState, TEvent>
) {

    enum class TypeMask(val maskValue: Int) {
        ATOMIC(1 shl 0),
        PARALLEL(1 shl 1),
        COMPOUND(1 shl 2);
    }

    private fun Int.hasType(mask: TypeMask): Boolean {
        return ((this and mask.maskValue) == mask.maskValue)
    }

    private fun Int.hasTypes(vararg masks: TypeMask): Boolean {
        for (mask in masks) {
            if (!hasType(mask)) {
                return false
            }
        }
        return true
    }

    private enum class ParentState {
        ROOT,

        /**
         * A machine that simply wraps up multiple machines and only has a type (parallel (or) compound)
         * Similar to root ??
         * What will be it's ID ?? the name of the state it is associated with will be it's ID??
         */
        CONTAINER
    }

    private val context: TContext = builder.context
    private val stateType: KClass<TState> = builder.stateType
    private val eventType: KClass<TEvent> = builder.eventType

    /**
     * Parent State for which the current StateMachine is valid / active
     */
    private val validParentState: Enum<*> = builder.validParentState

    /**
     * All possible State Transitions of the current state machine
     */
    private val possibleStates: Array<TState> = builder.possibleStates

    /**
     * The initial state to start with, whenever this StateMachine is newly active
     */
    private val initalState: TState? = builder.initialState
    private val childMachines: List<StateMachine<TContext, *, *>> = builder.childMachines

    private val machineType: Int

    init {
        machineType = when (initalState) {
            null -> when {
                childMachines.isEmpty() -> TypeMask.PARALLEL.maskValue or TypeMask.ATOMIC.maskValue
                else -> TypeMask.PARALLEL.maskValue
            }
            else -> when {
                childMachines.isEmpty() -> TypeMask.COMPOUND.maskValue or TypeMask.ATOMIC.maskValue
                else -> TypeMask.COMPOUND.maskValue
            }
        }
    }

    private fun subMachineAssociatedWithState(state: TState): StateMachine<TContext, *, *>? {
        for (machine in childMachines) {
            if (machine.validParentState == state) {
                return machine
            }
        }
        return null
    }

//  private fun childStateFor(parentState: State<*>): State<*> {
//    return (parentState.valueMap[validParentState] ?: State.STATE_EMPTY)
//  }

    private val transitionHandler: TransitionHandler<TContext, TState, TEvent> =
        builder.transitionHandler ?: { _, _, _ ->
            null
        }

    /**
     * A machine can have following scenarios :
     * -  No Parallel States + No Sub-Machine
     * -  Parallel States + No Sub-Machine
     * -  No Parallel States + Sub-Machine associated for Each/Some State (a Container (or) Non-Container Machine)
     * -  Parallel States + Sub-Machine associated for Each/Some State (a Container (or) Non-Container Machine)
     *
     * Assumptions :
     * -  If passed State<*> object has in-sufficient info, assume initial values for those machines
     * -  If Passed State<*> object doesn't have proper hierarchy , assume insufficient info and process so that
     * the returned object will be a completely formed state object
     *
     * Passed State Object Assumptions :
     *  -  the keys of the passed state reflect current StateMachine
     *  -  the values(if non-empty) reflect state of sub-state machine associated with one of the State
     *
     * Special Cases :
     *  -  StateMachine with parallel state doesn't have any transition on their own but forward events to their
     *    parallel states submachine.
     */
    fun <T : Enum<in T>> transition(currentState: State<in T>, event: Any): State<*> {
        return if (currentState.belongsToType(stateType)) {
            transitionToNextState(currentState, event)
        } else {
            transitionToInitialState(currentState, event)
        }
    }

    private fun <T : Enum<in T>> transitionToInitialState(currentState: State<in T>, event: Any): State<*> {
        StateMachineModule.logger.info { "Taking initial state path for machine associated with $validParentState" }
        // Initial State Setting Logic. No Transition Handler Invoked for this path
        return when {
            machineType.hasType(TypeMask.ATOMIC) -> {
                return transitionToInitialForAtomicState(currentState, event)
            }
            machineType.hasTypes(TypeMask.PARALLEL) -> {
                transitionToInitialForParallelState(currentState, event)
            }
            else -> { // machineType.hasTypes(TypeMask.COMPOUND)
                transitionToInitialForCompoundState(currentState, event)
            }
        }
    }

    private fun <T : Enum<in T>> transitionToInitialForAtomicState(
        @Suppress("UNUSED_PARAMETER") currentState: State<in T>,
        @Suppress("UNUSED_PARAMETER") event: Any
    ): State<*> {
        return if (machineType.hasType(TypeMask.PARALLEL)) {
            State.from(possibleStates)
        } else { // if(machineType.hasType(TypeMask.COMPOUND))
            // TECH-DEBT: Ensure safety during construction of StateMachine for initialState
            State.from(initalState!!)
        }
    }

    private fun <T : Enum<in T>> transitionToInitialForParallelState(currentState: State<in T>, event: Any): State<*> {
        val childStates = possibleStates.map {
            val subMachine = subMachineAssociatedWithState(it)
            val subState: State<*> = subMachine?.transition(currentState, event) ?: State.STATE_EMPTY
            it to subState
        }
        return State.from(childStates)
    }

    private fun <T : Enum<in T>> transitionToInitialForCompoundState(currentState: State<in T>, event: Any): State<*> {
        initalState?.let {
            val subMachine = subMachineAssociatedWithState(initalState)
            val subState: State<*> = subMachine?.transition(currentState, event) ?: State.STATE_EMPTY
            return State.from(initalState to subState)
        }
        throw IllegalStateException("Compound machine associated with: $validParentState doesn't have initialState ")
    }

    private fun <T : Enum<in T>> transitionToNextState(currentState: State<in T>, event: Any): State<*> {
        @Suppress("UNCHECKED_CAST")
        val myState: State<TState> = currentState as State<TState>
        return when {
            machineType.hasTypes(TypeMask.ATOMIC) -> {
                transitionToNextForAtomicState(myState, event)
            }
            machineType.hasTypes(TypeMask.PARALLEL) -> {
                transitionToNextForParallelState(myState, event)
            }
            else -> { // machineType.hasTypes(TypeMask.COMPOUND)
                transitionToNextForCompoundState(myState, event)
            }
        }
    }

    private fun transitionToNextForAtomicState(currentState: State<TState>, event: Any): State<*> {
        return when {
            machineType.hasTypes(TypeMask.ATOMIC, TypeMask.PARALLEL) -> {
                // Note : Atomic and Parallel is actually meaningless isn't it ?
                State.from(possibleStates)
            }
            machineType.hasTypes(TypeMask.ATOMIC, TypeMask.COMPOUND) -> {
                // Note: Only 1 entry should be found in StateMap (that is active)
                val activeState: TState = currentState.activeValues()[0]
                // TECH-DEBT: Check for an alternate approach to check compatibility without using reflection
                @Suppress("UNCHECKED_CAST")
                val transformedEvent = event as? TEvent
                var nextState = activeState
                if (transformedEvent != null) {
                    val transitionInfo: TransitionInfo<TState>? =
                        try {
                            transitionHandler(context, activeState, transformedEvent)
                        } catch (e: ClassCastException) {
                            null
                        }
                    transitionInfo?.let {
                        // TECH-DEBT: Send will change notification
                        nextState = transitionInfo.value
                        // TECH-DEBT: Send did change notification
                        transitionInfo.sideEffect()
                    }
                } else {
                    StateMachineModule.logger.warn { "Wrong Event Passed" }
                }
                return State.from(nextState)
            }
            else -> throw IllegalStateException("Transition to invalid state")
        }
    }

    private fun transitionToNextForParallelState(currentState: State<TState>, event: Any): State<*> {
        // Just forward to all sub-state machines
        val childStates = possibleStates.map {
            val subMachine = subMachineAssociatedWithState(it)
            if (subMachine != null) {
                val currentSubState: State<*> = currentState.subStateForAssociatedType(subMachine.validParentState)
                val nextSubState: State<*> = subMachine.transition(currentSubState, event)
                it to nextSubState
            } else {
                it to State.STATE_EMPTY
            }
        }
        return State.from(childStates)
    }

    private fun transitionToNextForCompoundState(currentState: State<TState>, event: Any): State<*> {
        // Note: Only 1 entry should be found in StateMap (that is active)
        val activeState: TState = currentState.activeValues()[0]

        @Suppress("UNCHECKED_CAST")
        val transformedEvent = event as? TEvent
        var nextState = activeState
        if (transformedEvent != null) {
            val transitionInfo: TransitionInfo<TState>? =
                try {
                    transitionHandler(context, activeState, transformedEvent)
                } catch (e: ClassCastException) {
                    null
                }
            transitionInfo?.let {
                // TECH-DEBT: Send will change notification
                nextState = transitionInfo.value
                // TECH-DEBT: Send did change notification
                transitionInfo.sideEffect()
            }
        }
        // forward state info to active child for transition
        val subMachine = subMachineAssociatedWithState(nextState)
        return if (subMachine != null) {
            val currentSubState: State<*> = currentState.subStateForAssociatedType(subMachine.validParentState)
            val subState: State<*> = subMachine.transition(currentSubState, event)
            State.from(nextState to subState)
        } else {
            State.from(nextState to State.STATE_EMPTY)
        }
    }

    companion object {

        inline fun <TContext : Any, reified TState : Enum<TState>, reified TEvent : Any> create(
            context: TContext,
            noinline builder: StateMachineBuilder<TContext, TState, TEvent>.() -> Unit
        ): StateMachine<TContext, TState, TEvent> {
            @Suppress("DEPRECATION")
            return create(context, TState::class, TEvent::class, enumValues(), builder)
        }

        @Deprecated(message = "This API is not meant for external Use, Please use the other create API")
        fun <TContext : Any, TState : Enum<TState>, TEvent : Any> create(
            context: TContext,
            stateType: KClass<TState>,
            eventType: KClass<TEvent>,
            possibleStates: Array<TState>,
            builder: StateMachineBuilder<TContext, TState, TEvent>.() -> Unit
        ): StateMachine<TContext, TState, TEvent> {
            return StateMachine(
                StateMachineBuilder(possibleStates, stateType, eventType, ParentState.ROOT, context).apply(builder)
            )
        }
    }

    class StateMachineBuilder<TContext : Any, TState : Enum<TState>, TEvent : Any>(
        internal val possibleStates: Array<TState>,
        internal val stateType: KClass<TState>,
        internal val eventType: KClass<TEvent>,
        internal val validParentState: Enum<*>,
        internal val context: TContext
    ) {
        internal var initialState: TState? = null
        internal var childMachines: LinkedList<StateMachine<TContext, *, *>> = LinkedList()
        internal var transitionHandler: TransitionHandler<TContext, TState, TEvent>? = null

        fun initial(initialState: TState) {
            this.initialState = initialState
        }

        @Deprecated(message = "This API is not meant for external Use, Please use the other subMachine API")
        fun <CState : Enum<CState>, CEvent : Any> subMachine(
            possibleStates: Array<CState>,
            stateType: KClass<CState>,
            eventType: KClass<CEvent>,
            validParentState: TState,
            builder: StateMachineBuilder<TContext, CState, CEvent>.() -> Unit
        ) {
            // TODO : Ensure type-safety without reflection libraries
            // require(eventType.sealedSubclasses.isNotEmpty()) { "EventType must be Sealed class" }
            childMachines.add(
                StateMachineBuilder<TContext, CState, CEvent>(possibleStates, stateType, eventType, validParentState, context)
                    .apply(builder)
                    .build()
            )
        }

        inline fun <reified CState : Enum<CState>, reified CEvent : Any> subMachine(
            validParentState: TState,
            noinline builder: StateMachineBuilder<TContext, CState, CEvent>.() -> Unit
        ) {
            @Suppress("DEPRECATION")
            subMachine(enumValues(), CState::class, CEvent::class, validParentState, builder)
        }

        fun transitionHandler(
            handler: TransitionHandler<TContext, TState, TEvent>?
        ) {
            this.transitionHandler = handler
        }

        private fun build(): StateMachine<TContext, TState, TEvent> {
            // NOTE: Sub-machines doesn't have a context.
            // They will receive root-context while transitionHandler is invoked.
            return StateMachine(this)
        }
    }
}
