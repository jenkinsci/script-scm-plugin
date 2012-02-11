package com.cwctravel.hudson.plugins.script_scm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

public class ScriptSCMChangeLogParser extends ChangeLogParser {

	@Override
	public ChangeLogSet<? extends Entry> parse(AbstractBuild build, File changelogFile) throws IOException, SAXException {
		return ScriptSCMChangeLogSet.parse(build, changelogFile);
	}

}
