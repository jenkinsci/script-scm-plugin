package com.cwctravel.hudson.plugins.script_scm;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.scm.RepositoryBrowser;

import java.io.IOException;
import java.net.URL;

public class ScriptSCMBrowser extends RepositoryBrowser<ScriptSCMChangeLogEntry> {
	private static final long serialVersionUID = 3690443721906714778L;

	@Extension
	public static final class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {

		@Override
		public String getDisplayName() {
			return "detail";
		}

	}

	@Override
	public URL getChangeSetLink(ScriptSCMChangeLogEntry changeSet) throws IOException {
		return new URL(changeSet.getChangesetUrl());
	}

}