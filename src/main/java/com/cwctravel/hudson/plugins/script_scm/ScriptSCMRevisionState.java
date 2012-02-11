package com.cwctravel.hudson.plugins.script_scm;

import hudson.scm.SCMRevisionState;

public class ScriptSCMRevisionState extends SCMRevisionState {
	private String revisionState;

	public String getRevisionState() {
		return revisionState;
	}

	public void setRevisionState(String revisionState) {
		this.revisionState = revisionState;
	}

}
