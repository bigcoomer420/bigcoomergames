package views
package html.site

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.user.User

object message:

  def apply(
      title: String,
      back: Option[String] = None,
      icon: Option[String] = None,
      moreCss: Option[Frag] = None
  )(message: Modifier*)(using Context) =
    views.html.base.layout(title = title, moreCss = ~moreCss):
      main(cls := "box box-pad")(
        boxTop(
          h1(dataIcon := icon ifTrue back.isEmpty, cls := List("text" -> (icon.isDefined && back.isEmpty)))(
            back map { url =>
              a(href := url, dataIcon := "", cls := "text")
            },
            title
          )
        ),
        p(message)
      )

  def noBot(using Context) = apply("No bot area"):
    frag("Sorry, bot accounts are not allowed here.")

  def noEngine(using Context) = apply("No engine area"):
    "Sorry, engine assisted players are not allowed here."

  def noBooster(using Context) = apply("No booster area"):
    "Sorry, boosters and sandbaggers are not allowed here."

  def blacklistedMessage(using ctx: Context) =
    s"Sorry, your IP address ${ctx.ip} has been used to violate the ToS, and is now blacklisted."

  def privateStudy(study: lila.study.Study)(using Context) =
    apply(
      title = s"${titleNameOrId(study.ownerId)}'s study",
      back = routes.Study.allDefault(1).url.some
    )(
      "Sorry! This study is private, you cannot access it.",
      isGranted(_.StudyAdmin) option postForm(action := routes.Study.admin(study.id))(
        submitButton("View as admin")(cls := "button button-red")
      )
    )

  def streamingMod(using Context) = apply("Disabled while streaming"):
    frag(
      "This moderation feature is disabled while streaming, ",
      "to avoid leaking sensible information."
    )

  def challengeDenied(msg: String)(using Context) =
    apply(
      title = trans.challenge.challengeToPlay.txt(),
      back = routes.Lobby.home.url.some
    )(msg)

  def insightNoGames(u: User)(using Context) =
    apply(
      title = s"${u.username} has not played a rated game yet!",
      back = routes.User.show(u.id).url.some
    ):
      frag(
        "Before using chess insights,",
        userLink(u),
        " has to play at least one rated game."
      )

  def teamCreateLimit(using Context) = apply("Cannot create a team"):
    "You have already created a team this week."

  def teamJoinLimit(using Context) = apply("Cannot join the team"):
    "You have already joined too many teams."

  def authFailed(using Context) = apply("403 - Access denied!"):
    "You tried to visit a page you're not authorized to access."

  def temporarilyDisabled(using Context) = apply("Temporarily disabled"):
    "Sorry, this feature is temporarily disabled while we figure out a way to bring it back."

  def notYet(text: String)(using Context) =
    apply("Not yet available")(text)
