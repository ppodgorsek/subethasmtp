package org.subethamail.smtp.command;

import java.io.IOException;

import org.subethamail.smtp.server.BaseCommand;
import org.subethamail.smtp.server.Session;

/**
 * @author <a href="mailto:zuccala.m@gmail.com">Michele Zuccala</a>
 */
public class ExpandCommand extends BaseCommand 
{
	public ExpandCommand() 
	{
		super("EXPN", "The expn command.");
	}

	@Override
	public void execute(String commandString, Session sess) throws IOException 
	{
		sess.sendResponse("502 EXPN command is disabled");
	}
}
