package applications.actorcritic.agent

import environment.Environment
import applications.actorcritic.SoloArguments._
import base.Memory

import scala.util.Random

case class TableActorCriticAgent(initialEnvironment: Environment,
                                 stateActionValuePairMap: Map[String, List[ActionValuePair]] = Map(),
                                 epsilonRate: Double = actorEpsilonRate,
                                 actorEligibilities: Map[String, List[Double]] = Map(),
                                 criticEligibilities: Map[String, Double] = Map(),
                                 stateValueMap: Map[String, Double] = Map())
    extends ActorCriticAgent {

  def train(memories: List[Memory]): ActorCriticAgent = {
    val memory = memories.last

    // 1. Current environment
    val stateKey    = memory.environment.toString
    val actionIndex = memory.environment.possibleActions.indexOf(memory.action)

    // 1.1. Critic
    val stateValue       = stateValueMap.getOrElse(stateKey, Random.nextDouble())
    val newStateValueMap = stateValueMap + (stateKey -> stateValue)

    val newCriticEligibilities = criticEligibilities + (stateKey -> 1.0)

    // 1.2 Actor
    val stateActionValuePairList   = stateActionValuePairMap.getOrElse(stateKey, memory.environment.possibleActions.map(action => ActionValuePair(action)))
    val newStateActionValuePairMap = stateActionValuePairMap + (stateKey -> stateActionValuePairList)

    val actorEligibilityList    = actorEligibilities.getOrElse(stateKey, memory.environment.possibleActions.map(_ => 0.0))
    val newActorEligibilityList = actorEligibilityList.updated(actionIndex, 1.0)
    val newActorEligibilities   = actorEligibilities + (stateKey -> newActorEligibilityList)

    // 2. Next environment
    val nextStateKey = memory.nextEnvironment.toString

    // 2.1 Critic
    val nextStateValue          = stateValueMap.getOrElse(nextStateKey, Random.nextDouble())
    val temporalDifferenceError = memory.nextEnvironment.reward + (criticDiscountFactor * nextStateValue) - stateValue

    // 3. New applications.actorcritic.agent
    val newAgent = TableActorCriticAgent(
      initialEnvironment,
      stateActionValuePairMap = newStateActionValuePairMap,
      epsilonRate = epsilonRate,
      actorEligibilities = newActorEligibilities,
      criticEligibilities = newCriticEligibilities,
      stateValueMap = newStateValueMap
    )

    step(memories, newAgent, temporalDifferenceError)
  }

  def step(memories: List[Memory], currentAgent: TableActorCriticAgent, temporalDifferenceError: Double, memoryIndex: Int = 0): ActorCriticAgent = {
    if (memoryIndex > memories.length - 1) {
      currentAgent
    } else {
      val memory   = memories(memoryIndex)
      val stateKey = memory.environment.toString

      // Critic
      val stateValue        = currentAgent.stateValueMap(stateKey)
      val criticEligibility = currentAgent.criticEligibilities(stateKey)

      val newStateValue    = stateValue + (tableCriticLearningRate * temporalDifferenceError * criticEligibility)
      val newStateValueMap = currentAgent.stateValueMap + (stateKey -> newStateValue)

      val newCriticEligibility   = criticDiscountFactor * criticEligibilityDecayRate * criticEligibility
      val newCriticEligibilities = currentAgent.criticEligibilities + (stateKey -> newCriticEligibility)

      // Actor
      val actionIndex = memory.environment.possibleActions.indexOf(memory.action)

      val stateActionValuePairList = currentAgent.stateActionValuePairMap(stateKey)
      val stateActionValuePair     = stateActionValuePairList(actionIndex)
      val value                = stateActionValuePair.value

      val actorEligibilityList = currentAgent.actorEligibilities(stateKey)
      val actorEligibility     = actorEligibilityList(actionIndex)

      val newValue                = value + (actorLearningRate * temporalDifferenceError * actorEligibility)
      val newStateActionValuePair     = ActionValuePair(memory.action, newValue)
      val newStateActionValuePairList = stateActionValuePairList.updated(actionIndex, newStateActionValuePair)
      val newStateActionValuePairMap  = currentAgent.stateActionValuePairMap + (stateKey -> newStateActionValuePairList)

      val newActorEligibility     = actorDiscountFactor * actorEligibilityDecayRate * actorEligibility
      val newActorEligibilityList = actorEligibilityList.updated(actionIndex, newActorEligibility)
      val newActorEligibilities   = currentAgent.actorEligibilities + (stateKey -> newActorEligibilityList)

      val newAgent = TableActorCriticAgent(
        initialEnvironment,
        stateActionValuePairMap = newStateActionValuePairMap,
        epsilonRate = epsilonRate,
        actorEligibilities = newActorEligibilities,
        criticEligibilities = newCriticEligibilities,
        stateValueMap = newStateValueMap
      )

      step(memories, newAgent, temporalDifferenceError, memoryIndex = memoryIndex + 1)
    }
  }

  def updateEpsilonRate(): ActorCriticAgent = {
    val potentialNewEpsilonRate = epsilonRate * actorEpsilonDecayRate
    val newEpsilonRate          = if (potentialNewEpsilonRate >= actorEpsilonMinRate) potentialNewEpsilonRate else actorEpsilonMinRate
    TableActorCriticAgent(initialEnvironment, stateActionValuePairMap = stateActionValuePairMap, epsilonRate = newEpsilonRate, stateValueMap = stateValueMap)
  }

  def removeEpsilon(): ActorCriticAgent = {
    TableActorCriticAgent(initialEnvironment, stateActionValuePairMap = stateActionValuePairMap, epsilonRate = 0.0, stateValueMap = stateValueMap)
  }

  def resetEligibilities(): ActorCriticAgent = {
    TableActorCriticAgent(initialEnvironment, stateActionValuePairMap = stateActionValuePairMap, epsilonRate = epsilonRate, stateValueMap = stateValueMap)
  }

  override def toString: String = {
    s"StateActionValueMap: ${stateActionValuePairMap.size}, StateValueMap: ${stateValueMap.size}, EpsilonRate: $epsilonRate"
  }
}
