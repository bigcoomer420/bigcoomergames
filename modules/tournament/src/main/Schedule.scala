package lila.tournament

import chess.format.Fen
import chess.variant.Variant
import chess.Clock.{ LimitSeconds, IncrementSeconds }
import play.api.i18n.Lang

import lila.i18n.I18nKeys
import lila.rating.PerfType
import lila.gathering.Condition

case class Schedule(
    freq: Schedule.Freq,
    speed: Schedule.Speed,
    variant: Variant,
    position: Option[Fen.Opening],
    at: LocalDateTime,
    conditions: TournamentCondition.All = TournamentCondition.All.empty
):

  def name(full: Boolean = true)(using Lang): String =
    import Schedule.Freq.*
    import Schedule.Speed.*
    import lila.i18n.I18nKeys.tourname.*
    if (variant.standard && position.isEmpty)
      (conditions.minRating, conditions.maxRating) match
        case (None, None) =>
          (freq, speed) match
            case (Hourly, Rapid) if full      => hourlyRapidArena.txt()
            case (Hourly, Rapid)              => hourlyRapid.txt()
            case (Hourly, speed) if full      => hourlyXArena.txt(speed.trans)
            case (Hourly, speed)              => hourlyX.txt(speed.trans)
            case (Daily, Rapid) if full       => dailyRapidArena.txt()
            case (Daily, Rapid)               => dailyRapid.txt()
            case (Daily, Classical) if full   => dailyClassicalArena.txt()
            case (Daily, Classical)           => dailyClassical.txt()
            case (Daily, speed) if full       => dailyXArena.txt(speed.trans)
            case (Daily, speed)               => dailyX.txt(speed.trans)
            case (Eastern, Rapid) if full     => easternRapidArena.txt()
            case (Eastern, Rapid)             => easternRapid.txt()
            case (Eastern, Classical) if full => easternClassicalArena.txt()
            case (Eastern, Classical)         => easternClassical.txt()
            case (Eastern, speed) if full     => easternXArena.txt(speed.trans)
            case (Eastern, speed)             => easternX.txt(speed.trans)
            case (Weekly, Rapid) if full      => weeklyRapidArena.txt()
            case (Weekly, Rapid)              => weeklyRapid.txt()
            case (Weekly, Classical) if full  => weeklyClassicalArena.txt()
            case (Weekly, Classical)          => weeklyClassical.txt()
            case (Weekly, speed) if full      => weeklyXArena.txt(speed.trans)
            case (Weekly, speed)              => weeklyX.txt(speed.trans)
            case (Monthly, Rapid) if full     => monthlyRapidArena.txt()
            case (Monthly, Rapid)             => monthlyRapid.txt()
            case (Monthly, Classical) if full => monthlyClassicalArena.txt()
            case (Monthly, Classical)         => monthlyClassical.txt()
            case (Monthly, speed) if full     => monthlyXArena.txt(speed.trans)
            case (Monthly, speed)             => monthlyX.txt(speed.trans)
            case (Yearly, Rapid) if full      => yearlyRapidArena.txt()
            case (Yearly, Rapid)              => yearlyRapid.txt()
            case (Yearly, Classical) if full  => yearlyClassicalArena.txt()
            case (Yearly, Classical)          => yearlyClassical.txt()
            case (Yearly, speed) if full      => yearlyXArena.txt(speed.trans)
            case (Yearly, speed)              => yearlyX.txt(speed.trans)
            case (Shield, Rapid) if full      => rapidShieldArena.txt()
            case (Shield, Rapid)              => rapidShield.txt()
            case (Shield, Classical) if full  => classicalShieldArena.txt()
            case (Shield, Classical)          => classicalShield.txt()
            case (Shield, speed) if full      => xShieldArena.txt(speed.trans)
            case (Shield, speed)              => xShield.txt(speed.trans)
            case _ if full                    => xArena.txt(s"${freq.toString} ${speed.trans}")
            case _                            => s"${freq.toString} ${speed.trans}"
        case (Some(_), _) if full   => eliteXArena.txt(speed.trans)
        case (Some(_), _)           => eliteX.txt(speed.trans)
        case (_, Some(max)) if full => s"≤${max.rating} ${xArena.txt(speed.trans)}"
        case (_, Some(max))         => s"≤${max.rating} ${speed.trans}"
    else if (variant.standard)
      val n = position.flatMap(Thematic.byFen).fold(speed.trans) { pos =>
        s"${pos.family.name} ${speed.trans}"
      }
      if (full) xArena.txt(n) else n
    else
      freq match
        case Hourly if full  => hourlyXArena.txt(variant.name)
        case Hourly          => hourlyX.txt(variant.name)
        case Daily if full   => dailyXArena.txt(variant.name)
        case Daily           => dailyX.txt(variant.name)
        case Eastern if full => easternXArena.txt(variant.name)
        case Eastern         => easternX.txt(variant.name)
        case Weekly if full  => weeklyXArena.txt(variant.name)
        case Weekly          => weeklyX.txt(variant.name)
        case Monthly if full => monthlyXArena.txt(variant.name)
        case Monthly         => monthlyX.txt(variant.name)
        case Yearly if full  => yearlyXArena.txt(variant.name)
        case Yearly          => yearlyX.txt(variant.name)
        case Shield if full  => xShieldArena.txt(variant.name)
        case Shield          => xShield.txt(variant.name)
        case _ =>
          val n = s"${freq.name} ${variant.name}"
          if (full) xArena.txt(n) else n

  def day = at.withTimeAtStartOfDay

  def sameSpeed(other: Schedule) = speed == other.speed

  def similarSpeed(other: Schedule) = Schedule.Speed.similar(speed, other.speed)

  def sameVariant(other: Schedule) = variant.id == other.variant.id

  def sameVariantAndSpeed(other: Schedule) = sameVariant(other) && sameSpeed(other)

  def sameFreq(other: Schedule) = freq == other.freq

  def sameConditions(other: Schedule) = conditions == other.conditions

  def sameMaxRating(other: Schedule) = conditions sameMaxRating other.conditions

  def similarConditions(other: Schedule) = conditions similar other.conditions

  def sameDay(other: Schedule) = day == other.day

  def hasMaxRating = conditions.maxRating.isDefined

  def similarTo(other: Schedule) =
    similarSpeed(other) && sameVariant(other) && sameFreq(other) && sameConditions(other)

  def perfType = PerfType.byVariant(variant) | Schedule.Speed.toPerfType(speed)

  def plan                                  = Schedule.Plan(this, None)
  def plan(build: Tournament => Tournament) = Schedule.Plan(this, build.some)

  override def toString = s"$freq ${variant.key} ${speed.key} $conditions ${at.instant}"

object Schedule:

  def uniqueFor(tour: Tournament) =
    Schedule(
      freq = Freq.Unique,
      speed = Speed fromClock tour.clock,
      variant = tour.variant,
      position = tour.position,
      at = tour.startsAt.dateTime
    )

  case class Plan(schedule: Schedule, buildFunc: Option[Tournament => Tournament]):

    def build: Tournament =
      val t = Tournament.scheduleAs(addCondition(schedule), durationFor(schedule))
      buildFunc.foldRight(t) { _(_) }

    def map(f: Tournament => Tournament) =
      copy(
        buildFunc = buildFunc.fold(f)(f.compose).some
      )

  sealed abstract class Freq(val id: Int, val importance: Int) extends Ordered[Freq]:

    val name = Freq.this.toString.toLowerCase

    def compare(other: Freq) = Integer.compare(importance, other.importance)

    def isDaily          = this == Schedule.Freq.Daily
    def isDailyOrBetter  = this >= Schedule.Freq.Daily
    def isWeeklyOrBetter = this >= Schedule.Freq.Weekly
  object Freq:
    case object Hourly   extends Freq(10, 10)
    case object Daily    extends Freq(20, 20)
    case object Eastern  extends Freq(30, 15)
    case object Weekly   extends Freq(40, 40)
    case object Weekend  extends Freq(41, 41)
    case object Monthly  extends Freq(50, 50)
    case object Shield   extends Freq(51, 51)
    case object Marathon extends Freq(60, 60)
    case object ExperimentalMarathon extends Freq(61, 55): // for DB BC
      override val name = "Experimental Marathon"
    case object Yearly extends Freq(70, 70)
    case object Unique extends Freq(90, 59)
    val all: List[Freq] = List(
      Hourly,
      Daily,
      Eastern,
      Weekly,
      Weekend,
      Monthly,
      Shield,
      Marathon,
      ExperimentalMarathon,
      Yearly,
      Unique
    )
    def apply(name: String) = all.find(_.name == name)
    def byId(id: Int)       = all.find(_.id == id)

  enum Speed(val id: Int):
    val name = Speed.this.toString
    val key  = lila.common.String lcfirst name
    def trans(using Lang): String = this match
      case Speed.Rapid     => I18nKeys.rapid.txt()
      case Speed.Classical => I18nKeys.classical.txt()
      case _               => name
    case UltraBullet extends Speed(5)
    case HyperBullet extends Speed(10)
    case Bullet      extends Speed(20)
    case HippoBullet extends Speed(25)
    case SuperBlitz  extends Speed(30)
    case Blitz       extends Speed(40)
    case Rapid       extends Speed(50)
    case Classical   extends Speed(60)
  object Speed:
    val all                      = values.toList
    val mostPopular: List[Speed] = List(Bullet, Blitz, Rapid, Classical)
    def apply(key: String) = all.find(_.key == key) orElse all.find(_.key.toLowerCase == key.toLowerCase)
    def byId(id: Int)      = all find (_.id == id)
    def similar(s1: Speed, s2: Speed) =
      (s1, s2) match
        case (a, b) if a == b                                        => true
        case (Bullet, HippoBullet) | (HippoBullet, Bullet)           => true
        case (HyperBullet, UltraBullet) | (UltraBullet, HyperBullet) => true
        case _                                                       => false
    def fromClock(clock: chess.Clock.Config) =
      val time = clock.estimateTotalSeconds
      if (time < 30) UltraBullet
      else if (time < 60) HyperBullet
      else if (time < 120) Bullet
      else if (time < 180) HippoBullet
      else if (time < 480) Blitz
      else if (time < 1500) Rapid
      else Classical
    def toPerfType(speed: Speed) =
      speed match
        case UltraBullet                        => PerfType.UltraBullet
        case HyperBullet | Bullet | HippoBullet => PerfType.Bullet
        case SuperBlitz | Blitz                 => PerfType.Blitz
        case Rapid                              => PerfType.Rapid
        case Classical                          => PerfType.Classical

  enum Season:
    case Spring, Summer, Autumn, Winter

  private[tournament] def durationFor(s: Schedule): Int =
    import Freq.*, Speed.*
    import chess.variant.*

    (s.freq, s.variant, s.speed) match

      case (Hourly, _, UltraBullet | HyperBullet | Bullet) => 27
      case (Hourly, _, HippoBullet | SuperBlitz | Blitz)   => 57
      case (Hourly, _, Rapid) if s.hasMaxRating            => 57
      case (Hourly, _, Rapid | Classical)                  => 117

      case (Daily | Eastern, Standard, SuperBlitz) => 90
      case (Daily | Eastern, Standard, Blitz)      => 120
      case (Daily | Eastern, _, Blitz)             => 90
      case (Daily | Eastern, _, Rapid | Classical) => 150
      case (Daily | Eastern, _, _)                 => 60

      case (Weekly, _, UltraBullet | HyperBullet | Bullet) => 60 * 2
      case (Weekly, _, HippoBullet | SuperBlitz | Blitz)   => 60 * 3
      case (Weekly, _, Rapid)                              => 60 * 4
      case (Weekly, _, Classical)                          => 60 * 5

      case (Weekend, Crazyhouse, _)                         => 60 * 2
      case (Weekend, _, UltraBullet | HyperBullet | Bullet) => 90
      case (Weekend, _, HippoBullet | SuperBlitz)           => 60 * 2
      case (Weekend, _, Blitz)                              => 60 * 3
      case (Weekend, _, Rapid)                              => 60 * 4
      case (Weekend, _, Classical)                          => 60 * 5

      case (Monthly, _, UltraBullet)              => 60 * 2
      case (Monthly, _, HyperBullet | Bullet)     => 60 * 3
      case (Monthly, _, HippoBullet | SuperBlitz) => 60 * 3 + 30
      case (Monthly, _, Blitz)                    => 60 * 4
      case (Monthly, _, Rapid)                    => 60 * 5
      case (Monthly, _, Classical)                => 60 * 6

      case (Shield, _, UltraBullet)              => 60 * 3
      case (Shield, _, HyperBullet | Bullet)     => 60 * 4
      case (Shield, _, HippoBullet | SuperBlitz) => 60 * 5
      case (Shield, _, Blitz)                    => 60 * 6
      case (Shield, _, Rapid)                    => 60 * 8
      case (Shield, _, Classical)                => 60 * 10

      case (Yearly, _, UltraBullet | HyperBullet | Bullet) => 60 * 4
      case (Yearly, _, HippoBullet | SuperBlitz)           => 60 * 5
      case (Yearly, _, Blitz)                              => 60 * 6
      case (Yearly, _, Rapid)                              => 60 * 8
      case (Yearly, _, Classical)                          => 60 * 10

      case (Marathon, _, _)             => 60 * 24 // lol
      case (ExperimentalMarathon, _, _) => 60 * 4

      case (Unique, _, _) => 60 * 6

  private val standardIncHours         = Set(1, 7, 13, 19)
  private def standardInc(s: Schedule) = standardIncHours(s.at.getHour)
  private def zhInc(s: Schedule)       = s.at.getHour % 2 == 0

  private given Conversion[Int, LimitSeconds]     = LimitSeconds(_)
  private given Conversion[Int, IncrementSeconds] = IncrementSeconds(_)

  private def zhEliteTc(s: Schedule) =
    val TC = chess.Clock.Config
    s.at.getDayOfMonth / 7 match
      case 0 => TC(3 * 60, 0)
      case 1 => TC(1 * 60, 1)
      case 2 => TC(3 * 60, 2)
      case 3 => TC(1 * 60, 0)
      case _ => TC(2 * 60, 0) // for the sporadic 5th Saturday

  private[tournament] def clockFor(s: Schedule) =
    import Freq.*, Speed.*
    import chess.variant.*

    val TC = chess.Clock.Config

    (s.freq, s.variant, s.speed) match
      // Special cases.
      case (Weekend, Crazyhouse, Blitz)                 => zhEliteTc(s)
      case (Hourly, Crazyhouse, SuperBlitz) if zhInc(s) => TC(3 * 60, 1)
      case (Hourly, Crazyhouse, Blitz) if zhInc(s)      => TC(4 * 60, 2)
      case (Hourly, Standard, Blitz) if standardInc(s)  => TC(3 * 60, 2)

      case (Shield, variant, Blitz) if variant.exotic => TC(3 * 60, 2)

      case (_, _, UltraBullet) => TC(15, 0)
      case (_, _, HyperBullet) => TC(30, 0)
      case (_, _, Bullet)      => TC(60, 0)
      case (_, _, HippoBullet) => TC(2 * 60, 0)
      case (_, _, SuperBlitz)  => TC(3 * 60, 0)
      case (_, _, Blitz)       => TC(5 * 60, 0)
      case (_, _, Rapid)       => TC(10 * 60, 0)
      case (_, _, Classical)   => TC(20 * 60, 10)
  private[tournament] def addCondition(s: Schedule) =
    s.copy(conditions = conditionFor(s))

  private[tournament] def conditionFor(s: Schedule) =
    if s.conditions.nonEmpty then s.conditions
    else
      import Freq.*, Speed.*

      val nbRatedGame = (s.freq, s.variant, s.speed) match

        case (Hourly, variant, _) if variant.exotic => 0

        case (Hourly | Daily | Eastern, _, HyperBullet | Bullet)             => 20
        case (Hourly | Daily | Eastern, _, HippoBullet | SuperBlitz | Blitz) => 15
        case (Hourly | Daily | Eastern, _, Rapid)                            => 10

        case (Weekly | Weekend | Monthly | Shield, _, HyperBullet | Bullet)             => 30
        case (Weekly | Weekend | Monthly | Shield, _, HippoBullet | SuperBlitz | Blitz) => 20
        case (Weekly | Weekend | Monthly | Shield, _, Rapid)                            => 15
        case (Weekly | Weekend | Monthly | Shield, _, Classical)                        => 5

        case _ => 0

      val minRating = IntRating:
        (s.freq, s.variant) match
          case (Weekend, chess.variant.Crazyhouse) => 2100
          case (Weekend, _)                        => 2200
          case _                                   => 0

      TournamentCondition.All(
        nbRatedGame = nbRatedGame.some.filter(0 <) map Condition.NbRatedGame.apply,
        minRating = minRating.some.filter(_ > 0) map Condition.MinRating.apply,
        maxRating = none,
        titled = none,
        teamMember = none,
        allowList = none
      )
