package routes.interactions.utils

import db.members.MemberDao
import enums.Developer
import enums.Track
import routes.interactions.bodies.ViewSubmissionRequestBody
import secrets.JiraSecrets
import utils.SlackJsonCreator.createMarkdownText
import utils.escapeNewLine
import utils.toJson

class ReportCreatedMessageJsonCreator(
    private val viewSubmissionRequestBody: ViewSubmissionRequestBody,
    private val slackChannelId: String,
    private val issueKey: String
) {

    fun create() = """
        {
            "channel": "$slackChannelId",
            "blocks": [
                ${createQaMentionSection()},
                ${createFieldsSection()}
            ]
        }
    """.toJson()

    private fun createQaMentionSection() = """
        {
			"type": "section",
			"text": ${createMarkdownText("@qa")}
		}
    """

    private fun createFieldsSection(): String {
        val submissionValues = viewSubmissionRequestBody.view.state.values
        return """
            {
                "type": "section",
                "text": ${createMarkdownText(
            StringBuffer().apply {
                append(createField("보고자", "<@${viewSubmissionRequestBody.user.id}>"))
                append(createField("발생 경로", submissionValues.path.action.value!!))
                append(createField("오류 현상", submissionValues.situation.action.value!!))
                append(createField("기대 결과", submissionValues.expectedResult.action.value ?: "-"))
                append(createField("발생 버전", submissionValues.version.action.selectedOption!!.value))
                append(createField("서버", submissionValues.server.action.selectedOption?.value ?: "-"))
                append(createField("기타 환경", submissionValues.etcEnvironment.action.value ?: "-"))
                append(createField("심각도", submissionValues.priority.action.selectedOption!!.value))
                append(createField("예상 오류 유형", submissionValues.errorType.action.selectedOption?.value ?: "-"))
                append(createField("재현 가능 여부", submissionValues.reproducing.action.selectedOption?.value ?: "-"))
                append(createField("예상 담당트랙", createTrackMention(submissionValues.track.action.selectedOption!!.value)))
                append(createField("예상 담당개발자", createDeveloperMention(submissionValues.developer.action.selectedOption!!.value)))
                append(createField("리포팅 채널", submissionValues.channel.action.selectedOption!!.value))
                append(createField("지라 링크", getIssueUrl(issueKey)))
            }.toString()
                .escapeNewLine()
        )}
            }
        """
    }

    private fun createField(label: String, text: String) = "\n*$label*\n$text"

    private fun createTrackMention(displayName: String): String {
        return Track.get(displayName).slackUserGroupHandles
            .map { handle -> "@$handle" }
            .toList().toString()
    }

    private fun createDeveloperMention(displayName: String): String {
        return "<@${getMemberId(Developer.get(displayName).nickname)}>"
    }

    private fun getMemberId(nickname: String): String {
        return MemberDao().getMemberByNickname(nickname).id!!
    }

    private fun getIssueUrl(issueKey: String): String {
        return "${JiraSecrets.DOMAIN}/browse/$issueKey"
    }
}