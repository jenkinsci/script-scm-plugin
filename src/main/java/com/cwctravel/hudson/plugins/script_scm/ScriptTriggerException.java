package com.cwctravel.hudson.plugins.script_scm;

public class ScriptTriggerException extends RuntimeException {

	public ScriptTriggerException(String message) {
		super(message);
	}

	public ScriptTriggerException(String messsage, Throwable throwable) {
		super(messsage, throwable);
	}

	public ScriptTriggerException(Throwable throwable) {
		super(throwable);
	}

}
