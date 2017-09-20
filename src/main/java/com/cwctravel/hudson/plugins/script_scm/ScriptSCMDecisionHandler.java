package com.cwctravel.hudson.plugins.script_scm;

import java.util.List;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Queue.BlockedItem;
import hudson.scm.SCM;
import hudson.triggers.SCMTrigger.SCMTriggerCause;
import jenkins.model.Jenkins;
import jenkins.scm.SCMDecisionHandler;

@Extension
public class ScriptSCMDecisionHandler extends SCMDecisionHandler {

	@Override
	public boolean shouldPoll(Item item) {
		boolean result = true;
		if(item instanceof AbstractProject<?, ?>) {
			AbstractProject<?, ?> p = (AbstractProject<?, ?>)item;
			SCM scm = p.getScm();
			if(scm instanceof ScriptSCM) {
				List<Queue.Item> queueItems = Jenkins.getInstance().getQueue().getItems(p);
				if(queueItems != null) {
					for(Queue.Item queueItem: queueItems) {
						if(queueItem instanceof BlockedItem) {
							List<Cause> causes = queueItem.getCauses();
							for(Cause cause: causes) {
								if(cause instanceof SCMTriggerCause) {
									result = false;
									break;
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

}
