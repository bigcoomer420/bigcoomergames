package views
package html.tournament

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.markdownLinksOrRichText
import lila.tournament.{ TeamBattle, Tournament }

object side:

  private val separator = " • "

  def apply(
      tour: Tournament,
      verdicts: lila.gathering.Condition.WithVerdicts,
      streamers: List[UserId],
      shieldOwner: Option[UserId],
      chat: Boolean
  )(using ctx: Context) =
    frag(
      div(cls := "tour__meta")(
        st.section(dataIcon := tour.perfType.iconChar.toString)(
          div(
            p(
              tour.clock.show,
              separator,
              views.html.game.bits.variantLink(
                tour.variant,
                tour.perfType.some,
                shortName = true
              ),
              tour.position.isDefined ?? s"$separator${trans.thematic.txt()}",
              separator,
              tour.durationString
            ),
            if tour.mode.rated then trans.ratedTournament() else trans.casualTournament(),
            separator,
            "Arena",
            (isGranted(_.ManageTournament) || (ctx.userId
              .has(tour.createdBy) && !tour.isFinished)) option frag(
              " ",
              a(href := routes.Tournament.edit(tour.id), title := "Edit tournament")(iconTag(""))
            )
          )
        ),
        tour.teamBattle map teamBattle(tour),
        tour.spotlight map { s =>
          st.section(cls := "description")(
            markdownLinksOrRichText(s.description),
            shieldOwner map { owner =>
              p(cls := "defender", dataIcon := "")(
                "Defender:",
                userIdLink(owner.some)
              )
            }
          )
        },
        variantTeamLinks.get(tour.variant.key) filter { (team, _) =>
          tour.createdBy == lila.user.User.lichessId || tour.conditions.teamMember.exists(_.teamId == team.id)
        } map { case (team, link) =>
          st.section(
            if (isMyTeamSync(team.id)) frag(trans.team.team(), " ", link)
            else trans.team.joinLichessVariantTeam(link)
          )
        },
        tour.description map { d =>
          st.section(cls := "description")(markdownLinksOrRichText(d))
        },
        tour.looksLikePrize option bits.userPrizeDisclaimer(tour.createdBy),
        views.html.gathering.verdicts(verdicts, tour.perfType),
        tour.noBerserk option div(cls := "text", dataIcon := "")(trans.arena.noBerserkAllowed()),
        tour.noStreak option div(cls := "text", dataIcon := "")(trans.arena.noArenaStreaks()),
        !tour.isScheduled option frag(small(trans.by(userIdLink(tour.createdBy.some))), br),
        (!tour.isStarted || (tour.isScheduled && tour.position.isDefined)) option absClientInstant(
          tour.startsAt
        ),
        tour.startingPosition.map { pos =>
          p(a(href := pos.url)(pos.name))
        } orElse tour.position.map { fen =>
          p(
            "Custom position",
            separator,
            views.html.base.bits.fenAnalysisLink(fen into chess.format.Fen.Epd)
          )
        }
      ),
      views.html.streamer.bits.contextual(streamers),
      chat option views.html.chat.frag
    )

  private def teamBattle(tour: Tournament)(battle: TeamBattle)(implicit ctx: Context) =
    st.section(cls := "team-battle")(
      p(cls := "team-battle__title text", dataIcon := "")(
        s"Battle of ${battle.teams.size} teams and ${battle.nbLeaders} leaders",
        (ctx.userId.has(tour.createdBy) || isGranted(_.ManageTournament)) option
          a(href := routes.Tournament.teamBattleEdit(tour.id), title := "Edit team battle")(iconTag(""))
      )
    )
