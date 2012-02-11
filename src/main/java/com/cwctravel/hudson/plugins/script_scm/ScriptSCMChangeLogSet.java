package com.cwctravel.hudson.plugins.script_scm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.util.Digester2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.kohsuke.stapler.framework.io.IOException2;
import org.xml.sax.SAXException;

public class ScriptSCMChangeLogSet extends ChangeLogSet<ScriptSCMChangeLogEntry> {
	private final List<ScriptSCMChangeLogEntry> changeLogEntries;

	protected ScriptSCMChangeLogSet(AbstractBuild<?, ?> build, List<ScriptSCMChangeLogEntry> changeLogEntries) {
		super(build);
		this.changeLogEntries = changeLogEntries;
	}

	public Iterator<ScriptSCMChangeLogEntry> iterator() {
		return changeLogEntries.iterator();
	}

	@Override
	public boolean isEmptySet() {
		return changeLogEntries.isEmpty();
	}

	public static ScriptSCMChangeLogSet parse(AbstractBuild<?, ?> build, File changelogFile) throws IOException, SAXException {
		List<ScriptSCMChangeLogEntry> changeLogEntries = new ArrayList<ScriptSCMChangeLogEntry>();

		Digester digester = new Digester2();
		digester.push(changeLogEntries);

		digester.addObjectCreate("*/entry", ScriptSCMChangeLogEntry.class);
		digester.addBeanPropertySetter("*/entry/date");
		digester.addBeanPropertySetter("*/entry/user-id", "userId");
		digester.addBeanPropertySetter("*/entry/changeset-number", "changesetNumber");
		digester.addBeanPropertySetter("*/entry/comment", "comment");
		digester.addSetNext("*/entry", "add");

		digester.addObjectCreate("*/entry/items/item", ScriptSCMChangeLogEntry.Item.class);
		digester.addBeanPropertySetter("*/entry/items/item/change-type", "changeType");
		digester.addBeanPropertySetter("*/entry/items/item/file-name", "filename");
		digester.addSetNext("*/entry/items/item", "addFile");

		try {
			digester.parse(changelogFile);
		}
		catch(IOException e) {
			throw new IOException2("Failed to parse " + changelogFile, e);
		}
		catch(SAXException e) {
			throw new IOException2("Failed to parse " + changelogFile, e);
		}

		return new ScriptSCMChangeLogSet(build, changeLogEntries);
	}

}
