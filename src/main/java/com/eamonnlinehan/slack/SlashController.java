package com.eamonnlinehan.slack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This Http endpoint handles Slack slash commands - by triggering specific
 * Jenkins jobs
 * 
 * https://api.slack.com/slash-commands
 * 
 * @author <a href="mailto:eamonnlinehan@gmail.com">Eamonn Linehan</a>
 */
@RestController
public class SlashController {

	private static Log log = LogFactory.getLog(SlashController.class);

	@Value("${slack.token}")
	private String slackToken;

	@Autowired
	private JenkinsWebhook jenkins;

	@Autowired
	private SlashCommandParser commandParser;

	/**
	 * 
	 * https://api.slack.com/slash-commands
	 * 
	 * token=gIkuvaNzQIHg97ATvDxqgjtO team_id=T0001 team_domain=example
	 * channel_id=C2147483705 channel_name=test user_id=U2147483697
	 * user_name=Steve command=/weather text=94070
	 * 
	 * @param valueOne
	 * @return
	 */
	@RequestMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> handleSlackSlashCommand(@RequestParam("command") String command,
			@RequestParam("text") String text, @RequestParam("user_name") String userName,
			@RequestParam("user_id") String user_id, @RequestParam("channel_name") String channelName,
			@RequestParam("team_domain") String teamDomain, @RequestParam("token") String token) {

		log.info(userName + "@" + teamDomain + " request to execute command '" + command + " " + text
				+ "' on channel #" + channelName);

		// check the token is correct
		if (!slackToken.equals(token))
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);

		String buildCause = "Slash command " + command + " on channel #" + channelName + " executed by "
				+ userName + "@" + teamDomain;

		JenkinsJob job = commandParser.parse(text);
		job.setCause(buildCause);

		HttpStatus status = jenkins.triggerJob(job);
		
		if (status.is2xxSuccessful())
			return new ResponseEntity<String>("Jenkins building '" + job.getJobName() + "'", status);
		else
			return new ResponseEntity<String>("Failed to trigger Jenkins job '" + job.getJobName() + "'", status);
	}

}
